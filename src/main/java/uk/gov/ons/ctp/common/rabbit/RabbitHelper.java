package uk.gov.ons.ctp.common.rabbit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.godaddy.logging.Logger;
import com.godaddy.logging.LoggerFactory;
import com.rabbitmq.client.AMQP.Queue.PurgeOk;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.GetResponse;
import java.io.IOException;
import java.util.concurrent.TimeoutException;
import uk.gov.ons.ctp.common.config.YmlConfigReader;
import uk.gov.ons.ctp.common.error.CTPException;
import uk.gov.ons.ctp.common.error.CTPException.Fault;
import uk.gov.ons.ctp.common.event.EventPublisher;
import uk.gov.ons.ctp.common.event.EventPublisher.EventType;
import uk.gov.ons.ctp.common.event.EventPublisher.RoutingKey;
import uk.gov.ons.ctp.common.event.EventPublisher.Source;
import uk.gov.ons.ctp.common.event.NativeRabbitEventSender;
import uk.gov.ons.ctp.common.event.RabbitConnectionDetails;
import uk.gov.ons.ctp.common.event.model.EventPayload;

/**
 * This is a test support class for interacting with RabbitMQ.
 *
 * <p>It runs as a singleton and the connection is established on first usage. When connecting to
 * Rabbit it uses the connection details from a property file, with any or all of these fields
 * overridable with equivalent environment variables.
 *
 * <p>The RabbitMQ Java API does not support concurrent usage of the Channel object, so this class
 * enforces this restriction with method level synchronisation.
 */
public class RabbitHelper {
  private static final Logger log = LoggerFactory.getLogger(RabbitHelper.class);

  private static final String RABBIT_YML_FILENAME = "rabbitmq.yml";

  private static RabbitHelper instance = null;

  private Connection rabbit;
  private Channel channel;
  private String exchange;

  private EventPublisher eventPublisher;

  private ObjectMapper mapper = new ObjectMapper();

  private RabbitHelper(
      RabbitConnectionDetails rabbitDetails, String exchange, boolean addRmProperties)
      throws CTPException {

    ConnectionFactory factory = new ConnectionFactory();
    factory.setUsername(rabbitDetails.getUsername());
    factory.setPassword(rabbitDetails.getPassword());
    factory.setHost(rabbitDetails.getHost());
    factory.setPort(rabbitDetails.getPort());

    // Connect to rabbit
    try {
      this.rabbit = factory.newConnection();
    } catch (IOException | TimeoutException e) {
      String errorMessage = "Failed to connect to RabbitMQ";
      log.with("exchange", exchange).error(errorMessage, e);
      throw new CTPException(Fault.SYSTEM_ERROR, e, errorMessage);
    }

    NativeRabbitEventSender sender =
        new NativeRabbitEventSender(this.rabbit, exchange, addRmProperties);
    eventPublisher = EventPublisher.createWithoutEventPersistence(sender);

    this.exchange = exchange;
  }

  public static synchronized RabbitHelper instance(String exchange, boolean addRmProperties)
      throws CTPException {
    if (instance == null) {
      YmlConfigReader ymlConfig = new YmlConfigReader(RABBIT_YML_FILENAME);
      RabbitConnectionDetails rabbitDetails =
          ymlConfig.convertToObject(RabbitConnectionDetails.class);

      instance = new RabbitHelper(rabbitDetails, exchange, addRmProperties);
    }

    if (!instance.exchange.equals(exchange)) {
      throw new CTPException(
          Fault.BAD_REQUEST, "Cannot switch existing connection to different exchange");
    }

    // Make sure channel has been created
    instance.createChannelIfNeeded();

    return instance;
  }

  // Make sure channel has been created. It may need to be re-established if a previous command
  // failed.
  private synchronized void createChannelIfNeeded() throws CTPException {
    if (channel == null) {
      try {
        channel = rabbit.createChannel();
        channel.exchangeDeclare(exchange, "topic", true);
      } catch (IOException e) {
        channel = null;
        String errorMessage = "Failed to create RabbitMQ channel";
        log.error(errorMessage, e);
        throw new CTPException(Fault.SYSTEM_ERROR, e, errorMessage);
      }
    }
  }

  /**
   * This method releases the Rabbit connection.
   *
   * @throws CTPException if an error was detected.
   */
  public synchronized void close() throws CTPException {
    if (channel != null) {
      try {
        channel.close();
        channel = null;
      } catch (IOException | TimeoutException e1) {
        String errorMessage1 = "Failed to close RabbitMQ channel";
        log.error(errorMessage1, e1);
        throw new CTPException(Fault.SYSTEM_ERROR, e1, errorMessage1);
      }
    }

    if (rabbit != null) {
      try {
        rabbit.close(1000);
      } catch (IOException e) {
        String errorMessage = "Failed to close RabbitMQ connection";
        log.error(errorMessage, e);
        throw new CTPException(Fault.SYSTEM_ERROR, e, errorMessage);
      }
    }

    RabbitHelper.instance = null;
  }

  /**
   * Creates and binds a queue. Rabbit doesn't mind if this method is rerun on for an existing
   * queue/binding.
   *
   * @param eventType is the type of events which we need a queue to receive.
   * @return a String containing the name of the queue. For the purposes of testing this is actually
   *     the routing key.
   * @throws CTPException if the queue or binding could not be created.
   */
  public synchronized String createQueue(EventType eventType) throws CTPException {
    String queueName;

    try {
      // Find routing key for supplied event type
      RoutingKey routingKey = RoutingKey.forType(eventType);
      if (routingKey == null) {
        String errorMessage = "Routing key for eventType '" + eventType + "' not configured";
        log.with("eventType", eventType).error(errorMessage);
        throw new UnsupportedOperationException(errorMessage);
      }

      // Use routing key for queue name as well as binding. This gives the queue a 'fake' name, but
      // it saves the Cucumber tests from having to decide on a queue name
      String routingKeyName = routingKey.getKey();
      queueName = routingKeyName;

      // Create queue and binding
      channel.queueDeclare(queueName, true, false, false, null);
      channel.queueBind(queueName, exchange, routingKey.getKey());
    } catch (IOException e) {
      channel = null; // Channel object now in broken state. Force recreation
      String errorMessage = "Failed to create/bind queue";
      log.error(errorMessage, e);
      throw new CTPException(Fault.SYSTEM_ERROR, e, errorMessage);
    }

    return queueName;
  }

