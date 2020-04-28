package uk.gov.ons.ctp.common.event.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
public class QuestionnaireLinkedEvent extends GenericEvent {

  private QuestionnaireLinkedPayload payload = new QuestionnaireLinkedPayload();
}
