package uk.gov.ons.ctp.common.event.persistence;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import uk.gov.ons.ctp.common.event.EventPublisher.EventType;
import uk.gov.ons.ctp.common.event.EventPublisher.RoutingKey;
import uk.gov.ons.ctp.common.event.model.GenericEvent;

/**
 * This class holds data about an event which Rabbit failed to send.
 *
 * <p>This object is persisted into the backup event collection in Firestore for later resending.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class EventBackupData {
  private EventType eventType;
  private Long messageFailureDateTimeInMillis;
  private Long messageSentDateTimeInMillis;
  private RoutingKey routingKey;
  private GenericEvent genericEvent;
}
