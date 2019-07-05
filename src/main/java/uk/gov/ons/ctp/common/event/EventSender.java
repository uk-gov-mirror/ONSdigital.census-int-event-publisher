package uk.gov.ons.ctp.common.event;

import uk.gov.ons.ctp.common.event.EventPublisher.RoutingKey;
import uk.gov.ons.ctp.common.event.model.GenericEvent;

public interface EventSender {

  void sendEvent(RoutingKey routingKey, GenericEvent genericEvent) throws Exception;

  default void close() throws Exception {}
}
