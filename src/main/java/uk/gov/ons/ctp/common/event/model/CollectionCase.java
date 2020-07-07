package uk.gov.ons.ctp.common.event.model;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import java.util.Date;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.ons.ctp.common.jackson.CustomDateSerialiser;

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
  private boolean handDelivery;
  private boolean addressInvalid;
  private Integer ceExpectedCapacity;

  @JsonSerialize(using = CustomDateSerialiser.class)
  private Date createdDateTime;
}
