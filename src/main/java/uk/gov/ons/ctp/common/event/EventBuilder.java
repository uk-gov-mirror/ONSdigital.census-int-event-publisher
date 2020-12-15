package uk.gov.ons.ctp.common.event;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Date;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import uk.gov.ons.ctp.common.event.EventPublisher.Channel;
import uk.gov.ons.ctp.common.event.EventPublisher.EventType;
import uk.gov.ons.ctp.common.event.EventPublisher.Source;
import uk.gov.ons.ctp.common.event.model.AddressModification;
import uk.gov.ons.ctp.common.event.model.AddressModifiedEvent;
import uk.gov.ons.ctp.common.event.model.AddressModifiedPayload;
import uk.gov.ons.ctp.common.event.model.AddressNotValid;
import uk.gov.ons.ctp.common.event.model.AddressNotValidEvent;
import uk.gov.ons.ctp.common.event.model.AddressNotValidPayload;
import uk.gov.ons.ctp.common.event.model.AddressTypeChanged;
import uk.gov.ons.ctp.common.event.model.AddressTypeChangedEvent;
import uk.gov.ons.ctp.common.event.model.AddressTypeChangedPayload;
import uk.gov.ons.ctp.common.event.model.CaseEvent;
import uk.gov.ons.ctp.common.event.model.CasePayload;
import uk.gov.ons.ctp.common.event.model.CollectionCase;
import uk.gov.ons.ctp.common.event.model.EventPayload;
import uk.gov.ons.ctp.common.event.model.Feedback;
import uk.gov.ons.ctp.common.event.model.FeedbackEvent;
import uk.gov.ons.ctp.common.event.model.FeedbackPayload;
import uk.gov.ons.ctp.common.event.model.FulfilmentPayload;
import uk.gov.ons.ctp.common.event.model.FulfilmentRequest;
import uk.gov.ons.ctp.common.event.model.FulfilmentRequestedEvent;
import uk.gov.ons.ctp.common.event.model.GenericEvent;
import uk.gov.ons.ctp.common.event.model.Header;
import uk.gov.ons.ctp.common.event.model.NewAddress;
import uk.gov.ons.ctp.common.event.model.NewAddressPayload;
import uk.gov.ons.ctp.common.event.model.NewAddressReportedEvent;
import uk.gov.ons.ctp.common.event.model.QuestionnaireLinkedDetails;
import uk.gov.ons.ctp.common.event.model.QuestionnaireLinkedEvent;
import uk.gov.ons.ctp.common.event.model.QuestionnaireLinkedPayload;
import uk.gov.ons.ctp.common.event.model.RespondentAuthenticatedEvent;
import uk.gov.ons.ctp.common.event.model.RespondentAuthenticatedResponse;
import uk.gov.ons.ctp.common.event.model.RespondentRefusalDetails;
import uk.gov.ons.ctp.common.event.model.RespondentRefusalEvent;
import uk.gov.ons.ctp.common.event.model.RespondentRefusalPayload;
import uk.gov.ons.ctp.common.event.model.SurveyLaunchedEvent;
import uk.gov.ons.ctp.common.event.model.SurveyLaunchedResponse;
import uk.gov.ons.ctp.common.event.model.UAC;
import uk.gov.ons.ctp.common.event.model.UACEvent;
import uk.gov.ons.ctp.common.event.model.UACPayload;
import uk.gov.ons.ctp.common.jackson.CustomObjectMapper;

/**
 * Build objects ready for the publisher to send events. The subclasses of the event builder handle
 * the inconsistent structure of each event object.
 */
public abstract class EventBuilder {
  public static final EventBuilder NONE = new NullEventBuilder();
  public static final EventBuilder FULFILMENT_REQUESTED = new FulfilmentRequestedBuilder();
  public static final EventBuilder SURVEY_LAUNCHED = new SurveyLaunchedBuilder();
  public static final EventBuilder RESPONDENT_AUTHENTICATED = new RespondentAuthenticatedBuilder();
  public static final EventBuilder CASE_CREATED = new CaseCreatedBuilder();
  public static final EventBuilder CASE_UPDATED = new CaseUpdatedBuilder();
  public static final EventBuilder REFUSAL_RECEIVED = new RefusalReceivedBuilder();
  public static final EventBuilder UAC_CREATED = new UacCreatedBuilder();
  public static final EventBuilder UAC_UPDATED = new UacUpdatedBuilder();
  public static final EventBuilder ADDRESS_MODIFIED = new AddressModifiedBuilder();
  public static final EventBuilder ADDRESS_NOT_VALID = new AddressNotValidBuilder();
  public static final EventBuilder ADDRESS_TYPE_CHANGED = new AddressTypeChangedBuilder();
  public static final EventBuilder NEW_ADDRESS_REPORTED = new NewAddressReportedBuilder();
  public static final EventBuilder FEEDBACK = new FeedbackBuilder();
  public static final EventBuilder QUESTIONNAIRE_LINKED = new QuestionnaireLinkedBuilder();

