package uk.gov.ons.ctp.common.event;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;

import java.util.Date;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import uk.gov.ons.ctp.common.event.EventPublisher.Channel;
import uk.gov.ons.ctp.common.event.EventPublisher.EventType;
import uk.gov.ons.ctp.common.event.EventPublisher.Source;
import uk.gov.ons.ctp.common.event.model.GenericEvent;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class EventPublisherTestUtil {

  static void assertHeader(
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
}
