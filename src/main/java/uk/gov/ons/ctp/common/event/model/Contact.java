package uk.gov.ons.ctp.common.event.model;

import com.godaddy.logging.LoggingScope;
import com.godaddy.logging.Scope;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
public class Contact extends ContactCompact {

  @LoggingScope(scope = Scope.SKIP)
  private String telNo;
}
