package uk.gov.ons.ctp.common.event;

import java.io.IOException;
import java.util.concurrent.TimeoutException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.godaddy.logging.Logger;
import com.godaddy.logging.LoggerFactory;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import uk.gov.ons.ctp.common.error.CTPException;
import uk.gov.ons.ctp.common.error.CTPException.Fault;
import uk.gov.ons.ctp.common.event.EventPublisher.RoutingKey;
import uk.gov.ons.ctp.common.event.model.GenericEvent;

public class NativeRabbitEventSender implements EventSender {
  private static final Logger log = LoggerFactory.getLogger(NativeRabbitEventSender.class);

  Connection connection;
  String exchange;
  Channel channel;

  ObjectMapper objectMapper;

  public NativeRabbitEventSender(RabbitConnectionDetails connectionDetails, String exchange)
      throws CTPException {
    objectMapper = new ObjectMapper();

    ConnectionFactory factory = new ConnectionFactory();
    factory.setHost(connectionDetails.getHost());
    factory.setPort(connectionDetails.getPort());
    factory.setUsername(connectionDetails.getUser());
    factory.setPassword(connectionDetails.getPassword());
    connection = null;
    try {
      connection = factory.newConnection();
      this.exchange = exchange;
      channel = connection.createChannel();
      channel.exchangeDeclare(exchange, "topic", true);
    } catch (TimeoutException | IOException e) {
      String errorMessage = "Failed to connect to Rabbit";
      log.error(e, errorMessage);
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
