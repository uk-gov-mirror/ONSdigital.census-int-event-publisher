package uk.gov.ons.ctp.common.event;

import com.godaddy.logging.Logger;
import com.godaddy.logging.LoggerFactory;
import java.util.Arrays;
import java.util.List;
import lombok.Getter;
import org.springframework.cloud.client.circuitbreaker.CircuitBreaker;
import uk.gov.ons.ctp.common.event.EventBuilder.SendInfo;
import uk.gov.ons.ctp.common.event.model.AddressModification;
import uk.gov.ons.ctp.common.event.model.AddressNotValid;
import uk.gov.ons.ctp.common.event.model.AddressTypeChanged;
import uk.gov.ons.ctp.common.event.model.CollectionCase;
import uk.gov.ons.ctp.common.event.model.EventPayload;
import uk.gov.ons.ctp.common.event.model.Feedback;
import uk.gov.ons.ctp.common.event.model.FulfilmentRequest;
import uk.gov.ons.ctp.common.event.model.GenericEvent;
import uk.gov.ons.ctp.common.event.model.NewAddress;
import uk.gov.ons.ctp.common.event.model.QuestionnaireLinkedDetails;
import uk.gov.ons.ctp.common.event.model.RespondentAuthenticatedResponse;
import uk.gov.ons.ctp.common.event.model.RespondentRefusalDetails;
import uk.gov.ons.ctp.common.event.model.SurveyLaunchedResponse;
import uk.gov.ons.ctp.common.event.model.UAC;
import uk.gov.ons.ctp.common.event.persistence.EventBackupData;
import uk.gov.ons.ctp.common.event.persistence.EventPersistence;

/** Service responsible for the publication of events. */
public class EventPublisher {

  private static final Logger log = LoggerFactory.getLogger(EventPublisher.class);

  private EventSender sender;
  private CircuitBreaker circuitBreaker;

  private EventPersistence eventPersistence;

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
    EVENT_UAC_UPDATE("event.uac.update", EventType.UAC_UPDATED, EventType.UAC_CREATED),
    EVENT_QUESTIONNAIRE_UPDATE("event.questionnaire.update", EventType.QUESTIONNAIRE_LINKED),
    EVENT_CASE_UPDATE("event.case.update", EventType.CASE_UPDATED, EventType.CASE_CREATED),
    EVENT_CASE_ADDRESS_UPDATE(
        "event.case.address.update",
        EventType.NEW_ADDRESS_REPORTED,
        EventType.ADDRESS_MODIFIED,
        EventType.ADDRESS_NOT_VALID,
        EventType.ADDRESS_TYPE_CHANGED),
    EVENT_CASE_APPOINTMENT("event.case.appointment", EventType.APPOINTMENT_REQUESTED),
    EVENT_FIELD_CASE_UPDATE("event.fieldcase.update", EventType.FIELD_CASE_UPDATED),
    EVENT_SAMPLE_UNIT_UPDATE("event.sampleunit.update", EventType.SAMPLE_UNIT_VALIDATED),
    EVENT_CCS_PROPERTY_LISTING("event.ccs.propertylisting", EventType.CCS_PROPERTY_LISTED),
    FEEDBACK("event.website.feedback", EventType.FEEDBACK);

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
    ADDRESS_MODIFIED(AddressModification.class, EventBuilder.ADDRESS_MODIFIED),
    ADDRESS_NOT_VALID(AddressNotValid.class, EventBuilder.ADDRESS_NOT_VALID),
    ADDRESS_TYPE_CHANGED(AddressTypeChanged.class, EventBuilder.ADDRESS_TYPE_CHANGED),
    APPOINTMENT_REQUESTED,
    CASE_CREATED(CollectionCase.class, EventBuilder.CASE_CREATED),
    CASE_UPDATED(CollectionCase.class, EventBuilder.CASE_UPDATED),
    CCS_PROPERTY_LISTED,
    FIELD_CASE_UPDATED,
    FULFILMENT_CONFIRMED,
    FULFILMENT_REQUESTED(FulfilmentRequest.class, EventBuilder.FULFILMENT_REQUESTED),
    NEW_ADDRESS_REPORTED(NewAddress.class, EventBuilder.NEW_ADDRESS_REPORTED),
    QUESTIONNAIRE_LINKED(QuestionnaireLinkedDetails.class, EventBuilder.QUESTIONNAIRE_LINKED),
    REFUSAL_RECEIVED(RespondentRefusalDetails.class, EventBuilder.REFUSAL_RECEIVED),
    RESPONDENT_AUTHENTICATED(
        RespondentAuthenticatedResponse.class, EventBuilder.RESPONDENT_AUTHENTICATED),
    RESPONSE_RECEIVED,
    SAMPLE_UNIT_VALIDATED,
    SURVEY_LAUNCHED(SurveyLaunchedResponse.class, EventBuilder.SURVEY_LAUNCHED),
    UAC_CREATED(UAC.class, EventBuilder.UAC_CREATED),
    UAC_UPDATED(UAC.class, EventBuilder.UAC_UPDATED),
    UNDELIVERED_MAIL_REPORTED,
    FEEDBACK(Feedback.class, EventBuilder.FEEDBACK);

