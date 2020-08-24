package uk.gov.ons.ctp.common.event.persistence;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.godaddy.logging.Logger;
import com.godaddy.logging.LoggerFactory;
import javax.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uk.gov.ons.ctp.common.cloud.RetryableCloudDataStore;
import uk.gov.ons.ctp.common.error.CTPException;
import uk.gov.ons.ctp.common.event.EventPublishException;
import uk.gov.ons.ctp.common.event.EventPublisher.EventType;
import uk.gov.ons.ctp.common.event.model.GenericEvent;
import uk.gov.ons.ctp.common.jackson.CustomObjectMapper;

/**
 * This class saves details about an event which Rabbit failed to sent into a Firestore collection.
 */
@Service
public class FirestoreEventPersistence implements EventPersistence {

  private static final Logger log = LoggerFactory.getLogger(FirestoreEventPersistence.class);

  private RetryableCloudDataStore cloudDataStore;
  private CustomObjectMapper objectMapper;

  @Value("${GOOGLE_CLOUD_PROJECT}")
  String gcpProject;

  @Value("${cloud-storage.event-backup-schema-name}")
  String eventBackupSchemaName;

  String eventBackupSchema;

  @PostConstruct
  public void init() {
    eventBackupSchema = gcpProject + "-" + eventBackupSchemaName.toLowerCase();
  }

  @Autowired
  public FirestoreEventPersistence(
      RetryableCloudDataStore cloudDataStore, CustomObjectMapper objectMapper) {
    this.cloudDataStore = cloudDataStore;
    this.objectMapper = objectMapper;
  }

  private String serialise(Object obj) {
    try {
      return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(obj);
    } catch (JsonProcessingException e) {
      throw new EventPublishException("Failed to serialise event to JSON", e);
    }
  }

  @Override
  public void persistEvent(EventType eventType, GenericEvent genericEvent) throws CTPException {
    String id = genericEvent.getEvent().getTransactionId();

    log.with("id", id).debug("Storing event data in Firestore");

    EventBackupData eventData = new EventBackupData();
    eventData.setEventType(eventType);
    eventData.setMessageFailureDateTimeInMillis(System.currentTimeMillis());
    eventData.setId(id);
    eventData.setEvent(serialise(genericEvent));

    cloudDataStore.storeObject(
        eventBackupSchema,
        genericEvent.getEvent().getTransactionId(),
        eventData,
        genericEvent.getEvent().getTransactionId());

    log.with("id", id).debug("Stored event data");
  }
}
