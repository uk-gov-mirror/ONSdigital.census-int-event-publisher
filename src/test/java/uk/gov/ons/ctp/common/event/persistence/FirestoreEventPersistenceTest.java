package uk.gov.ons.ctp.common.event.persistence;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;

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
import uk.gov.ons.ctp.common.event.EventPublisher.RoutingKey;
import uk.gov.ons.ctp.common.event.model.FulfilmentRequestedEvent;

@RunWith(MockitoJUnitRunner.class)
public class FirestoreEventPersistenceTest {

  @InjectMocks private FirestoreEventPersistence persistence;

  @Mock RetryableCloudDataStore cloudDataStore;

  @Test
  public void testPersistEvent() throws Exception {
    long startTime = System.currentTimeMillis();

    ReflectionTestUtils.setField(persistence, "gcpProject", "testing");
    ReflectionTestUtils.setField(persistence, "eventBackupSchemaName", "backupcollection");
    persistence.init();

    RoutingKey routingKey = RoutingKey.forType(EventType.RESPONDENT_AUTHENTICATED);
    FulfilmentRequestedEvent event =
        FixtureHelper.loadClassFixtures(FulfilmentRequestedEvent[].class).get(0);

    ArgumentCaptor<EventBackupData> eventBackupCapture =
        ArgumentCaptor.forClass(EventBackupData.class);

    persistence.persistEvent(EventType.RESPONDENT_AUTHENTICATED, routingKey, event);

    String expectedTransactionId = event.getEvent().getTransactionId();
    Mockito.verify(cloudDataStore, times(1))
        .storeObject(
            eq("testing-backupcollection"),
            eq(expectedTransactionId),
            eventBackupCapture.capture(),
            eq(expectedTransactionId));

    assertEquals(EventType.RESPONDENT_AUTHENTICATED, eventBackupCapture.getValue().getEventType());
    assertTrue(
        eventBackupCapture.getValue().toString(),
        eventBackupCapture.getValue().getMessageFailureDateTimeInMillis() >= startTime);
    assertTrue(
        eventBackupCapture.getValue().toString(),
        eventBackupCapture.getValue().getMessageFailureDateTimeInMillis()
            <= System.currentTimeMillis());
    assertNull(
        eventBackupCapture.getValue().toString(),
        eventBackupCapture.getValue().getMessageSentDateTimeInMillis());
    assertEquals(routingKey, eventBackupCapture.getValue().getRoutingKey());
    assertEquals(event, eventBackupCapture.getValue().getGenericEvent());
  }
}
