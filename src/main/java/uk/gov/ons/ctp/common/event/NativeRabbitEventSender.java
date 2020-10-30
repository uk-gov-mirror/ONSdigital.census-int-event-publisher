package uk.gov.ons.ctp.common.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.godaddy.logging.Logger;
import com.godaddy.logging.LoggerFactory;
import com.rabbitmq.client.AMQP.BasicProperties;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import java.io.IOException;
import java.util.HashMap;
import java.util.concurrent.TimeoutException;
import uk.gov.ons.ctp.common.error.CTPException;
import uk.gov.ons.ctp.common.error.CTPException.Fault;
import uk.gov.ons.ctp.common.event.EventPublisher.EventType;
import uk.gov.ons.ctp.common.event.EventPublisher.RoutingKey;
import uk.gov.ons.ctp.common.event.model.GenericEvent;
import uk.gov.ons.ctp.common.jackson.CustomObjectMapper;

public class NativeRabbitEventSender implements EventSender {
  private static final Logger log = LoggerFactory.getLogger(NativeRabbitEventSender.class);

  private Connection connection;
  private String exchange;
  private Channel channel;
  private boolean addRmProperties;

  private ObjectMapper objectMapper;

  public NativeRabbitEventSender(
      Connection rabbitConnection, String exchange, boolean addRmProperties) throws CTPException {
    this.connection = rabbitConnection;
    this.addRmProperties = addRmProperties;

    try {
      this.exchange = exchange;
      channel = connection.createChannel();
      channel.exchangeDeclare(exchange, "topic", true);
    } catch (IOException e) {
      String errorMessage = "Failed to create Rabbit channel";
      log.with("exchange", exchange).error(e, errorMessage);
      throw new CTPException(Fault.SYSTEM_ERROR, errorMessage);
    }

    objectMapper = new CustomObjectMapper();
  }

  public NativeRabbitEventSender(
      RabbitConnectionDetails connectionDetails, String exchange, boolean addRmProperties)
      throws CTPException {
    this(createRabbitConnection(connectionDetails), exchange, addRmProperties);
  }

  private static Connection createRabbitConnection(RabbitConnectionDetails connectionDetails)
      throws CTPException {
    ConnectionFactory factory = new ConnectionFactory();
    factory.setHost(connectionDetails.getHost());
    factory.setPort(connectionDetails.getPort());
    factory.setUsername(connectionDetails.getUsername());
    factory.setPassword(connectionDetails.getPassword());

    try {
      return factory.newConnection();
    } catch (IOException | TimeoutException e) {
      String errorMessage = "Failed to connect to Rabbit";
      log.with("connectionDetails", connectionDetails).error(e, errorMessage);
      throw new CTPException(Fault.SYSTEM_ERROR, errorMessage);
    }
  }

  @Override
  public void close() throws IOException {
    connection.close();
  }

  // create basic properties that may optionally partially simulate RM message properties.
  private BasicProperties createBasicProperties(GenericEvent genericEvent) {
    BasicProperties props = null;
    if (this.addRmProperties) {
      var headers = new HashMap<String, Object>();
      EventType type = genericEvent.getEvent().getType();
      switch (type) {
        case CASE_CREATED:
        case CASE_UPDATED:
        case UAC_CREATED:
        case UAC_UPDATED:
          headers.put("__TypeId__", "uk.gov.ons.census.casesvc.model.dto.ResponseManagementEvent");
          break;
        default:
          break;
      }

      props =
          new BasicProperties.Builder()
              .contentType("application/json")
              .contentEncoding("UTF-8")
              .headers(headers)
              .priority(0)
              .build();
    }
    return props;
  }

  @Override
  public void sendEvent(RoutingKey routingKey, GenericEvent genericEvent) {
    BasicProperties props = createBasicProperties(genericEvent);
    try {
      channel.basicPublish(
          exchange,
          routingKey.getKey(),
          props,
          objectMapper.writeValueAsString(genericEvent).getBytes("UTF-8"));
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}
