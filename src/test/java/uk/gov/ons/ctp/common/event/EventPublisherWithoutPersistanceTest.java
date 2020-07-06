package uk.gov.ons.ctp.common.event;

import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import uk.gov.ons.ctp.common.FixtureHelper;
import uk.gov.ons.ctp.common.error.CTPException;
import uk.gov.ons.ctp.common.event.EventPublisher.Channel;
import uk.gov.ons.ctp.common.event.EventPublisher.EventType;
import uk.gov.ons.ctp.common.event.EventPublisher.Source;
import uk.gov.ons.ctp.common.event.model.SurveyLaunchedResponse;

/** EventPublisher tests specific to the scenario in which event persistence is turned off. */
@RunWith(MockitoJUnitRunner.class)
public class EventPublisherWithoutPersistanceTest {

  @InjectMocks private EventPublisher eventPublisher;
  @Mock private RabbitTemplate template;
  @Mock private SpringRabbitEventSender sender;

  @Test
  public void eventSendingFailsWithException() throws CTPException {
    SurveyLaunchedResponse surveyLaunchedResponse = loadJson(SurveyLaunchedResponse[].class);

    Mockito.doThrow(new AmqpException("Failed to send")).when(sender).sendEvent(any(), any());

    Exception e =
        assertThrows(
            Exception.class,
            () -> {
              eventPublisher.sendEvent(
                  EventType.SURVEY_LAUNCHED,
                  Source.RESPONDENT_HOME,
                  Channel.RH,
                  surveyLaunchedResponse);
            });
    assertTrue(e.getMessage(), e.getMessage().matches(".* failed to send .*"));
  }

  private <T> T loadJson(Class<T[]> clazz) {
    return FixtureHelper.loadPackageFixtures(clazz).get(0);
  }
}
