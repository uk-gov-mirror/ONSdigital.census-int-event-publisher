package uk.gov.ons.ctp.common.event.persistence;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.test.util.ReflectionTestUtils;
import uk.gov.ons.ctp.common.FixtureHelper;
import uk.gov.ons.ctp.common.cloud.RetryableCloudDataStore;
import uk.gov.ons.ctp.common.event.EventPublisher.EventType;
import uk.gov.ons.ctp.common.event.model.FulfilmentRequestedEvent;
import uk.gov.ons.ctp.common.jackson.CustomObjectMapper;

@RunWith(MockitoJUnitRunner.class)
public class FirestoreEventPersistenceTest {

  private ObjectMapper objectMapper = new CustomObjectMapper();

  @InjectMocks private FirestoreEventPersistence persistence;

  @Mock RetryableCloudDataStore cloudDataStore;

  @Before
  public void setup() {
    ReflectionTestUtils.setField(persistence, "gcpProject", "testing");
    ReflectionTestUtils.setField(persistence, "eventBackupSchemaName", "backupcollection");
    ReflectionTestUtils.setField(persistence, "objectMapper", objectMapper);
    persistence.init();
  }

  @Test
  public void testPersistEvent() throws Exception {
    long startTime = System.currentTimeMillis();

    FulfilmentRequestedEvent event =
        FixtureHelper.loadClassFixtures(FulfilmentRequestedEvent[].class).get(0);

    ArgumentCaptor<EventBackupData> eventBackupCapture =
        ArgumentCaptor.forClass(EventBackupData.class);

    persistence.persistEvent(EventType.RESPONDENT_AUTHENTICATED, event);

    String expectedTransactionId = event.getEvent().getTransactionId();
    Mockito.verify(cloudDataStore, times(1))
        .storeObject(
            eq("testing-backupcollection"),
            eq(expectedTransactionId),
            eventBackupCapture.capture(),
            eq(expectedTransactionId));

    EventBackupData storedData = eventBackupCapture.getValue();
    assertEquals(EventType.RESPONDENT_AUTHENTICATED, storedData.getEventType());
    assertTrue(storedData.toString(), storedData.getMessageFailureDateTimeInMillis() >= startTime);
    assertTrue(
        storedData.toString(),
        storedData.getMessageFailureDateTimeInMillis() <= System.currentTimeMillis());
    assertNull(storedData.toString(), storedData.getMessageSentDateTimeInMillis());
    assertEquals(expectedTransactionId, storedData.getId());

    String eventJson = storedData.getEvent();

    FulfilmentRequestedEvent sentEvent =
        objectMapper.readValue(eventJson, FulfilmentRequestedEvent.class);

    assertEquals(event, sentEvent);
  }
}
