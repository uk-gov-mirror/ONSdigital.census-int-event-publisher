package uk.gov.ons.ctp.common.event.model;

import com.godaddy.logging.LoggingScope;
import com.godaddy.logging.Scope;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Contact {

  @LoggingScope(scope = Scope.SKIP)
  private String title;
  @LoggingScope(scope = Scope.SKIP)
  private String forename;
  @LoggingScope(scope = Scope.SKIP)
  private String surname;
  @LoggingScope(scope = Scope.SKIP)
  private String email;
  @LoggingScope(scope = Scope.SKIP)
  private String telNo;
}
