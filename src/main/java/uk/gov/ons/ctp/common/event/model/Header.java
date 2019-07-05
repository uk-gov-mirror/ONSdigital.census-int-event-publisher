package uk.gov.ons.ctp.common.event.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.util.Date;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.ons.ctp.common.event.EventPublisher.Channel;
import uk.gov.ons.ctp.common.event.EventPublisher.EventType;
import uk.gov.ons.ctp.common.event.EventPublisher.Source;
import uk.gov.ons.ctp.common.time.DateTimeUtil;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Header {

  private EventType type;
  private Source source;
  private Channel channel;

  @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = DateTimeUtil.DATE_FORMAT_IN_JSON)
  private Date dateTime;

  private String transactionId;
}
