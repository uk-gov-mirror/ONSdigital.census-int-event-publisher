package uk.gov.ons.ctp.common.event;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
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
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import uk.gov.ons.ctp.common.event.EventPublisher.Channel;
import uk.gov.ons.ctp.common.event.EventPublisher.EventType;
import uk.gov.ons.ctp.common.event.EventPublisher.RoutingKey;
import uk.gov.ons.ctp.common.event.EventPublisher.Source;
import uk.gov.ons.ctp.common.event.model.AddressModification;
import uk.gov.ons.ctp.common.event.model.AddressModifiedEvent;
import uk.gov.ons.ctp.common.event.model.AddressNotValid;
import uk.gov.ons.ctp.common.event.model.AddressNotValidEvent;
import uk.gov.ons.ctp.common.event.model.CaseEvent;
import uk.gov.ons.ctp.common.event.model.CollectionCase;
import uk.gov.ons.ctp.common.event.model.EventPayload;
import uk.gov.ons.ctp.common.event.model.Feedback;
import uk.gov.ons.ctp.common.event.model.FeedbackEvent;
import uk.gov.ons.ctp.common.event.model.FulfilmentRequest;
import uk.gov.ons.ctp.common.event.model.FulfilmentRequestedEvent;
import uk.gov.ons.ctp.common.event.model.GenericEvent;
import uk.gov.ons.ctp.common.event.model.QuestionnaireLinkedDetails;
import uk.gov.ons.ctp.common.event.model.QuestionnaireLinkedEvent;
import uk.gov.ons.ctp.common.event.model.RespondentAuthenticatedEvent;
import uk.gov.ons.ctp.common.event.model.RespondentAuthenticatedResponse;
import uk.gov.ons.ctp.common.event.model.RespondentRefusalDetails;
import uk.gov.ons.ctp.common.event.model.RespondentRefusalEvent;
import uk.gov.ons.ctp.common.event.model.SurveyLaunchedEvent;
import uk.gov.ons.ctp.common.event.model.SurveyLaunchedResponse;

@RunWith(MockitoJUnitRunner.class)
public class EventPublisherTest {

  @InjectMocks private EventPublisher eventPublisher;
  @Mock private RabbitTemplate template;
  @Mock private SpringRabbitEventSender sender;

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

  /** Test event message with SurveyLaunchedResponse payload */
  @Test
  public void sendEventSurveyLaunchedPayload() {
    SurveyLaunchedResponse surveyLaunchedResponse = EventFixture.createSurveyLaunchedResponse();

    ArgumentCaptor<SurveyLaunchedEvent> eventCapture =
        ArgumentCaptor.forClass(SurveyLaunchedEvent.class);

    String transactionId =
        eventPublisher.sendEvent(
            EventType.SURVEY_LAUNCHED, Source.RESPONDENT_HOME, Channel.RH, surveyLaunchedResponse);

    RoutingKey routingKey = RoutingKey.forType(EventType.RESPONDENT_AUTHENTICATED);
    verify(sender, times(1)).sendEvent(eq(routingKey), eventCapture.capture());
    SurveyLaunchedEvent event = eventCapture.getValue();
    assertHeader(
        event, transactionId, EventType.SURVEY_LAUNCHED, Source.RESPONDENT_HOME, Channel.RH);
    assertEquals(surveyLaunchedResponse, event.getPayload().getResponse());
  }

  /** Test event message with RespondentAuthenticatedResponse payload */
  @Test
  public void sendEventRespondentAuthenticatedPayload() {
    RespondentAuthenticatedResponse respondentAuthenticatedResponse =
        EventFixture.createRespondentAuthenticatedResponse();

    ArgumentCaptor<RespondentAuthenticatedEvent> eventCapture =
        ArgumentCaptor.forClass(RespondentAuthenticatedEvent.class);

    String transactionId =
        eventPublisher.sendEvent(
            EventType.RESPONDENT_AUTHENTICATED,
            Source.RESPONDENT_HOME,
            Channel.RH,
            respondentAuthenticatedResponse);

    RoutingKey routingKey = RoutingKey.forType(EventType.RESPONDENT_AUTHENTICATED);
    verify(sender, times(1)).sendEvent(eq(routingKey), eventCapture.capture());
    RespondentAuthenticatedEvent event = eventCapture.getValue();

    assertHeader(
        event,
        transactionId,
        EventType.RESPONDENT_AUTHENTICATED,
        Source.RESPONDENT_HOME,
        Channel.RH);
    assertEquals(respondentAuthenticatedResponse, event.getPayload().getResponse());
  }