  /**
   * Deletes any outstanding messages on a queue.
   *
   * @param queueName is the name of the queue to be cleared.
   * @return the number of messages deleted.
   * @throws CTPException if Rabbit failed during the queue purge.
   */
  public synchronized int flushQueue(String queueName) throws CTPException {
    try {
      PurgeOk result = channel.queuePurge(queueName);
      return result.getMessageCount();
    } catch (IOException e) {
      channel = null;
      String errorMessage = "Failed to flush queue '" + queueName + "'";
      log.with("queueName", queueName).error(errorMessage, e);
      throw new CTPException(Fault.SYSTEM_ERROR, e, errorMessage);
    }
  }

  /**
   * Publish a message to a Rabbit exchange.
   *
   * @param eventType is the type of the event that is being sent.
   * @param source states who is sending, or pretending, to set the message.
   * @param channel holds a channel identifier.
   * @param payload is the object to be sent.
   * @return the transaction id generated for the published message.
   * @throws CTPException if anything went wrong.
   */
  public synchronized String sendEvent(
      EventType eventType, Source source, EventPublisher.Channel channel, EventPayload payload)
      throws CTPException {
    try {
      String transactionId = eventPublisher.sendEvent(eventType, source, channel, payload);
      return transactionId;

    } catch (Exception e) {
      String errorMessage = "Failed to send message. Cause: " + e.getMessage();
      log.with("eventType", eventType)
          .with("source", source)
          .with("channel", channel)
          .error(errorMessage, e);
      throw new CTPException(Fault.SYSTEM_ERROR, errorMessage, e);
    }
  }

  /**
   * Reads a message from the named queue. This method will wait for up to the specified number of
   * milliseconds for a message to appear on the queue.
   *
   * @param queueName is the name of the queue to read from.
   * @param maxWaitTimeMillis is the maximum amount of time the caller is prepared to wait for the
   *     message to appear.
   * @return a String containing the content of the message body, or null if no message was found
   *     before the timeout expired.
   * @throws CTPException if Rabbit threw an exception when we attempted to read a message.
   */
  public String getMessage(String queueName, long maxWaitTimeMillis) throws CTPException {
    final long startTime = System.currentTimeMillis();
    final long timeoutLimit = startTime + maxWaitTimeMillis;

    log.info(
        "Rabbit getMessage. Reading from queue '"
            + queueName
            + "'"
            + " within "
            + maxWaitTimeMillis
            + "ms");

    // Keep trying to read a message from rabbit, or we timeout waiting
    String messageBody;
    do {
      messageBody = getMessageNoWait(queueName);
      if (messageBody != null) {
        log.info("Message read from queue");
        break;
      }

      try {
        Thread.sleep(10);
      } catch (InterruptedException e) {
        break;
      }
    } while (messageBody == null && System.currentTimeMillis() < timeoutLimit);

    return messageBody;
  }

  /**
   * Reads a message from the named queue and convert it to a Java object. This method will wait for
   * up to the specified number of milliseconds for a message to appear on the queue.
   *
   * @param <T> is the class of object we are expected to recieve.
   * @param queueName is the name of the queue to read from.
   * @param clazz is the class that the message should be converted to.
   * @param maxWaitTimeMillis is the maximum amount of time the caller is prepared to wait for the
   *     message to appear.
   * @return an object of the specified type, or null if no message was found before the timeout
   *     expired.
   * @throws CTPException if Rabbit threw an exception when we attempted to read a message.
   */
  public <T> T getMessage(String queueName, Class<T> clazz, long maxWaitTimeMillis)
      throws CTPException {
    String message = getMessage(queueName, maxWaitTimeMillis);

    // Return to caller if nothing read from queue
    if (message == null) {
      log.info(
          "Rabbit getMessage. Message is null. Unable to convert to class '"
              + clazz.getName()
              + "'");
      return null;
    }

    // Use Jackson to convert from a Json message to a Java object
    try {
      log.info("Rabbit getMessage. Converting result into class '" + clazz.getName() + "'");
      return mapper.readValue(message, clazz);

    } catch (IOException e) {
      String errorMessage = "Failed to convert message to object of type '" + clazz.getName() + "'";
      log.with("queueName", queueName).error(errorMessage, e);
      throw new CTPException(Fault.SYSTEM_ERROR, e, errorMessage);
    }
  }

  /**
   * Read the next message from a queue.
   *
   * @param queueName holds the name of the queue to attempt the read from.
   * @return a String with the content of the message body, or null if there was no message to read.
   * @throws CTPException if Rabbit threw an exception during the message get.
   */
  private synchronized String getMessageNoWait(String queueName) throws CTPException {
    // Attempt to read a message from the queue
    GetResponse result;
    try {
      result = channel.basicGet(queueName, true);
    } catch (IOException e) {
      channel = null;
      String errorMessage = "Failed to flush queue '" + queueName + "'";
      log.with("queueName", queueName).error(errorMessage, e);
      throw new CTPException(Fault.SYSTEM_ERROR, e, errorMessage);
    }

    return result == null ? null : new String(result.getBody());
  }
}
