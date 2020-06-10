package uk.gov.ons.ctp.common.event.model;

import com.godaddy.logging.LoggingScope;
import com.godaddy.logging.Scope;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UAC implements EventPayload {

  @LoggingScope(scope = Scope.SKIP)
  private String uacHash;

  private String active;
  private String questionnaireId;
  private String caseId;
  private String formType;
}
