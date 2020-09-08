package uk.gov.ons.ctp.common.event.persistence;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import uk.gov.ons.ctp.common.event.EventPublisher.EventType;

/**
 * This class holds data about an event which Rabbit failed to send.
 *
 * <p>If Rabbit fails to send an event then an instance of this object is persisted into the backup
 * event collection in Firestore for later resending.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class EventBackupData {
  private EventType eventType;
  private Long messageFailureDateTimeInMillis;
  private Long messageSentDateTimeInMillis;
  private String id;
  private String event;
}
