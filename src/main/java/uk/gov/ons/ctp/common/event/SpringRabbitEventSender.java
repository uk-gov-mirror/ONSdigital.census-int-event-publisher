package uk.gov.ons.ctp.common.event;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import uk.gov.ons.ctp.common.event.EventPublisher.RoutingKey;
import uk.gov.ons.ctp.common.event.model.GenericEvent;

public class SpringRabbitEventSender implements EventSender {

  private RabbitTemplate template;

  public SpringRabbitEventSender(RabbitTemplate template) {
    this.template = template;
  }

  @Override
  public void sendEvent(RoutingKey routingKey, GenericEvent genericEvent) {
    template.convertAndSend(routingKey.getKey(), genericEvent);
  }
}
