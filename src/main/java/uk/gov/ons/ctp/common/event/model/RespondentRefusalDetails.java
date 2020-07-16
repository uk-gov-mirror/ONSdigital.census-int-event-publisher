package uk.gov.ons.ctp.common.event.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RespondentRefusalDetails implements EventPayload {

  private String type;
  private String agentId;
  private String callId;

  @Getter(AccessLevel.NONE)
  @Setter(AccessLevel.NONE)
  private boolean isHouseholder;

  private CollectionCaseCompact collectionCase;
  private ContactCompact contact;
  private AddressCompact address;

  // The explicit get and set methods workaround a problem caused by the interaction of Lombok and
  // Jackson.
  // By default lombok creates an isHouseholder() method, but then Jackson serialises this as
  // simply 'householder'.
  // BTW, a JsonProperty annotation still leaves the isHouseholder() method visible, so the
  // generated Json contains a 'isHouseholder' and a 'householder' field.
  @JsonProperty(value = "isHouseholder")
  public boolean isHouseholder() {
    return isHouseholder;
  }

  @JsonProperty(value = "isHouseholder")
  public void setHouseholder(boolean isHouseholder) {
    this.isHouseholder = isHouseholder;
  }
}
