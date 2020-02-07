package uk.gov.ons.ctp.common.event.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NewAddress implements EventPayload {

  private String sourceCaseId;
  private CollectionCaseNewAddress collectionCase;
}
