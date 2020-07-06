package uk.gov.ons.ctp.common.event;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.Date;
import java.util.UUID;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import uk.gov.ons.ctp.common.FixtureHelper;
import uk.gov.ons.ctp.common.error.CTPException;
import uk.gov.ons.ctp.common.error.CTPException.Fault;
import uk.gov.ons.ctp.common.event.EventPublisher.Channel;
import uk.gov.ons.ctp.common.event.EventPublisher.EventType;
import uk.gov.ons.ctp.common.event.EventPublisher.RoutingKey;
import uk.gov.ons.ctp.common.event.EventPublisher.Source;
import uk.gov.ons.ctp.common.event.model.GenericEvent;
import uk.gov.ons.ctp.common.event.model.SurveyLaunchedEvent;
import uk.gov.ons.ctp.common.event.model.SurveyLaunchedResponse;
import uk.gov.ons.ctp.common.event.persistence.FirestoreEventPersistence;

/**
 * EventPublisher tests specific to failure scenarios when running with event persistence enabled.
 */
@RunWith(MockitoJUnitRunner.class)
public class EventPublisherWithPersistenceTest {

  @InjectMocks private EventPublisher eventPublisher;
  @Mock private RabbitTemplate template;
  @Mock private SpringRabbitEventSender sender;
  @Mock private FirestoreEventPersistence eventPersistence;

  private void assertHeader(
      GenericEvent event,
      String transactionId,
      EventType expectedType,
      Source expectedSource,
      Channel expectedChannel) {
    assertEquals(transactionId, event.getEvent().getTransactionId());
    assertThat(UUID.fromString(event.getEvent().getTransactionId()), instanceOf(UUID.class));
    assertEquals(expectedType, event.getEvent().getType());
    assertEquals(expectedSource, event.getEvent().getSource());
    assertEquals(expectedChannel, event.getEvent().getChannel());
    assertThat(event.getEvent().getDateTime(), instanceOf(Date.class));
  }

  @Test
  public void eventPersistedWhenRabbitFails() throws CTPException {
    SurveyLaunchedResponse surveyLaunchedResponse = loadJson(SurveyLaunchedResponse[].class);

    ArgumentCaptor<SurveyLaunchedEvent> eventCapture =
        ArgumentCaptor.forClass(SurveyLaunchedEvent.class);

    Mockito.doThrow(new AmqpException("Failed to send")).when(sender).sendEvent(any(), any());

    String transactionId =
        eventPublisher.sendEvent(
            EventType.SURVEY_LAUNCHED, Source.RESPONDENT_HOME, Channel.RH, surveyLaunchedResponse);

    // Verify that the event was persistent following simulated Rabbit failure
    RoutingKey routingKey = RoutingKey.forType(EventType.RESPONDENT_AUTHENTICATED);
    verify(eventPersistence, times(1))
        .persistEvent(eq(EventType.SURVEY_LAUNCHED), eq(routingKey), eventCapture.capture());
    SurveyLaunchedEvent event = eventCapture.getValue();
    assertHeader(
        event, transactionId, EventType.SURVEY_LAUNCHED, Source.RESPONDENT_HOME, Channel.RH);
    assertEquals(surveyLaunchedResponse, event.getPayload().getResponse());
  }

  @Test
  public void exceptionThrownWhenRabbitAndFirestoreFail() throws CTPException {
    SurveyLaunchedResponse surveyLaunchedResponse = loadJson(SurveyLaunchedResponse[].class);

    Mockito.doThrow(new AmqpException("Failed to send")).when(sender).sendEvent(any(), any());
    Mockito.doThrow(new CTPException(Fault.SYSTEM_ERROR, "Firestore broken"))
        .when(eventPersistence)
        .persistEvent(any(), any(), any());

    Exception e =
        assertThrows(
            Exception.class,
            () ->
                eventPublisher.sendEvent(
                    EventType.SURVEY_LAUNCHED,
                    Source.RESPONDENT_HOME,
                    Channel.RH,
                    surveyLaunchedResponse));
    assertTrue(
        e.getMessage(),
        e.getMessage().matches(".* event persistence failed following Rabbit failure"));
  }

  private <T> T loadJson(Class<T[]> clazz) {
    return FixtureHelper.loadPackageFixtures(clazz).get(0);
  }
}
