package uk.gov.ons.ctp.common.event;

import com.fasterxml.jackson.annotation.JsonRootName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonRootName("cucumberrabbitmq")
public class RabbitConnectionDetails {
  private String host;
  private Integer port;
  private String username;
  private String password;
}
