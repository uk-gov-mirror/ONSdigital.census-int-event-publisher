package uk.gov.ons.ctp.common.event;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
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
import uk.gov.ons.ctp.common.event.model.AddressModified;
import uk.gov.ons.ctp.common.event.model.AddressModifiedEvent;
import uk.gov.ons.ctp.common.event.model.CollectionCaseCompact;
import uk.gov.ons.ctp.common.event.model.EventPayload;
import uk.gov.ons.ctp.common.event.model.Feedback;
import uk.gov.ons.ctp.common.event.model.FeedbackEvent;
import uk.gov.ons.ctp.common.event.model.FulfilmentRequest;
import uk.gov.ons.ctp.common.event.model.FulfilmentRequestedEvent;
import uk.gov.ons.ctp.common.event.model.RespondentAuthenticatedEvent;
import uk.gov.ons.ctp.common.event.model.RespondentAuthenticatedResponse;
import uk.gov.ons.ctp.common.event.model.RespondentRefusalDetails;
import uk.gov.ons.ctp.common.event.model.RespondentRefusalEvent;
import uk.gov.ons.ctp.common.event.model.SurveyLaunchedEvent;
import uk.gov.ons.ctp.common.event.model.SurveyLaunchedResponse;

@RunWith(MockitoJUnitRunner.class)
public class EventPublisherTest {

  private static final UUID CASE_ID = UUID.fromString("dc4477d1-dd3f-4c69-b181-7ff725dc9fa4");
  private static final String QUESTIONNAIRE_ID = "1110000009";
  private static final String UPRN_1 = "1";
  private static final String UPRN_2 = "2";

  @InjectMocks private EventPublisher eventPublisher;
  @Mock private RabbitTemplate template;
  @Mock private SpringRabbitEventSender sender;

  /** Test event message with SurveyLaunchedResponse payload */
  @Test
  public void sendEventSurveyLaunchedPayload() throws Exception {

    SurveyLaunchedResponse surveyLaunchedResponse =
        SurveyLaunchedResponse.builder().questionnaireId(QUESTIONNAIRE_ID).caseId(CASE_ID).build();

    ArgumentCaptor<SurveyLaunchedEvent> eventCapture =
        ArgumentCaptor.forClass(SurveyLaunchedEvent.class);

    String transactionId =
        eventPublisher.sendEvent(
            EventType.SURVEY_LAUNCHED, Source.RESPONDENT_HOME, Channel.RH, surveyLaunchedResponse);

    RoutingKey routingKey = RoutingKey.forType(EventType.RESPONDENT_AUTHENTICATED);
    verify(sender, times(1)).sendEvent(eq(routingKey), eventCapture.capture());
    SurveyLaunchedEvent event = eventCapture.getValue();

    assertEquals(event.getEvent().getTransactionId(), transactionId);
    assertThat(UUID.fromString(event.getEvent().getTransactionId()), instanceOf(UUID.class));
    assertEquals(EventPublisher.EventType.SURVEY_LAUNCHED, event.getEvent().getType());
    assertEquals(EventPublisher.Source.RESPONDENT_HOME, event.getEvent().getSource());
    assertEquals(EventPublisher.Channel.RH, event.getEvent().getChannel());
    assertThat(event.getEvent().getDateTime(), instanceOf(Date.class));
    assertEquals(CASE_ID, event.getPayload().getResponse().getCaseId());
    assertEquals(QUESTIONNAIRE_ID, event.getPayload().getResponse().getQuestionnaireId());
  }

  /** Test event message with RespondentAuthenticatedResponse payload */
  @Test
  public void sendEventRespondentAuthenticatedPayload() throws Exception {

    RespondentAuthenticatedResponse respondentAuthenticatedResponse =
        RespondentAuthenticatedResponse.builder()
            .questionnaireId(QUESTIONNAIRE_ID)
            .caseId(CASE_ID)
            .build();

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

    assertEquals(event.getEvent().getTransactionId(), transactionId);
    assertThat(UUID.fromString(event.getEvent().getTransactionId()), instanceOf(UUID.class));
    assertEquals(EventPublisher.EventType.RESPONDENT_AUTHENTICATED, event.getEvent().getType());
    assertEquals(EventPublisher.Source.RESPONDENT_HOME, event.getEvent().getSource());
    assertEquals(EventPublisher.Channel.RH, event.getEvent().getChannel());
    assertThat(event.getEvent().getDateTime(), instanceOf(Date.class));
    assertEquals(CASE_ID, event.getPayload().getResponse().getCaseId());
    assertEquals(QUESTIONNAIRE_ID, event.getPayload().getResponse().getQuestionnaireId());
  }

  /** Test event message with FulfilmentRequest payload */
  @Test
  public void sendEventFulfilmentRequestPayload() throws Exception {

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

    assertEquals(event.getEvent().getTransactionId(), transactionId);
    assertThat(UUID.fromString(event.getEvent().getTransactionId()), instanceOf(UUID.class));
    assertEquals(EventPublisher.EventType.FULFILMENT_REQUESTED, event.getEvent().getType());
    assertEquals(EventPublisher.Source.CONTACT_CENTRE_API, event.getEvent().getSource());
    assertEquals(EventPublisher.Channel.CC, event.getEvent().getChannel());
    assertNotNull(event.getEvent().getDateTime());
    assertEquals("id-123", event.getPayload().getFulfilmentRequest().getCaseId());
  }

