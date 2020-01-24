package uk.gov.ons.ctp.common.event.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CollectionCase implements EventPayload {

  private String id;
  private String caseRef;
  private String caseType;
  private String survey;
  private String collectionExerciseId;
  private Address address = new Address();
  private Contact contact = new Contact();
  private String actionableFrom;
}
