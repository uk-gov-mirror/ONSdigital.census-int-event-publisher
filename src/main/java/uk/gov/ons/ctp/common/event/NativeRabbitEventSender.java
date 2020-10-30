package uk.gov.ons.ctp.common.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.godaddy.logging.Logger;
import com.godaddy.logging.LoggerFactory;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import java.io.IOException;
import java.util.concurrent.TimeoutException;
import uk.gov.ons.ctp.common.error.CTPException;
import uk.gov.ons.ctp.common.error.CTPException.Fault;
import uk.gov.ons.ctp.common.event.EventPublisher.RoutingKey;
import uk.gov.ons.ctp.common.event.model.GenericEvent;
import uk.gov.ons.ctp.common.jackson.CustomObjectMapper;

public class NativeRabbitEventSender implements EventSender {
  private static final Logger log = LoggerFactory.getLogger(NativeRabbitEventSender.class);

  Connection connection;
  String exchange;
  Channel channel;

  ObjectMapper objectMapper;

  public NativeRabbitEventSender(Connection rabbitConnection, String exchange) throws CTPException {
    this.connection = rabbitConnection;

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

  public NativeRabbitEventSender(RabbitConnectionDetails connectionDetails, String exchange)
      throws CTPException {
    this(createRabbitConnection(connectionDetails), exchange);
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

  @Override
  public void sendEvent(RoutingKey routingKey, GenericEvent genericEvent) throws Exception {
    channel.basicPublish(
        exchange,
        routingKey.getKey(),
        null,
        objectMapper.writeValueAsString(genericEvent).getBytes("UTF-8"));
  }
}