  /** Test event message with RespondentRefusalDetails payload */
  @Test
  public void sendEventRespondentRefusalDetailsPayload() throws Exception {

    // Build fulfilment
    RespondentRefusalDetails respondentRefusalDetails = new RespondentRefusalDetails();
    respondentRefusalDetails.setAgentId("x1");
    respondentRefusalDetails.setType("EXTRAORDINARY_REFUSAL");

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

    assertEquals(event.getEvent().getTransactionId(), transactionId);
    assertThat(UUID.fromString(event.getEvent().getTransactionId()), instanceOf(UUID.class));
    assertEquals(EventPublisher.EventType.REFUSAL_RECEIVED, event.getEvent().getType());
    assertEquals(EventPublisher.Source.CONTACT_CENTRE_API, event.getEvent().getSource());
    assertEquals(EventPublisher.Channel.CC, event.getEvent().getChannel());
    assertNotNull(event.getEvent().getDateTime());

    RespondentRefusalDetails payloadDetails = event.getPayload().getRefusal();
    assertEquals(respondentRefusalDetails.getAgentId(), payloadDetails.getAgentId());
    assertEquals("EXTRAORDINARY_REFUSAL", payloadDetails.getType());
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
  public void sendEventAddressModificationPayload() throws Exception {

    AddressModified originalAddress = new AddressModified();
    AddressModified newAddress = new AddressModified();
    originalAddress.setUprn(UPRN_1);
    newAddress.setUprn(UPRN_2);

    AddressModification addressModification =
        AddressModification.builder()
            .collectionCase(new CollectionCaseCompact())
            .originalAddress(originalAddress)
            .newAddress(newAddress)
            .build();

    ArgumentCaptor<AddressModifiedEvent> eventCapture =
        ArgumentCaptor.forClass(AddressModifiedEvent.class);

    String transactionId =
        eventPublisher.sendEvent(
            EventType.ADDRESS_MODIFIED, Source.RESPONDENT_HOME, Channel.RH, addressModification);

    RoutingKey routingKey = RoutingKey.forType(EventType.ADDRESS_MODIFIED);
    verify(sender, times(1)).sendEvent(eq(routingKey), eventCapture.capture());
    AddressModifiedEvent event = eventCapture.getValue();

    assertEquals(event.getEvent().getTransactionId(), transactionId);
    assertThat(UUID.fromString(event.getEvent().getTransactionId()), instanceOf(UUID.class));
    assertEquals(EventPublisher.EventType.ADDRESS_MODIFIED, event.getEvent().getType());
    assertEquals(EventPublisher.Source.RESPONDENT_HOME, event.getEvent().getSource());
    assertEquals(EventPublisher.Channel.RH, event.getEvent().getChannel());
    assertThat(event.getEvent().getDateTime(), instanceOf(Date.class));
    assertEquals(
        originalAddress.getUprn(),
        event.getPayload().getAddressModification().getOriginalAddress().getUprn());
    assertEquals(
        newAddress.getUprn(),
        event.getPayload().getAddressModification().getNewAddress().getUprn());
  }

  /** Test event message with FeedbackResponse payload */
  @Test
  public void sendEventFeedbackPayload() throws Exception {

    Feedback feedbackResponse = new Feedback();
    feedbackResponse.setPageUrl("url-x");
    feedbackResponse.setPageTitle("randomPage");
    feedbackResponse.setFeedbackText("Bla bla bla");

    ArgumentCaptor<FeedbackEvent> eventCapture = ArgumentCaptor.forClass(FeedbackEvent.class);

    String transactionId =
        eventPublisher.sendEvent(
            EventType.FEEDBACK, Source.RESPONDENT_HOME, Channel.RH, feedbackResponse);

    RoutingKey routingKey = RoutingKey.forType(EventType.FEEDBACK);
    verify(sender, times(1)).sendEvent(eq(routingKey), eventCapture.capture());
    FeedbackEvent event = eventCapture.getValue();

    assertEquals(event.getEvent().getTransactionId(), transactionId);
    assertThat(UUID.fromString(event.getEvent().getTransactionId()), instanceOf(UUID.class));
    assertEquals(EventPublisher.EventType.FEEDBACK, event.getEvent().getType());
    assertEquals(EventPublisher.Source.RESPONDENT_HOME, event.getEvent().getSource());
    assertEquals(EventPublisher.Channel.RH, event.getEvent().getChannel());
    assertThat(event.getEvent().getDateTime(), instanceOf(Date.class));
    assertEquals("url-x", event.getPayload().getFeedback().getPageUrl());
    assertEquals("randomPage", event.getPayload().getFeedback().getPageTitle());
    assertEquals("Bla bla bla", event.getPayload().getFeedback().getFeedbackText());
  }
}
