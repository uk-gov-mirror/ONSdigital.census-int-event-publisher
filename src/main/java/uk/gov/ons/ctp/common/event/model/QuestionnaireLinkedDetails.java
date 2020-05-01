package uk.gov.ons.ctp.common.event.model;

import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QuestionnaireLinkedDetails implements EventPayload {

  private String questionnaireId;
  private UUID caseId;
  private UUID individualCaseId;
}
