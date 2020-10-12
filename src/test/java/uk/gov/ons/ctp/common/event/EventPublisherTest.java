package uk.gov.ons.ctp.common.event;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static uk.gov.ons.ctp.common.event.EventPublisherTestUtil.assertHeader;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Date;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.test.util.ReflectionTestUtils;
import uk.gov.ons.ctp.common.FixtureHelper;
import uk.gov.ons.ctp.common.event.EventPublisher.Channel;
import uk.gov.ons.ctp.common.event.EventPublisher.EventType;
import uk.gov.ons.ctp.common.event.EventPublisher.RoutingKey;
import uk.gov.ons.ctp.common.event.EventPublisher.Source;
import uk.gov.ons.ctp.common.event.model.AddressModification;
import uk.gov.ons.ctp.common.event.model.AddressModifiedEvent;
import uk.gov.ons.ctp.common.event.model.AddressNotValid;
import uk.gov.ons.ctp.common.event.model.AddressNotValidEvent;
import uk.gov.ons.ctp.common.event.model.AddressTypeChanged;
import uk.gov.ons.ctp.common.event.model.AddressTypeChangedEvent;
import uk.gov.ons.ctp.common.event.model.CaseEvent;
import uk.gov.ons.ctp.common.event.model.CollectionCase;
import uk.gov.ons.ctp.common.event.model.EventPayload;
import uk.gov.ons.ctp.common.event.model.Feedback;
import uk.gov.ons.ctp.common.event.model.FeedbackEvent;
import uk.gov.ons.ctp.common.event.model.FulfilmentRequest;
import uk.gov.ons.ctp.common.event.model.FulfilmentRequestedEvent;
import uk.gov.ons.ctp.common.event.model.GenericEvent;
import uk.gov.ons.ctp.common.event.model.Header;
import uk.gov.ons.ctp.common.event.model.NewAddress;
import uk.gov.ons.ctp.common.event.model.NewAddressReportedEvent;
import uk.gov.ons.ctp.common.event.model.QuestionnaireLinkedDetails;
import uk.gov.ons.ctp.common.event.model.QuestionnaireLinkedEvent;
import uk.gov.ons.ctp.common.event.model.RespondentAuthenticatedEvent;
import uk.gov.ons.ctp.common.event.model.RespondentAuthenticatedResponse;
import uk.gov.ons.ctp.common.event.model.RespondentRefusalDetails;
import uk.gov.ons.ctp.common.event.model.RespondentRefusalEvent;
import uk.gov.ons.ctp.common.event.model.SurveyLaunchedEvent;
import uk.gov.ons.ctp.common.event.model.SurveyLaunchedResponse;
import uk.gov.ons.ctp.common.event.model.UAC;
import uk.gov.ons.ctp.common.event.model.UACEvent;
import uk.gov.ons.ctp.common.event.persistence.EventBackupData;
import uk.gov.ons.ctp.common.event.persistence.FirestoreEventPersistence;
import uk.gov.ons.ctp.common.jackson.CustomObjectMapper;

@RunWith(MockitoJUnitRunner.class)
public class EventPublisherTest {

  @InjectMocks private EventPublisher eventPublisher;
  @Mock private RabbitTemplate template;
  @Mock private SpringRabbitEventSender sender;
  @Mock private FirestoreEventPersistence eventPersistence;

  ObjectMapper objectMapper = new CustomObjectMapper();

  @Captor private ArgumentCaptor<FulfilmentRequestedEvent> fulfilmentRequestedEventCaptor;
  @Captor private ArgumentCaptor<RespondentAuthenticatedEvent> respondentAuthenticatedEventCaptor;
  @Captor private ArgumentCaptor<RespondentRefusalEvent> respondentRefusalEventCaptor;
  @Captor private ArgumentCaptor<UACEvent> uacEventCaptor;
  @Captor private ArgumentCaptor<SurveyLaunchedEvent> surveyLaunchedEventCaptor;
  @Captor private ArgumentCaptor<AddressModifiedEvent> addressModifiedEventCaptor;
  @Captor private ArgumentCaptor<AddressNotValidEvent> addressNotValidEventCaptor;
  @Captor private ArgumentCaptor<AddressTypeChangedEvent> addressTypeChangedEventCaptor;
  @Captor private ArgumentCaptor<CaseEvent> caseEventCaptor;
  @Captor private ArgumentCaptor<FeedbackEvent> feedbackEventCaptor;
  @Captor private ArgumentCaptor<NewAddressReportedEvent> newAddressReportedEventCaptor;
  @Captor private ArgumentCaptor<QuestionnaireLinkedEvent> questionnaireLinkedEventCaptor;

