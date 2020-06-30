package uk.gov.ons.ctp.common.event.persistence;

import uk.gov.ons.ctp.common.error.CTPException;
import uk.gov.ons.ctp.common.event.EventPublisher.EventType;
import uk.gov.ons.ctp.common.event.EventPublisher.RoutingKey;
import uk.gov.ons.ctp.common.event.model.GenericEvent;

public interface EventPersistence {

  boolean isFirestorePersistenceSupported();

  void persistEvent(EventType eventType, RoutingKey routingKey, GenericEvent genericEvent)
      throws CTPException;
}
