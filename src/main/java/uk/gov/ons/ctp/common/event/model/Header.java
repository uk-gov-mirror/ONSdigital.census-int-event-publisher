package uk.gov.ons.ctp.common.event.model;

import java.util.Date;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.ons.ctp.common.event.EventPublisher.Channel;
import uk.gov.ons.ctp.common.event.EventPublisher.EventType;
import uk.gov.ons.ctp.common.event.EventPublisher.Source;
import uk.gov.ons.ctp.common.jackson.CustomDateSerialiser;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Header {

  private EventType type;
  private Source source;
  private Channel channel;

  @JsonSerialize(using = CustomDateSerialiser.class)
  private Date dateTime;

  private String transactionId;
}