  private Date startOfTestDateTime;

  @Before
  public void setup() {
    this.startOfTestDateTime = new Date();
  }

  @Test
  public void shouldCreateWithoutEventPersistence() {
    EventPublisher ep = EventPublisher.createWithoutEventPersistence(sender);
    assertNull(ReflectionTestUtils.getField(ep, "eventPersistence"));
    assertEquals(sender, ReflectionTestUtils.getField(ep, "sender"));
  }

  @Test
  public void shouldCreateWithEventPersistence() {
    EventPublisher ep = EventPublisher.createWithEventPersistence(sender, eventPersistence, null);
    assertNotNull(ReflectionTestUtils.getField(ep, "eventPersistence"));
    assertEquals(sender, ReflectionTestUtils.getField(ep, "sender"));
  }

  @Test
  public void sendEventSurveyLaunchedPayload() {
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
  }

  @Test
  public void sendEventRespondentAuthenticatedPayload() {
    RespondentAuthenticatedResponse respondentAuthenticatedResponse =
        loadJson(RespondentAuthenticatedResponse[].class);

    String transactionId =
        eventPublisher.sendEvent(
            EventType.RESPONDENT_AUTHENTICATED,
            Source.RESPONDENT_HOME,
            Channel.RH,
            respondentAuthenticatedResponse);

    RoutingKey routingKey = RoutingKey.forType(EventType.RESPONDENT_AUTHENTICATED);
    verify(sender, times(1))
        .sendEvent(eq(routingKey), respondentAuthenticatedEventCaptor.capture());
    RespondentAuthenticatedEvent event = respondentAuthenticatedEventCaptor.getValue();

    assertHeader(
        event,
        transactionId,
        EventType.RESPONDENT_AUTHENTICATED,
        Source.RESPONDENT_HOME,
        Channel.RH);
    assertEquals(respondentAuthenticatedResponse, event.getPayload().getResponse());
  }

  @Test
  public void sendEventFulfilmentRequestPayload() {
    FulfilmentRequest fulfilmentRequest = loadJson(FulfilmentRequest[].class);

    String transactionId =
        eventPublisher.sendEvent(
            EventType.FULFILMENT_REQUESTED,
            Source.CONTACT_CENTRE_API,
            Channel.CC,
            fulfilmentRequest);

    RoutingKey routingKey = RoutingKey.forType(EventType.FULFILMENT_REQUESTED);
    verify(sender, times(1)).sendEvent(eq(routingKey), fulfilmentRequestedEventCaptor.capture());
    FulfilmentRequestedEvent event = fulfilmentRequestedEventCaptor.getValue();

    assertHeader(
        event,
        transactionId,
        EventType.FULFILMENT_REQUESTED,
        Source.CONTACT_CENTRE_API,
        Channel.CC);
    assertEquals("id-123", event.getPayload().getFulfilmentRequest().getCaseId());
  }

