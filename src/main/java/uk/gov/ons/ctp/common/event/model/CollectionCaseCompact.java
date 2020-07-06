package uk.gov.ons.ctp.common.event.model;

import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CollectionCaseCompact {
  private UUID id;
  private String caseType;
  private Integer ceExpectedCapacity;

  public CollectionCaseCompact(UUID id) {
    this.id = id;
  }
}
