package uk.gov.ons.ctp.common.event;

import com.godaddy.logging.Logger;
import com.godaddy.logging.LoggerFactory;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import lombok.Getter;
import uk.gov.ons.ctp.common.event.model.AddressModification;
import uk.gov.ons.ctp.common.event.model.AddressModifiedEvent;
import uk.gov.ons.ctp.common.event.model.AddressModifiedPayload;
import uk.gov.ons.ctp.common.event.model.CaseEvent;
import uk.gov.ons.ctp.common.event.model.CasePayload;
import uk.gov.ons.ctp.common.event.model.CollectionCase;
import uk.gov.ons.ctp.common.event.model.EventPayload;
import uk.gov.ons.ctp.common.event.model.FulfilmentPayload;
import uk.gov.ons.ctp.common.event.model.FulfilmentRequest;
import uk.gov.ons.ctp.common.event.model.FulfilmentRequestedEvent;
import uk.gov.ons.ctp.common.event.model.GenericEvent;
import uk.gov.ons.ctp.common.event.model.Header;
import uk.gov.ons.ctp.common.event.model.RespondentAuthenticatedEvent;
import uk.gov.ons.ctp.common.event.model.RespondentAuthenticatedResponse;
import uk.gov.ons.ctp.common.event.model.RespondentRefusalDetails;
import uk.gov.ons.ctp.common.event.model.RespondentRefusalEvent;
import uk.gov.ons.ctp.common.event.model.RespondentRefusalPayload;
import uk.gov.ons.ctp.common.event.model.SurveyLaunchedEvent;
import uk.gov.ons.ctp.common.event.model.SurveyLaunchedResponse;

/** Service responsible for the publication of events. */
public class EventPublisher {

  private static final Logger log = LoggerFactory.getLogger(EventPublisher.class);

  private EventSender sender;

  @Getter
  public enum RoutingKey {
    //// @formatter:off
    EVENT_FULFILMENT_REQUEST("event.fulfilment.request", EventType.FULFILMENT_REQUESTED),
    EVENT_FULFILMENT_CONFIRMATION("event.fulfilment.confirmation", EventType.FULFILMENT_CONFIRMED),
    EVENT_FULFILMENT_UNDELIVERED(
        "event.fulfilment.undelivered", EventType.UNDELIVERED_MAIL_REPORTED),
    EVENT_RESPONSE_AUTHENTICATION(
        "event.response.authentication",
        EventType.RESPONDENT_AUTHENTICATED,
        EventType.SURVEY_LAUNCHED),
    EVENT_RESPONSE_RECEIPT("event.response.receipt", EventType.RESPONSE_RECEIVED),
    EVENT_RESPONDENT_REFUSAL("event.respondent.refusal", EventType.REFUSAL_RECEIVED),
    EVENT_UAC_UPDATE("event.uac.update", EventType.UAC_UPDATED),
    EVENT_QUESTIONNAIRE_UPDATE("event.questionnaire.update", EventType.QUESTIONNAIRE_LINKED),
    EVENT_CASE_UPDATE("event.case.update.event", EventType.CASE_UPDATED, EventType.CASE_CREATED),
    EVENT_CASE_ADDRESS_UPDATE(
        "event.case.address.update",
        EventType.NEW_ADDRESS_REPORTED,
        EventType.ADDRESS_MODIFIED,
        EventType.ADDRESS_NOT_VALID,
        EventType.ADDRESS_TYPE_CHANGED),
    EVENT_CASE_APPOINTMENT("event.case.appointment", EventType.APPOINTMENT_REQUESTED),
    EVENT_FIELD_CASE_UPDATE("event.fieldcase.update", EventType.FIELD_CASE_UPDATED),
    EVENT_SAMPLE_UNIT_UPDATE("event.sampleunit.update", EventType.SAMPLE_UNIT_VALIDATED),
    EVENT_CCS_PROPERTY_LISTING("event.ccs.propertylisting", EventType.CCS_PROPERTY_LISTED);

    private String key;
    private List<EventType> eventTypes;

    private RoutingKey(String key, EventType... types) {
      this.key = key;
      this.eventTypes = Arrays.asList(types);
    }

    public static RoutingKey forType(EventType eventType) {
      for (RoutingKey routingKey : values()) {
        if (routingKey.eventTypes.contains(eventType)) {
          return routingKey;
        }
      }
      return null;
    }
  }

  @Getter
  public enum EventType {
    ADDRESS_MODIFIED(AddressModification.class),
    ADDRESS_NOT_VALID,
    ADDRESS_TYPE_CHANGED,
    APPOINTMENT_REQUESTED,
    CASE_CREATED(CollectionCase.class),
    CASE_UPDATED(CollectionCase.class),
    CCS_PROPERTY_LISTED,
    FIELD_CASE_UPDATED,
    FULFILMENT_CONFIRMED,
    FULFILMENT_REQUESTED(FulfilmentRequest.class),
    NEW_ADDRESS_REPORTED,
    QUESTIONNAIRE_LINKED,
    REFUSAL_RECEIVED(RespondentRefusalDetails.class),
    RESPONDENT_AUTHENTICATED(RespondentAuthenticatedResponse.class),
    RESPONSE_RECEIVED,
    SAMPLE_UNIT_VALIDATED,
    SURVEY_LAUNCHED(SurveyLaunchedResponse.class),
    UAC_UPDATED,
    UNDELIVERED_MAIL_REPORTED;