  @Test
  public void sendEventRespondentRefusalDetailsPayload() {
    RespondentRefusalDetails respondentRefusalDetails = loadJson(RespondentRefusalDetails[].class);

    String transactionId =
        eventPublisher.sendEvent(
            EventType.REFUSAL_RECEIVED,
            Source.CONTACT_CENTRE_API,
            Channel.CC,
            respondentRefusalDetails);

    RoutingKey routingKey = RoutingKey.forType(EventType.REFUSAL_RECEIVED);
    verify(sender, times(1)).sendEvent(eq(routingKey), respondentRefusalEventCaptor.capture());
    RespondentRefusalEvent event = respondentRefusalEventCaptor.getValue();

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

  @Test
  public void sendEventAddressModificationPayload() {
    AddressModification addressModification = loadJson(AddressModification[].class);

    String transactionId =
        eventPublisher.sendEvent(
            EventType.ADDRESS_MODIFIED, Source.RESPONDENT_HOME, Channel.RH, addressModification);

    RoutingKey routingKey = RoutingKey.forType(EventType.ADDRESS_MODIFIED);
    verify(sender, times(1)).sendEvent(eq(routingKey), addressModifiedEventCaptor.capture());
    AddressModifiedEvent event = addressModifiedEventCaptor.getValue();

    assertHeader(
        event, transactionId, EventType.ADDRESS_MODIFIED, Source.RESPONDENT_HOME, Channel.RH);
    assertEquals(addressModification, event.getPayload().getAddressModification());
  }

  @Test
  public void shouldSentAddressNotValid() {
    AddressNotValid payload = loadJson(AddressNotValid[].class);

    String transactionId =
        eventPublisher.sendEvent(
            EventType.ADDRESS_NOT_VALID, Source.CONTACT_CENTRE_API, Channel.CC, payload);

    RoutingKey routingKey = RoutingKey.forType(EventType.ADDRESS_NOT_VALID);
    verify(sender).sendEvent(eq(routingKey), addressNotValidEventCaptor.capture());
    AddressNotValidEvent event = addressNotValidEventCaptor.getValue();

    assertHeader(
        event, transactionId, EventType.ADDRESS_NOT_VALID, Source.CONTACT_CENTRE_API, Channel.CC);
    assertEquals(payload, event.getPayload().getInvalidAddress());
  }

  private void assertSendCase(EventType type) {
    CollectionCase payload = loadJson(CollectionCase[].class);

    String transactionId =
        eventPublisher.sendEvent(type, Source.CONTACT_CENTRE_API, Channel.CC, payload);

    RoutingKey routingKey = RoutingKey.forType(type);
    verify(sender).sendEvent(eq(routingKey), caseEventCaptor.capture());
    CaseEvent event = caseEventCaptor.getValue();

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

  @Test
  public void shouldSendAddressTypeChanged() {
    AddressTypeChanged payload = loadJson(AddressTypeChanged[].class);

    String transactionId =
        eventPublisher.sendEvent(
            EventType.ADDRESS_TYPE_CHANGED, Source.CONTACT_CENTRE_API, Channel.CC, payload);

    RoutingKey routingKey = RoutingKey.forType(EventType.ADDRESS_TYPE_CHANGED);
    verify(sender, times(1)).sendEvent(eq(routingKey), addressTypeChangedEventCaptor.capture());
    AddressTypeChangedEvent event = addressTypeChangedEventCaptor.getValue();

    assertHeader(
        event,
        transactionId,
        EventType.ADDRESS_TYPE_CHANGED,
        Source.CONTACT_CENTRE_API,
        Channel.CC);
    assertEquals(payload, event.getPayload().getAddressTypeChange());
  }

  @Test
  public void sendEventFeedbackPayload() {
    Feedback feedbackResponse = loadJson(Feedback[].class);

    String transactionId =
        eventPublisher.sendEvent(
            EventType.FEEDBACK, Source.RESPONDENT_HOME, Channel.RH, feedbackResponse);

    RoutingKey routingKey = RoutingKey.forType(EventType.FEEDBACK);
    verify(sender, times(1)).sendEvent(eq(routingKey), feedbackEventCaptor.capture());
    FeedbackEvent event = feedbackEventCaptor.getValue();

    assertHeader(event, transactionId, EventType.FEEDBACK, Source.RESPONDENT_HOME, Channel.RH);
    assertEquals(feedbackResponse, event.getPayload().getFeedback());
  }

  @Test
  public void sendQuestionnaireLinkedPayload() {
    QuestionnaireLinkedDetails questionnaireLinked = loadJson(QuestionnaireLinkedDetails[].class);

    String transactionId =
        eventPublisher.sendEvent(
            EventType.QUESTIONNAIRE_LINKED,
            Source.RESPONDENT_HOME,
            Channel.RH,
            questionnaireLinked);

    RoutingKey routingKey = RoutingKey.forType(EventType.QUESTIONNAIRE_LINKED);
    verify(sender, times(1)).sendEvent(eq(routingKey), questionnaireLinkedEventCaptor.capture());
    QuestionnaireLinkedEvent event = questionnaireLinkedEventCaptor.getValue();

    assertHeader(
        event, transactionId, EventType.QUESTIONNAIRE_LINKED, Source.RESPONDENT_HOME, Channel.RH);
    assertEquals(questionnaireLinked, event.getPayload().getUac());
  }

  @Test
  public void shouldSendNewAddressReported() {
    NewAddress payload = loadJson(NewAddress[].class);

    String transactionId =
        eventPublisher.sendEvent(
            EventType.NEW_ADDRESS_REPORTED, Source.CONTACT_CENTRE_API, Channel.CC, payload);

    RoutingKey routingKey = RoutingKey.forType(EventType.NEW_ADDRESS_REPORTED);
    verify(sender, times(1)).sendEvent(eq(routingKey), newAddressReportedEventCaptor.capture());
    NewAddressReportedEvent event = newAddressReportedEventCaptor.getValue();

    assertHeader(
        event,
        transactionId,
        EventType.NEW_ADDRESS_REPORTED,
        Source.CONTACT_CENTRE_API,
        Channel.CC);
    assertEquals(payload, event.getPayload().getNewAddress());
  }

  private void assertSendUac(EventType type) {
    UAC payload = loadJson(UAC[].class);

    String transactionId =
        eventPublisher.sendEvent(type, Source.CONTACT_CENTRE_API, Channel.CC, payload);

    RoutingKey routingKey = RoutingKey.forType(type);
    verify(sender).sendEvent(eq(routingKey), uacEventCaptor.capture());
    UACEvent event = uacEventCaptor.getValue();

    assertHeader(event, transactionId, type, Source.CONTACT_CENTRE_API, Channel.CC);
    assertEquals(payload, event.getPayload().getUac());
  }

  @Test
  public void shouldSendUacCreated() {
    assertSendUac(EventType.UAC_CREATED);
  }

  @Test
  public void shouldSendUacUpdated() {
    assertSendUac(EventType.UAC_UPDATED);
  }

  @Test(expected = IllegalArgumentException.class)
  public void shouldRejectSendForMismatchingPayload() {
    Feedback feedbackResponse = loadJson(Feedback[].class);

    eventPublisher.sendEvent(
        EventType.SAMPLE_UNIT_VALIDATED, Source.RESPONDENT_HOME, Channel.RH, feedbackResponse);
  }

  // -- replay send backup event tests ...

  @Test
  public void shouldSendBackupFulfilmentEvent() throws Exception {
    FulfilmentRequestedEvent ev = aFulfilmentRequestedEvent();
    sendBackupEvent(ev);

    RoutingKey routingKey = RoutingKey.forType(EventType.FULFILMENT_REQUESTED);
    verify(sender).sendEvent(eq(routingKey), fulfilmentRequestedEventCaptor.capture());
    verifyEventSent(ev, fulfilmentRequestedEventCaptor.getValue());
  }

  @Test
  public void shouldSendBackupRepondentAuthenticatedEvent() throws Exception {
    RespondentAuthenticatedEvent ev = aRespondentAuthenticatedEvent();
    sendBackupEvent(ev);

    RoutingKey routingKey = RoutingKey.forType(EventType.RESPONDENT_AUTHENTICATED);
    verify(sender).sendEvent(eq(routingKey), respondentAuthenticatedEventCaptor.capture());
    verifyEventSent(ev, respondentAuthenticatedEventCaptor.getValue());
  }

  @Test
  public void shouldSendBackupRefusalReceivedEvent() throws Exception {
    RespondentRefusalEvent ev = aRefusalEvent();
    sendBackupEvent(ev);

    RoutingKey routingKey = RoutingKey.forType(EventType.REFUSAL_RECEIVED);
    verify(sender).sendEvent(eq(routingKey), respondentRefusalEventCaptor.capture());
    verifyEventSent(ev, respondentRefusalEventCaptor.getValue());
  }

  @Test
  public void shouldSendBackupUacCreatedEvent() throws Exception {
    UACEvent ev = aUacEvent();
    ev.getEvent().setType(EventType.UAC_CREATED);
    sendBackupEvent(ev);

    RoutingKey routingKey = RoutingKey.forType(EventType.UAC_CREATED);
    verify(sender).sendEvent(eq(routingKey), uacEventCaptor.capture());
    verifyEventSent(ev, uacEventCaptor.getValue());
  }

  @Test
  public void shouldSendBackupUacUpdatedEvent() throws Exception {
    UACEvent ev = aUacEvent();
    ev.getEvent().setType(EventType.UAC_UPDATED);
    sendBackupEvent(ev);

    RoutingKey routingKey = RoutingKey.forType(EventType.UAC_UPDATED);
    verify(sender).sendEvent(eq(routingKey), uacEventCaptor.capture());
    verifyEventSent(ev, uacEventCaptor.getValue());
  }

  @Test
  public void shouldSendBackupSurveyLaunchedEvent() throws Exception {
    SurveyLaunchedEvent ev = aSurveyLaunchedEvent();
    sendBackupEvent(ev);

    RoutingKey routingKey = RoutingKey.forType(EventType.SURVEY_LAUNCHED);
    verify(sender).sendEvent(eq(routingKey), surveyLaunchedEventCaptor.capture());
    verifyEventSent(ev, surveyLaunchedEventCaptor.getValue());
  }

  @Test
  public void shouldSendBackupAddressModifiedEvent() throws Exception {
    AddressModifiedEvent ev = anAddressModifedEvent();
    sendBackupEvent(ev);

    RoutingKey routingKey = RoutingKey.forType(EventType.ADDRESS_MODIFIED);
    verify(sender).sendEvent(eq(routingKey), addressModifiedEventCaptor.capture());
    verifyEventSent(ev, addressModifiedEventCaptor.getValue());
  }

  @Test
  public void shouldSendBackupAddressNotValidEvent() throws Exception {
    AddressNotValidEvent ev = anAddressNotValidEvent();
    sendBackupEvent(ev);

    RoutingKey routingKey = RoutingKey.forType(EventType.ADDRESS_NOT_VALID);
    verify(sender).sendEvent(eq(routingKey), addressNotValidEventCaptor.capture());
    verifyEventSent(ev, addressNotValidEventCaptor.getValue());
  }

  @Test
  public void shouldSendBackupAddressTypeChangedEvent() throws Exception {
    AddressTypeChangedEvent ev = anAddressTypeChangedEvent();
    sendBackupEvent(ev);

    RoutingKey routingKey = RoutingKey.forType(EventType.ADDRESS_TYPE_CHANGED);
    verify(sender).sendEvent(eq(routingKey), addressTypeChangedEventCaptor.capture());
    verifyEventSent(ev, addressTypeChangedEventCaptor.getValue());
  }

  @Test
  public void shouldSendBackupCaseCreatedEvent() throws Exception {
    CaseEvent ev = aCaseEvent();
    ev.getEvent().setType(EventType.CASE_CREATED);
    sendBackupEvent(ev);

    RoutingKey routingKey = RoutingKey.forType(EventType.CASE_CREATED);
    verify(sender).sendEvent(eq(routingKey), caseEventCaptor.capture());
    verifyEventSent(ev, caseEventCaptor.getValue());
  }

  @Test
  public void shouldSendBackupCaseUpdatedEvent() throws Exception {
    CaseEvent ev = aCaseEvent();
    ev.getEvent().setType(EventType.CASE_UPDATED);
    sendBackupEvent(ev);

    RoutingKey routingKey = RoutingKey.forType(EventType.CASE_UPDATED);
    verify(sender).sendEvent(eq(routingKey), caseEventCaptor.capture());
    verifyEventSent(ev, caseEventCaptor.getValue());
  }

  @Test
  public void shouldSendBackupFeedbackEvent() throws Exception {
    FeedbackEvent ev = aFeedbackEvent();
    sendBackupEvent(ev);

    RoutingKey routingKey = RoutingKey.forType(EventType.FEEDBACK);
    verify(sender).sendEvent(eq(routingKey), feedbackEventCaptor.capture());
    verifyEventSent(ev, feedbackEventCaptor.getValue());
  }

  @Test
  public void shouldSendBackupNewAddressReportedEvent() throws Exception {
    NewAddressReportedEvent ev = aNewAddressReportedEvent();
    sendBackupEvent(ev);

    RoutingKey routingKey = RoutingKey.forType(EventType.NEW_ADDRESS_REPORTED);
    verify(sender).sendEvent(eq(routingKey), newAddressReportedEventCaptor.capture());
    verifyEventSent(ev, newAddressReportedEventCaptor.getValue());
  }

  @Test
  public void shouldSendBackupQuestionnaireLinkedEvent() throws Exception {
    QuestionnaireLinkedEvent ev = aQuestionnaireLinkedEvent();
    sendBackupEvent(ev);

    RoutingKey routingKey = RoutingKey.forType(EventType.QUESTIONNAIRE_LINKED);
    verify(sender).sendEvent(eq(routingKey), questionnaireLinkedEventCaptor.capture());
    verifyEventSent(ev, questionnaireLinkedEventCaptor.getValue());
  }

  @Test(expected = UnsupportedOperationException.class)
  public void shouldRejectEventWithoutPayload() throws Exception {
    QuestionnaireLinkedEvent ev = aQuestionnaireLinkedEvent();
    ev.getEvent().setType(EventType.SAMPLE_UNIT_VALIDATED);
    sendBackupEvent(ev);
  }

  @Test(expected = EventPublishException.class)
  public void shouldRejectMalformedEventBackupJson() throws Exception {
    QuestionnaireLinkedEvent ev = aQuestionnaireLinkedEvent();
    EventBackupData data = createEvent(ev);
    data.setEvent("xx" + data.getEvent()); // create broken Json
    eventPublisher.sendEvent(data);
  }

  // --- helpers

  private void sendBackupEvent(GenericEvent ev) throws Exception {
    EventBackupData data = createEvent(ev);
    String txId = eventPublisher.sendEvent(data);
    assertNotNull(txId);
  }

  private String serialise(Object obj) {
    try {
      return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(obj);
    } catch (JsonProcessingException e) {
      throw new RuntimeException("Failed to serialise event to JSON", e);
    }
  }

  private EventBackupData createEvent(GenericEvent event) {
    long failureTimeMillis = 123L;
    EventBackupData data = new EventBackupData();
    data.setId(event.getEvent().getTransactionId());
    data.setEventType(event.getEvent().getType());
    data.setMessageFailureDateTimeInMillis(failureTimeMillis);
    data.setMessageSentDateTimeInMillis(null);
    data.setEvent(serialise(event));
    return data;
  }

  FulfilmentRequestedEvent aFulfilmentRequestedEvent() {
    return FixtureHelper.loadPackageFixtures(FulfilmentRequestedEvent[].class).get(0);
  }

  RespondentRefusalEvent aRefusalEvent() {
    return FixtureHelper.loadPackageFixtures(RespondentRefusalEvent[].class).get(0);
  }

  RespondentAuthenticatedEvent aRespondentAuthenticatedEvent() {
    return FixtureHelper.loadPackageFixtures(RespondentAuthenticatedEvent[].class).get(0);
  }

  UACEvent aUacEvent() {
    return FixtureHelper.loadPackageFixtures(UACEvent[].class).get(0);
  }

  SurveyLaunchedEvent aSurveyLaunchedEvent() {
    return FixtureHelper.loadPackageFixtures(SurveyLaunchedEvent[].class).get(0);
  }

  AddressModifiedEvent anAddressModifedEvent() {
    return FixtureHelper.loadPackageFixtures(AddressModifiedEvent[].class).get(0);
  }

  AddressNotValidEvent anAddressNotValidEvent() {
    return FixtureHelper.loadPackageFixtures(AddressNotValidEvent[].class).get(0);
  }

  AddressTypeChangedEvent anAddressTypeChangedEvent() {
    return FixtureHelper.loadPackageFixtures(AddressTypeChangedEvent[].class).get(0);
  }

  CaseEvent aCaseEvent() {
    return FixtureHelper.loadPackageFixtures(CaseEvent[].class).get(0);
  }

  FeedbackEvent aFeedbackEvent() {
    return FixtureHelper.loadPackageFixtures(FeedbackEvent[].class).get(0);
  }

  NewAddressReportedEvent aNewAddressReportedEvent() {
    return FixtureHelper.loadPackageFixtures(NewAddressReportedEvent[].class).get(0);
  }

  QuestionnaireLinkedEvent aQuestionnaireLinkedEvent() {
    return FixtureHelper.loadPackageFixtures(QuestionnaireLinkedEvent[].class).get(0);
  }

  private <T> T loadJson(Class<T[]> clazz) {
    return FixtureHelper.loadPackageFixtures(clazz).get(0);
  }

  private void verifyEventSent(GenericEvent orig, GenericEvent sent) {
    Header origHeader = orig.getEvent();
    Header sentHeader = sent.getEvent();
    Date sentDate = sentHeader.getDateTime();
    assertTrue(
        sentDate.after(this.startOfTestDateTime) || sentDate.equals(this.startOfTestDateTime));
    assertTrue(sentDate.after(origHeader.getDateTime()));
    assertFalse(sentHeader.getTransactionId().equals(origHeader.getTransactionId()));

    // check all other fields are the same
    origHeader.setDateTime(sentDate);
    origHeader.setTransactionId(sentHeader.getTransactionId());
    assertEquals(orig, sent);
  }
}
