package uk.gov.ons.ctp.common.event;

public class EventPublishException extends RuntimeException {
  private static final long serialVersionUID = -8684755728584067879L;

  public EventPublishException(Throwable cause) {
    super("Failed to publish event", cause);
  }
}