  ObjectMapper objectMapper = new CustomObjectMapper();

  /**
   * Create event ready for send.
   *
   * @param sendInfo object containing payload , source and channel.
   * @return event
   */
  abstract GenericEvent create(SendInfo sendInfo);

  /**
   * Create information required to send the event based on the serialised backup event supplied.
   *
   * @param json string of serialised event backup JSON.
   * @return object containing deserialised payload , source and channel.
   */
  abstract SendInfo create(String json);

  <T extends GenericEvent> T deserialiseEventJson(String json, Class<T> clazz) {
    try {
      return objectMapper.readValue(json, clazz);
    } catch (JsonProcessingException e) {
      throw new EventPublishException(e);
    }
  }

  static Header buildHeader(EventType type, Source source, Channel channel) {
    return Header.builder()
        .type(type)
        .source(source)
        .channel(channel)
        .dateTime(new Date())
        .transactionId(UUID.randomUUID().toString())
        .build();
  }

  @Data
  @AllArgsConstructor
  @Builder
  public static class SendInfo {
    private EventPayload payload;
    private Source source;
    private Channel channel;
  }

  SendInfo build(GenericEvent genericEvent, EventPayload payload) {
    SendInfo info =
        SendInfo.builder()
            .payload(payload)
            .source(genericEvent.getEvent().getSource())
            .channel(genericEvent.getEvent().getChannel())
            .build();
    return info;
  }

  public static class NullEventBuilder extends EventBuilder {
    @Override
    GenericEvent create(SendInfo sendInfo) {
      return null;
    }

    @Override
    SendInfo create(String json) {
      return null;
    }
  }

  public static class FulfilmentRequestedBuilder extends EventBuilder {
    @Override
    GenericEvent create(SendInfo sendInfo) {
      FulfilmentRequestedEvent fulfilmentRequestedEvent = new FulfilmentRequestedEvent();
      fulfilmentRequestedEvent.setEvent(
          buildHeader(EventType.FULFILMENT_REQUESTED, sendInfo.getSource(), sendInfo.getChannel()));
      FulfilmentPayload fulfilmentPayload =
          new FulfilmentPayload((FulfilmentRequest) sendInfo.getPayload());
      fulfilmentRequestedEvent.setPayload(fulfilmentPayload);
      return fulfilmentRequestedEvent;
    }

    @Override
    SendInfo create(String json) {
      GenericEvent genericEvent = deserialiseEventJson(json, FulfilmentRequestedEvent.class);
      EventPayload payload =
          ((FulfilmentRequestedEvent) genericEvent).getPayload().getFulfilmentRequest();
      return build(genericEvent, payload);
    }
  }

  public static class SurveyLaunchedBuilder extends EventBuilder {
    @Override
    GenericEvent create(SendInfo sendInfo) {
      SurveyLaunchedEvent surveyLaunchedEvent = new SurveyLaunchedEvent();
      surveyLaunchedEvent.setEvent(
          buildHeader(EventType.SURVEY_LAUNCHED, sendInfo.getSource(), sendInfo.getChannel()));
      surveyLaunchedEvent.getPayload().setResponse((SurveyLaunchedResponse) sendInfo.getPayload());
      return surveyLaunchedEvent;
    }

    @Override
    SendInfo create(String json) {
      GenericEvent genericEvent = deserialiseEventJson(json, SurveyLaunchedEvent.class);
      EventPayload payload = ((SurveyLaunchedEvent) genericEvent).getPayload().getResponse();
      return build(genericEvent, payload);
    }
  }

  public static class RespondentAuthenticatedBuilder extends EventBuilder {
    @Override
    GenericEvent create(SendInfo sendInfo) {
      RespondentAuthenticatedEvent respondentAuthenticatedEvent =
          new RespondentAuthenticatedEvent();
      respondentAuthenticatedEvent.setEvent(
          buildHeader(
              EventType.RESPONDENT_AUTHENTICATED, sendInfo.getSource(), sendInfo.getChannel()));
      respondentAuthenticatedEvent
          .getPayload()
          .setResponse((RespondentAuthenticatedResponse) sendInfo.getPayload());
      return respondentAuthenticatedEvent;
    }

    @Override
    SendInfo create(String json) {
      GenericEvent genericEvent = deserialiseEventJson(json, RespondentAuthenticatedEvent.class);
      EventPayload payload =
          ((RespondentAuthenticatedEvent) genericEvent).getPayload().getResponse();
      return build(genericEvent, payload);
    }
  }

