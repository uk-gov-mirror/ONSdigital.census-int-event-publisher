package uk.gov.ons.ctp.common.event.persistence;

import com.godaddy.logging.Logger;
import com.godaddy.logging.LoggerFactory;
import javax.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uk.gov.ons.ctp.common.cloud.RetryableCloudDataStore;
import uk.gov.ons.ctp.common.error.CTPException;
import uk.gov.ons.ctp.common.event.EventPublisher.EventType;
import uk.gov.ons.ctp.common.event.EventPublisher.RoutingKey;
import uk.gov.ons.ctp.common.event.model.GenericEvent;

/**
 * This class saves details about an event which Rabbit failed to sent into a Firestore collection.
 */
@Service
public class FirestoreEventPersistence implements EventPersistence {

  private static final Logger log = LoggerFactory.getLogger(FirestoreEventPersistence.class);

  @Autowired private RetryableCloudDataStore cloudDataStore;

  @Value("${GOOGLE_CLOUD_PROJECT}")
  String gcpProject;

  @Value("${cloud-storage.event-backup-schema-name}")
  String eventBackupSchemaName;

  String eventBackupSchema;

  @PostConstruct
  public void init() {
    eventBackupSchema = gcpProject + "-" + eventBackupSchemaName.toLowerCase();
  }

  @Override
  public boolean isFirestorePersistenceSupported() {
    return true;
  }

  @Override
  public void persistEvent(EventType eventType, RoutingKey routingKey, GenericEvent genericEvent)
      throws CTPException {
    log.with("id", routingKey.getKey()).debug("Storing event data in Firestore");

    EventBackupData eventData = new EventBackupData();
    eventData.setEventType(eventType);
    eventData.setMessageFailureDateTimeInMillis(System.currentTimeMillis());
    eventData.setRoutingKey(routingKey);
    eventData.setGenericEvent(genericEvent);

    cloudDataStore.storeObject(
        eventBackupSchema,
        genericEvent.getEvent().getTransactionId(),
        eventData,
        genericEvent.getEvent().getTransactionId());

    log.with("id", routingKey.getKey()).debug("Stored event data");
  }
}
