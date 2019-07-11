package uk.gov.ons.ctp.common.event.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CommonPayload {

  @JsonInclude(Include.NON_NULL)
  private Response response;

  @JsonInclude(Include.NON_NULL)
  private RespondentRefusalDetails refusal;

  @JsonInclude(Include.NON_NULL)
  private RespondentAuthenticatedResponse respondentAuthenticatedResponse;

  @JsonInclude(Include.NON_NULL)
  private FulfilmentRequest fulfilmentRequest;

  @JsonInclude(Include.NON_NULL)
  private CollectionCase collectionCase;

  @JsonInclude(Include.NON_NULL)
  private UAC uac;
}
