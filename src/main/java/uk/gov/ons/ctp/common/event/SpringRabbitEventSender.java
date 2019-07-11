package uk.gov.ons.ctp.common.event;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import uk.gov.ons.ctp.common.event.EventPublisher.RoutingKey;
import uk.gov.ons.ctp.common.event.model.CommonEvent;

public class SpringRabbitEventSender implements EventSender {

  private RabbitTemplate template;

  public SpringRabbitEventSender(RabbitTemplate template) {
    this.template = template;
  }

  @Override
  public void sendEvent(RoutingKey routingKey, CommonEvent genericEvent) {
    template.convertAndSend(routingKey.getKey(), genericEvent);
  }
}
