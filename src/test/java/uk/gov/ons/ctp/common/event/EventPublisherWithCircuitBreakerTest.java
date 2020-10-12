package uk.gov.ons.ctp.common.event;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static uk.gov.ons.ctp.common.event.EventPublisherTestUtil.assertHeader;

import java.util.function.Function;
import java.util.function.Supplier;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.cloud.client.circuitbreaker.CircuitBreaker;
import uk.gov.ons.ctp.common.FixtureHelper;
import uk.gov.ons.ctp.common.event.EventPublisher.Channel;
import uk.gov.ons.ctp.common.event.EventPublisher.EventType;
import uk.gov.ons.ctp.common.event.EventPublisher.RoutingKey;
import uk.gov.ons.ctp.common.event.EventPublisher.Source;
import uk.gov.ons.ctp.common.event.model.SurveyLaunchedEvent;
import uk.gov.ons.ctp.common.event.model.SurveyLaunchedResponse;
import uk.gov.ons.ctp.common.event.persistence.FirestoreEventPersistence;

/** EventPublisher tests with circuit breaker */
@RunWith(MockitoJUnitRunner.class)
public class EventPublisherWithCircuitBreakerTest {

  @InjectMocks private EventPublisher eventPublisher;
  @Mock private RabbitTemplate template;
  @Mock private SpringRabbitEventSender sender;
  @Mock private FirestoreEventPersistence eventPersistence;
  @Mock private CircuitBreaker circuitBreaker;

  @Captor private ArgumentCaptor<SurveyLaunchedEvent> surveyLaunchedEventCaptor;

  private void mockCircuitBreakerRun() {
    doAnswer(
            new Answer<Object>() {
              @SuppressWarnings("unchecked")
              @Override
              public Object answer(InvocationOnMock invocation) throws Throwable {
                Object[] args = invocation.getArguments();
                Supplier<Object> runner = (Supplier<Object>) args[0];
                return runner.get();
              }
            })
        .when(circuitBreaker)
        .run(any(), any());
  }

  private void mockCircuitBreakerFail() {
    doAnswer(
            new Answer<Object>() {
              @SuppressWarnings("unchecked")
              @Override
              public Object answer(InvocationOnMock invocation) throws Throwable {
                Object[] args = invocation.getArguments();
                Supplier<Object> runner = (Supplier<Object>) args[0];
                Function<Throwable, Object> fallback = (Function<Throwable, Object>) args[1];

                try {
                  runner.get();
                } catch (Exception e) {
                  fallback.apply(e);
                }
                return null;
              }
            })
        .when(circuitBreaker)
        .run(any(), any());
  }

  @Test
  public void shouldSendEventThroughCircuitBreaker() throws Exception {
    mockCircuitBreakerRun();
    SurveyLaunchedResponse surveyLaunchedResponse = loadJson(SurveyLaunchedResponse[].class);

    String transactionId =
        eventPublisher.sendEvent(
            EventType.SURVEY_LAUNCHED, Source.RESPONDENT_HOME, Channel.RH, surveyLaunchedResponse);

    RoutingKey routingKey = RoutingKey.forType(EventType.SURVEY_LAUNCHED);
    verify(sender, times(1)).sendEvent(eq(routingKey), surveyLaunchedEventCaptor.capture());
    SurveyLaunchedEvent event = surveyLaunchedEventCaptor.getValue();
    assertHeader(
        event, transactionId, EventType.SURVEY_LAUNCHED, Source.RESPONDENT_HOME, Channel.RH);
    assertEquals(surveyLaunchedResponse, event.getPayload().getResponse());

    // since it succeeded, the event is NOT sent to firestore
    verify(eventPersistence, never()).persistEvent(eq(EventType.SURVEY_LAUNCHED), any());
  }

  @Test
  public void shouldNotSendEventToRabbitThroughCircuitBreakerWhenRabbitFails() throws Exception {
    mockCircuitBreakerFail();
    Mockito.doThrow(new RuntimeException("rabbit fail")).when(sender).sendEvent(any(), any());

    SurveyLaunchedResponse surveyLaunchedResponse = loadJson(SurveyLaunchedResponse[].class);

    eventPublisher.sendEvent(
        EventType.SURVEY_LAUNCHED, Source.RESPONDENT_HOME, Channel.RH, surveyLaunchedResponse);

    RoutingKey routingKey = RoutingKey.forType(EventType.SURVEY_LAUNCHED);
    verify(sender).sendEvent(eq(routingKey), surveyLaunchedEventCaptor.capture());

    // since it failed, the event is sent to firestore
    verify(eventPersistence).persistEvent(eq(EventType.SURVEY_LAUNCHED), any());
  }

  private <T> T loadJson(Class<T[]> clazz) {
    return FixtureHelper.loadPackageFixtures(clazz).get(0);
  }
}
