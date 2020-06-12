package uk.gov.ons.ctp.common.event.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CollectionCaseNewAddress implements EventPayload {

  private String id;
  private String caseType;
  private String survey;
  private String collectionExerciseId;
  private String organisationName;
  private Integer ceExpectedCapacity;
  private String fieldCoordinatorId;
  private String fieldOfficerId;
  private Address address = new Address();
}