  /** Test event message with FulfilmentRequest payload */
  @Test
  public void sendEventFulfilmentRequestPayload() {

    // Build fulfilment
    FulfilmentRequest fulfilmentRequest = new FulfilmentRequest();
    fulfilmentRequest.setCaseId("id-123");

    ArgumentCaptor<FulfilmentRequestedEvent> eventCapture =
        ArgumentCaptor.forClass(FulfilmentRequestedEvent.class);

    String transactionId =
        eventPublisher.sendEvent(
            EventType.FULFILMENT_REQUESTED,
            Source.CONTACT_CENTRE_API,
            Channel.CC,
            fulfilmentRequest);

    RoutingKey routingKey = RoutingKey.forType(EventType.FULFILMENT_REQUESTED);
    verify(sender, times(1)).sendEvent(eq(routingKey), eventCapture.capture());
    FulfilmentRequestedEvent event = eventCapture.getValue();

    assertHeader(
        event,
        transactionId,
        EventType.FULFILMENT_REQUESTED,
        Source.CONTACT_CENTRE_API,
        Channel.CC);
    assertEquals("id-123", event.getPayload().getFulfilmentRequest().getCaseId());
  }

  /** Test event message with RespondentRefusalDetails payload */
  @Test
  public void sendEventRespondentRefusalDetailsPayload() {
    RespondentRefusalDetails respondentRefusalDetails =
        EventFixture.createRespondentRefusalDetails();

    ArgumentCaptor<RespondentRefusalEvent> eventCapture =
        ArgumentCaptor.forClass(RespondentRefusalEvent.class);

    String transactionId =
        eventPublisher.sendEvent(
            EventType.REFUSAL_RECEIVED,
            Source.CONTACT_CENTRE_API,
            Channel.CC,
            respondentRefusalDetails);

    RoutingKey routingKey = RoutingKey.forType(EventType.REFUSAL_RECEIVED);
    verify(sender, times(1)).sendEvent(eq(routingKey), eventCapture.capture());
    RespondentRefusalEvent event = eventCapture.getValue();

    assertHeader(
        event, transactionId, EventType.REFUSAL_RECEIVED, Source.CONTACT_CENTRE_API, Channel.CC);
    assertEquals(respondentRefusalDetails, event.getPayload().getRefusal());
  }

  /** Test build of Respondent Authenticated event message with wrong pay load */
  @Test
  public void sendEventRespondentAuthenticatedWrongPayload() {

    boolean exceptionThrown = false;

    try {
      eventPublisher.sendEvent(
          EventType.ADDRESS_MODIFIED,
          Source.RECEIPT_SERVICE,
          Channel.CC,
          Mockito.mock(EventPayload.class));
    } catch (Exception e) {
      exceptionThrown = true;
      assertThat(e.getMessage(), containsString("incompatible for event type"));
    }

    assertTrue(exceptionThrown);
  }

  /** Test event message with SurveyLaunchedResponse payload */
  @Test
  public void sendEventAddressModificationPayload() {
    AddressModification addressModification = EventFixture.createAddressModification();

    ArgumentCaptor<AddressModifiedEvent> eventCapture =
        ArgumentCaptor.forClass(AddressModifiedEvent.class);

    String transactionId =
        eventPublisher.sendEvent(
            EventType.ADDRESS_MODIFIED, Source.RESPONDENT_HOME, Channel.RH, addressModification);

    RoutingKey routingKey = RoutingKey.forType(EventType.ADDRESS_MODIFIED);
    verify(sender, times(1)).sendEvent(eq(routingKey), eventCapture.capture());
    AddressModifiedEvent event = eventCapture.getValue();

    assertHeader(
        event, transactionId, EventType.ADDRESS_MODIFIED, Source.RESPONDENT_HOME, Channel.RH);
    assertEquals(addressModification, event.getPayload().getAddressModification());
  }