  public static class CaseCreatedBuilder extends EventBuilder {
    @Override
    GenericEvent create(SendInfo sendInfo) {
      CaseEvent caseEvent = new CaseEvent();
      caseEvent.setEvent(
          buildHeader(EventType.CASE_CREATED, sendInfo.getSource(), sendInfo.getChannel()));
      CasePayload casePayload = new CasePayload((CollectionCase) sendInfo.getPayload());
      caseEvent.setPayload(casePayload);
      return caseEvent;
    }

    @Override
    SendInfo create(String json) {
      GenericEvent genericEvent = deserialiseEventJson(json, CaseEvent.class);
      EventPayload payload = ((CaseEvent) genericEvent).getPayload().getCollectionCase();
      return build(genericEvent, payload);
    }
  }

  public static class CaseUpdatedBuilder extends EventBuilder {
    @Override
    GenericEvent create(SendInfo sendInfo) {
      CaseEvent caseEvent = new CaseEvent();
      caseEvent.setEvent(
          buildHeader(EventType.CASE_UPDATED, sendInfo.getSource(), sendInfo.getChannel()));
      CasePayload casePayload = new CasePayload((CollectionCase) sendInfo.getPayload());
      caseEvent.setPayload(casePayload);
      return caseEvent;
    }

    @Override
    SendInfo create(String json) {
      GenericEvent genericEvent = deserialiseEventJson(json, CaseEvent.class);
      EventPayload payload = ((CaseEvent) genericEvent).getPayload().getCollectionCase();
      return build(genericEvent, payload);
    }
  }

  public static class RefusalReceivedBuilder extends EventBuilder {
    @Override
    GenericEvent create(SendInfo sendInfo) {
      RespondentRefusalEvent respondentRefusalEvent = new RespondentRefusalEvent();
      respondentRefusalEvent.setEvent(
          buildHeader(EventType.REFUSAL_RECEIVED, sendInfo.getSource(), sendInfo.getChannel()));
      RespondentRefusalPayload respondentRefusalPayload =
          new RespondentRefusalPayload((RespondentRefusalDetails) sendInfo.getPayload());
      respondentRefusalEvent.setPayload(respondentRefusalPayload);
      return respondentRefusalEvent;
    }

    @Override
    SendInfo create(String json) {
      GenericEvent genericEvent = deserialiseEventJson(json, RespondentRefusalEvent.class);
      EventPayload payload = ((RespondentRefusalEvent) genericEvent).getPayload().getRefusal();
      return build(genericEvent, payload);
    }
  }

  public static class UacCreatedBuilder extends EventBuilder {
    @Override
    GenericEvent create(SendInfo sendInfo) {
      UACEvent uacEvent = new UACEvent();
      uacEvent.setEvent(
          buildHeader(EventType.UAC_CREATED, sendInfo.getSource(), sendInfo.getChannel()));
      UACPayload uacPayload = new UACPayload((UAC) sendInfo.getPayload());
      uacEvent.setPayload(uacPayload);
      return uacEvent;
    }

    @Override
    SendInfo create(String json) {
      GenericEvent genericEvent = deserialiseEventJson(json, UACEvent.class);
      EventPayload payload = ((UACEvent) genericEvent).getPayload().getUac();
      return build(genericEvent, payload);
    }
  }

  public static class UacUpdatedBuilder extends EventBuilder {
    @Override
    GenericEvent create(SendInfo sendInfo) {
      UACEvent uacEvent = new UACEvent();
      uacEvent.setEvent(
          buildHeader(EventType.UAC_UPDATED, sendInfo.getSource(), sendInfo.getChannel()));
      UACPayload uacPayload = new UACPayload((UAC) sendInfo.getPayload());
      uacEvent.setPayload(uacPayload);
      return uacEvent;
    }

    @Override
    SendInfo create(String json) {
      GenericEvent genericEvent = deserialiseEventJson(json, UACEvent.class);
      EventPayload payload = ((UACEvent) genericEvent).getPayload().getUac();
      return build(genericEvent, payload);
    }
  }

  public static class AddressModifiedBuilder extends EventBuilder {
    @Override
    GenericEvent create(SendInfo sendInfo) {
      AddressModifiedEvent addressModifiedEvent = new AddressModifiedEvent();
      addressModifiedEvent.setEvent(
          buildHeader(EventType.ADDRESS_MODIFIED, sendInfo.getSource(), sendInfo.getChannel()));
      AddressModifiedPayload addressModifiedPayload =
          new AddressModifiedPayload((AddressModification) sendInfo.getPayload());
      addressModifiedEvent.setPayload(addressModifiedPayload);
      return addressModifiedEvent;
    }

    @Override
    SendInfo create(String json) {
      GenericEvent genericEvent = deserialiseEventJson(json, AddressModifiedEvent.class);
      EventPayload payload =
          ((AddressModifiedEvent) genericEvent).getPayload().getAddressModification();
      return build(genericEvent, payload);
    }
  }

