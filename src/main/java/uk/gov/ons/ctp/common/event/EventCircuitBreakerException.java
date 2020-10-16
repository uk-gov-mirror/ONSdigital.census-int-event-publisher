package uk.gov.ons.ctp.common.event;

public class EventCircuitBreakerException extends EventPublishException {
  private static final long serialVersionUID = 7928252329309913211L;

  public EventCircuitBreakerException(String message) {
    super(message);
  }

  public EventCircuitBreakerException(String message, Throwable cause) {
    super(message, cause);
  }

  public EventCircuitBreakerException(Throwable cause) {
    super("Failed send within circuit breaker", cause);
  }
}
