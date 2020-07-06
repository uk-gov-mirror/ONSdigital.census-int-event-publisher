package uk.gov.ons.ctp.common.event.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
public class Address extends AddressCompact {
  private String latitude;
  private String longitude;
  private String estabUprn;
  private String addressType;
  private String addressLevel;
}