  public static class AddressNotValidBuilder extends EventBuilder {
    @Override
    GenericEvent create(SendInfo sendInfo) {
      AddressNotValidEvent addrNotValidEvent = new AddressNotValidEvent();
      addrNotValidEvent.setEvent(
          buildHeader(EventType.ADDRESS_NOT_VALID, sendInfo.getSource(), sendInfo.getChannel()));
      AddressNotValidPayload addrNotValidPayload =
          new AddressNotValidPayload((AddressNotValid) sendInfo.getPayload());
      addrNotValidEvent.setPayload(addrNotValidPayload);
      return addrNotValidEvent;
    }

    @Override
    SendInfo create(String json) {
      GenericEvent genericEvent = deserialiseEventJson(json, AddressNotValidEvent.class);
      EventPayload payload = ((AddressNotValidEvent) genericEvent).getPayload().getInvalidAddress();
      return build(genericEvent, payload);
    }
  }

  public static class AddressTypeChangedBuilder extends EventBuilder {
    @Override
    GenericEvent create(SendInfo sendInfo) {
      AddressTypeChangedEvent addressTypeChangedEvent = new AddressTypeChangedEvent();
      addressTypeChangedEvent.setEvent(
          buildHeader(EventType.ADDRESS_TYPE_CHANGED, sendInfo.getSource(), sendInfo.getChannel()));
      AddressTypeChangedPayload addressTypeChangedPayload =
          new AddressTypeChangedPayload((AddressTypeChanged) sendInfo.getPayload());
      addressTypeChangedEvent.setPayload(addressTypeChangedPayload);
      return addressTypeChangedEvent;
    }

    @Override
    SendInfo create(String json) {
      GenericEvent genericEvent = deserialiseEventJson(json, AddressTypeChangedEvent.class);
      EventPayload payload =
          ((AddressTypeChangedEvent) genericEvent).getPayload().getAddressTypeChange();
      return build(genericEvent, payload);
    }
  }

  public static class NewAddressReportedBuilder extends EventBuilder {
    @Override
    GenericEvent create(SendInfo sendInfo) {
      NewAddressReportedEvent newAddressReportedEvent = new NewAddressReportedEvent();
      newAddressReportedEvent.setEvent(
          buildHeader(EventType.NEW_ADDRESS_REPORTED, sendInfo.getSource(), sendInfo.getChannel()));
      NewAddressPayload newAddressPayload =
          new NewAddressPayload((NewAddress) sendInfo.getPayload());
      newAddressReportedEvent.setPayload(newAddressPayload);
      return newAddressReportedEvent;
    }

    @Override
    SendInfo create(String json) {
      GenericEvent genericEvent = deserialiseEventJson(json, NewAddressReportedEvent.class);
      EventPayload payload = ((NewAddressReportedEvent) genericEvent).getPayload().getNewAddress();
      return build(genericEvent, payload);
    }
  }

  public static class FeedbackBuilder extends EventBuilder {
    @Override
    GenericEvent create(SendInfo sendInfo) {
      FeedbackEvent feedbackEvent = new FeedbackEvent();
      feedbackEvent.setEvent(
          buildHeader(EventType.FEEDBACK, sendInfo.getSource(), sendInfo.getChannel()));
      FeedbackPayload feedbackPayload = new FeedbackPayload((Feedback) sendInfo.getPayload());
      feedbackEvent.setPayload(feedbackPayload);
      return feedbackEvent;
    }

    @Override
    SendInfo create(String json) {
      GenericEvent genericEvent = deserialiseEventJson(json, FeedbackEvent.class);
      EventPayload payload = ((FeedbackEvent) genericEvent).getPayload().getFeedback();
      return build(genericEvent, payload);
    }
  }

  public static class QuestionnaireLinkedBuilder extends EventBuilder {
    @Override
    GenericEvent create(SendInfo sendInfo) {
      QuestionnaireLinkedEvent questionnaireLinkedEvent = new QuestionnaireLinkedEvent();
      questionnaireLinkedEvent.setEvent(
          buildHeader(EventType.QUESTIONNAIRE_LINKED, sendInfo.getSource(), sendInfo.getChannel()));
      QuestionnaireLinkedPayload questionnaireLinkedPayload =
          new QuestionnaireLinkedPayload((QuestionnaireLinkedDetails) sendInfo.getPayload());
      questionnaireLinkedEvent.setPayload(questionnaireLinkedPayload);
      return questionnaireLinkedEvent;
    }

    @Override
    SendInfo create(String json) {
      GenericEvent genericEvent = deserialiseEventJson(json, QuestionnaireLinkedEvent.class);
      EventPayload payload = ((QuestionnaireLinkedEvent) genericEvent).getPayload().getUac();
      return build(genericEvent, payload);
    }
  }
}