    private Class<? extends EventPayload> payloadType;

    private EventType() {}

    private EventType(Class<? extends EventPayload> payloadType) {
      this.payloadType = payloadType;
    }
  }

  @Getter
  public enum Source {
    ACTION_EXPORTER,
    ADDRESS_RESOLUTION,
    CASE_SERVICE,
    CONTACT_CENTRE_API,
    FIELDWORK_GATEWAY,
    NOTIFY_GATEWAY,
    RECEIPT_SERVICE,
    RESPONDENT_HOME,
    SAMPLE_LOADER;
  }

  @Getter
  public enum Channel {
    AD,
    AR,
    CC,
    EQ,
    FIELD,
    PPO,
    PQRS,
    QM,
    RH,
    RM,
    RO;
  }
  // @formatter:on

  /**
   * Constructor taking publishing helper class
   *
   * @param eventSender the impl of EventSender that will be used to ... send the event
   */
  public EventPublisher(EventSender eventSender) {
    this.sender = eventSender;
  }

  /**
   * Method to publish an event
   *
   * @param eventType the event type
   * @param source the source
   * @param channel the channel
   * @param payload message payload for event
   * @return String UUID transaction Id for event
   */
  public String sendEvent(
      EventType eventType, Source source, Channel channel, EventPayload payload) {

    if (!payload.getClass().equals(eventType.getPayloadType())) {
      throw new IllegalArgumentException(
          "Payload type " + payload.getClass() + " incompatible for event type " + eventType);
    }

    RoutingKey routingKey = RoutingKey.forType(eventType);
    if (routingKey == null) {
      throw new UnsupportedOperationException(
          "Routing key for eventType " + eventType + " not configured");
    }

    GenericEvent genericEvent = null;
    switch (eventType) {
      case FULFILMENT_REQUESTED:
        FulfilmentRequestedEvent fulfilmentRequestedEvent = new FulfilmentRequestedEvent();
        fulfilmentRequestedEvent.setEvent(buildHeader(eventType, source, channel));
        FulfilmentPayload fulfilmentPayload = new FulfilmentPayload((FulfilmentRequest) payload);
        fulfilmentRequestedEvent.setPayload(fulfilmentPayload);
        genericEvent = fulfilmentRequestedEvent;
        break;

      case SURVEY_LAUNCHED:
        SurveyLaunchedEvent surveyLaunchedEvent = new SurveyLaunchedEvent();
        surveyLaunchedEvent.setEvent(buildHeader(eventType, source, channel));
        surveyLaunchedEvent.getPayload().setResponse((SurveyLaunchedResponse) payload);
        genericEvent = surveyLaunchedEvent;
        break;

      case RESPONDENT_AUTHENTICATED:
        RespondentAuthenticatedEvent respondentAuthenticatedEvent =
            new RespondentAuthenticatedEvent();
        respondentAuthenticatedEvent.setEvent(buildHeader(eventType, source, channel));
        respondentAuthenticatedEvent
            .getPayload()
            .setResponse((RespondentAuthenticatedResponse) payload);
        genericEvent = respondentAuthenticatedEvent;
        break;

      case CASE_CREATED:
        CaseEvent caseEvent = new CaseEvent();
        caseEvent.setEvent(buildHeader(eventType, source, channel));
        CasePayload casePayload = new CasePayload((CollectionCase) payload);
        caseEvent.setPayload(casePayload);
        genericEvent = caseEvent;
        break;

      case REFUSAL_RECEIVED:
        RespondentRefusalEvent respondentRefusalEvent = new RespondentRefusalEvent();
        respondentRefusalEvent.setEvent(buildHeader(eventType, source, channel));
        RespondentRefusalPayload respondentRefusalPayload =
            new RespondentRefusalPayload((RespondentRefusalDetails) payload);
        respondentRefusalEvent.setPayload(respondentRefusalPayload);
        genericEvent = respondentRefusalEvent;
        break;

      case ADDRESS_MODIFIED:
        AddressModifiedEvent addressModifiedEvent = new AddressModifiedEvent();
        addressModifiedEvent.setEvent(buildHeader(eventType, source, channel));
        AddressModifiedPayload addressModifiedPayload =
            new AddressModifiedPayload((AddressModification) payload);
        addressModifiedEvent.setPayload(addressModifiedPayload);
        genericEvent = addressModifiedEvent;
        break;

      default:
        log.error(payload.getClass().getName() + " not supported");
        throw new UnsupportedOperationException(
            payload.getClass().getName() + " not supported yet");
    }
    try {
      sender.sendEvent(routingKey, genericEvent);
    } catch (Exception e) {
      // diff sender impls may send diff exceptions
      throw new EventPublishException(e);
    }
    return genericEvent.getEvent().getTransactionId();
  }

  private static Header buildHeader(EventType type, Source source, Channel channel) {
    return Header.builder()
        .type(type)
        .source(source)
        .channel(channel)
        .dateTime(new Date())
        .transactionId(UUID.randomUUID().toString())
        .build();
  }
}
