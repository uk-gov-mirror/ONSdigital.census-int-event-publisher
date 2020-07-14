package uk.gov.ons.ctp.common.event.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RespondentRefusalDetails implements EventPayload {

  private String type;
  private String agentId;
  private String callId;

  @JsonProperty(value = "isHouseholder")
  private boolean isHouseholder;

  private CollectionCaseCompact collectionCase;
  private ContactCompact contact;
  private AddressCompact address;
}
