package uk.gov.ons.ctp.common.event;

import uk.gov.ons.ctp.common.event.EventPublisher.RoutingKey;
import uk.gov.ons.ctp.common.event.model.CommonEvent;

public interface EventSender {

  void sendEvent(RoutingKey routingKey, CommonEvent genericEvent) throws Exception;

  default void close() throws Exception {}
}