    private Class<? extends EventPayload> payloadType;
    private EventBuilder builder;

    private EventType() {
      this.builder = EventBuilder.NONE;
    }

    private EventType(Class<? extends EventPayload> payloadType, EventBuilder builder) {
      this.payloadType = payloadType;
      this.builder = builder;
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

  private EventPublisher(
      EventSender eventSender, EventPersistence eventPersistence, CircuitBreaker circuitBreaker) {
    this.sender = eventSender;
    this.eventPersistence = eventPersistence;
    this.circuitBreaker = circuitBreaker;
  }

  /**
   * Create method for creating an EventPublisher that will not attempt to persist events following
   * a Rabbit failure.
   *
   * @param eventSender the impl of EventSender that will be used to ... send the event.
   * @return an EventPubisher object.
   */
  public static EventPublisher createWithoutEventPersistence(EventSender eventSender) {
    return new EventPublisher(eventSender, null, null);
  }

  /**
   * Create method for creating an EventPublisher that will persist events following a Rabbit
   * failure. If Rabbit fails and the event is successfully persisted then all will appear well to
   * the caller, with the only indication of the failure being that an error is logged.
   *
   * @param eventSender the impl of EventSender that will be used to ... send the event.
   * @param eventPersistence is an EventPersistence implementation which does the actual event
   *     persistence.
   * @param circuitBreaker circuit breaker object, or null if not required.
   * @return an EventPubisher object.
   */
  public static EventPublisher createWithEventPersistence(
      EventSender eventSender, EventPersistence eventPersistence, CircuitBreaker circuitBreaker) {
    return new EventPublisher(eventSender, eventPersistence, circuitBreaker);
  }

  /**
   * Method to publish an event.
   *
   * <p>If no EventPersister has been set then a Rabbit failure results in an exception being
   * thrown.
   *
   * <p>If an EventPersister is set then in the event of a Rabbit failure it will attempt to save
   * the event into a persistent store. If event is persisted then this method returns as normal
   * with no exception. If event persistence fails then an error is logged and an exception is
   * thrown.
   *
   * @param eventType the event type
   * @param source the source
   * @param channel the channel
   * @param payload message payload for event
   * @return String UUID transaction Id for event
   */
  public String sendEvent(
      EventType eventType, Source source, Channel channel, EventPayload payload) {

    log.with(eventType).with(source).with(channel).with(payload).debug("Enter sendEvent()");

    String transactionId = doSendEvent(eventType, new SendInfo(payload, source, channel));

    log.with(eventType).with(source).with(channel).with(payload).debug("Exit sendEvent()");

    return transactionId;
  }

  /**
   * Send a backup event that would have previously been stored in cloud data storage.
   *
   * @param event backup event , typically recovered from firestore.
   * @return String UUID transaction Id for event
   */
  public String sendEvent(EventBackupData event) {
    EventType type = event.getEventType();
    SendInfo sendInfo = type.getBuilder().create(event.getEvent());
    if (sendInfo == null) {
      log.with("type", type).error("Unrecognised event type");
      throw new UnsupportedOperationException("Unknown event: " + type);
    }
    String transactionId = doSendEvent(type, sendInfo);
    log.debug("Sent {} with transactionId {}", event.getEventType(), transactionId);
    return transactionId;
  }

  private String doSendEvent(EventType eventType, SendInfo sendInfo) {
    EventPayload payload = sendInfo.getPayload();

    if (!payload.getClass().equals(eventType.getPayloadType())) {
      log.with("payloadType", payload.getClass())
          .with("eventType", eventType)
          .error("Payload incompatible for event type");
      String errorMessage =
          "Payload type '"
              + payload.getClass()
              + "' incompatible for event type '"
              + eventType
              + "'";
      throw new IllegalArgumentException(errorMessage);
    }

    RoutingKey routingKey = RoutingKey.forType(eventType);
    if (routingKey == null) {
      log.with("eventType", eventType).error("Routing key for eventType not configured");
      String errorMessage = "Routing key for eventType '" + eventType + "' not configured";
      throw new UnsupportedOperationException(errorMessage);
    }

    GenericEvent genericEvent = eventType.getBuilder().create(sendInfo);
    if (genericEvent == null) {
      log.with("eventType", eventType).error("Payload for eventType not configured");
      String errorMessage =
          payload.getClass().getName() + " for EventType '" + eventType + "' not supported yet";
      throw new UnsupportedOperationException(errorMessage);
    }

    try {
      sendToRabbit(routingKey, genericEvent);
    } catch (Exception e) {
      boolean backup = eventPersistence != null;
      log.with("eventType", eventType)
          .with("routingKey", routingKey)
          .with("backup", backup)
          .error(e, "Failed to send event but will now backup to firestore");

      if (!backup) {
        throw new EventPublishException("Rabbit failed to send event", e);
      }

      // Save event to persistent store
      try {
        eventPersistence.persistEvent(eventType, genericEvent);
        log.with("eventType", eventType)
            .with("routingKey", routingKey)
            .info("Event data saved to persistent store");
      } catch (Exception epe) {
        // There is no hope. Neither Rabbit or Persistence are working
        log.with("eventType", eventType)
            .with("routingKey", routingKey)
            .error(epe, "Backup event persistence failed following Rabbit failure");
        throw new EventPublishException(
            "Backup event persistence failed following Rabbit failure", e);
      }
    }

    return genericEvent.getEvent().getTransactionId();
  }

  private void sendToRabbit(RoutingKey routingKey, GenericEvent genericEvent) {
    if (circuitBreaker == null) {
      sendToRabbit(routingKey, genericEvent, "");
    } else {
      try {
        this.circuitBreaker.run(
            () -> {
              sendToRabbit(routingKey, genericEvent, "within circuit-breaker");
              return null;
            },
            throwable -> {
              throw new EventCircuitBreakerException(throwable);
            });
      } catch (EventCircuitBreakerException e) {
        log.debug("{}: {}", e.getMessage(), e.getCause().getMessage());
        throw e;
      }
    }
  }

  private void sendToRabbit(
      RoutingKey routingKey, GenericEvent genericEvent, String loggingMsgSuffix) {
    EventType eventType = genericEvent.getEvent().getType();
    log.with("eventType", eventType)
        .with("routingKey", routingKey)
        .info("Sending message to rabbit {}", loggingMsgSuffix);
    sender.sendEvent(routingKey, genericEvent);
    log.with("eventType", eventType)
        .with("routingKey", routingKey)
        .info("Message sent successfully to rabbit");
  }
}
