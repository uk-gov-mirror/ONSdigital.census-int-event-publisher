package uk.gov.ons.ctp.common.event.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Feedback implements EventPayload {

  private String pageUrl;
  private String pageTitle;
  private String feedbackText;
}