  @Test
  public void shouldSentAddressNotValid() {
    AddressNotValid payload = EventFixture.createAddressNotValid();

    ArgumentCaptor<AddressNotValidEvent> eventCapture =
        ArgumentCaptor.forClass(AddressNotValidEvent.class);

    String transactionId =
        eventPublisher.sendEvent(
            EventType.ADDRESS_NOT_VALID, Source.CONTACT_CENTRE_API, Channel.CC, payload);

    RoutingKey routingKey = RoutingKey.forType(EventType.ADDRESS_NOT_VALID);
    verify(sender).sendEvent(eq(routingKey), eventCapture.capture());
    AddressNotValidEvent event = eventCapture.getValue();

    assertHeader(
        event, transactionId, EventType.ADDRESS_NOT_VALID, Source.CONTACT_CENTRE_API, Channel.CC);
    assertEquals(payload, event.getPayload().getInvalidAddress());
  }

  private void assertSendCase(EventType type) {
    CollectionCase payload = EventFixture.createCollectionCase();

    ArgumentCaptor<CaseEvent> eventCapture = ArgumentCaptor.forClass(CaseEvent.class);

    String transactionId =
        eventPublisher.sendEvent(type, Source.CONTACT_CENTRE_API, Channel.CC, payload);

    RoutingKey routingKey = RoutingKey.forType(type);
    verify(sender).sendEvent(eq(routingKey), eventCapture.capture());
    CaseEvent event = eventCapture.getValue();

    assertHeader(event, transactionId, type, Source.CONTACT_CENTRE_API, Channel.CC);
    assertEquals(payload, event.getPayload().getCollectionCase());
  }

  @Test
  public void shouldSendCaseCreated() {
    assertSendCase(EventType.CASE_CREATED);
  }

  @Test
  public void shouldSendCaseUpdated() {
    assertSendCase(EventType.CASE_UPDATED);
  }

  /** Test event message with FeedbackResponse payload */
  @Test
  public void sendEventFeedbackPayload() {
    Feedback feedbackResponse = EventFixture.createFeedback();

    ArgumentCaptor<FeedbackEvent> eventCapture = ArgumentCaptor.forClass(FeedbackEvent.class);

    String transactionId =
        eventPublisher.sendEvent(
            EventType.FEEDBACK, Source.RESPONDENT_HOME, Channel.RH, feedbackResponse);

    RoutingKey routingKey = RoutingKey.forType(EventType.FEEDBACK);
    verify(sender, times(1)).sendEvent(eq(routingKey), eventCapture.capture());
    FeedbackEvent event = eventCapture.getValue();

    assertHeader(event, transactionId, EventType.FEEDBACK, Source.RESPONDENT_HOME, Channel.RH);
    assertEquals(feedbackResponse, event.getPayload().getFeedback());
  }

  @Test
  public void sendQuestionnaireLinkedPayload() {
    QuestionnaireLinkedDetails questionnaireLinked =
        EventFixture.createQuestionnaireLinkedDetails();

    ArgumentCaptor<QuestionnaireLinkedEvent> eventCapture =
        ArgumentCaptor.forClass(QuestionnaireLinkedEvent.class);

    String transactionId =
        eventPublisher.sendEvent(
            EventType.QUESTIONNAIRE_LINKED,
            Source.RESPONDENT_HOME,
            Channel.RH,
            questionnaireLinked);

    RoutingKey routingKey = RoutingKey.forType(EventType.QUESTIONNAIRE_LINKED);
    verify(sender, times(1)).sendEvent(eq(routingKey), eventCapture.capture());
    QuestionnaireLinkedEvent event = eventCapture.getValue();

    assertHeader(
        event, transactionId, EventType.QUESTIONNAIRE_LINKED, Source.RESPONDENT_HOME, Channel.RH);
    assertEquals(questionnaireLinked, event.getPayload().getUac());
  }
}
