package uk.gov.ons.ctp.common.event;

import java.util.Date;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import uk.gov.ons.ctp.common.event.model.Address;
import uk.gov.ons.ctp.common.event.model.AddressCompact;
import uk.gov.ons.ctp.common.event.model.AddressModification;
import uk.gov.ons.ctp.common.event.model.AddressNotValid;
import uk.gov.ons.ctp.common.event.model.CollectionCase;
import uk.gov.ons.ctp.common.event.model.CollectionCaseCompact;
import uk.gov.ons.ctp.common.event.model.Contact;
import uk.gov.ons.ctp.common.event.model.Feedback;
import uk.gov.ons.ctp.common.event.model.QuestionnaireLinkedDetails;
import uk.gov.ons.ctp.common.event.model.RespondentAuthenticatedResponse;
import uk.gov.ons.ctp.common.event.model.RespondentRefusalDetails;
import uk.gov.ons.ctp.common.event.model.SurveyLaunchedResponse;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class EventFixture {
  public static final UUID CASE_ID = UUID.fromString("dc4477d1-dd3f-4c69-b181-7ff725dc9fa4");
  public static final UUID INDIVIDUAL_CASE_ID = UUID.randomUUID();
  public static final String QUESTIONNAIRE_ID = "1110000009";
  public static final String UPRN_1 = "1";
  public static final String UPRN_2 = "2";
  public static final Date CREATED_DATE_TIME = new Date();
  public static final boolean ADDRESS_INVALID = true;

  public static CollectionCase createCollectionCase() {
    CollectionCase cc = new CollectionCase();

    String caseRef = "10000000010";
    String survey = "Census";
    String collectionExerciseId = "n66de4dc-3c3b-11e9-b210-d663bd873d93";
    String actionableFrom = "2011-08-12T20:17:46.384Z";

    Address address = createAddress();
    Contact contact = createContact();

    cc.setId(CASE_ID.toString());
    cc.setCaseRef(caseRef);
    cc.setSurvey(survey);
    cc.setCollectionExerciseId(collectionExerciseId);
    cc.setAddress(address);
    cc.setContact(contact);
    cc.setActionableFrom(actionableFrom);
    cc.setHandDelivery(true);
    cc.setAddressInvalid(ADDRESS_INVALID);
    cc.setCreatedDateTime(CREATED_DATE_TIME);

    return cc;
  }

  public static Address createAddress() {
    Address address = new Address();
    address.setAddressLine1("1 main street");
    address.setAddressLine2("upper upperingham");
    address.setAddressLine3("");
    address.setTownName("upton");
    address.setPostcode("UP103UP");
    address.setRegion("E");
    address.setLatitude("50.863849");
    address.setLongitude("-1.229710");
    address.setUprn("XXXXXXXXXXXXX");
    address.setAddressType("CE");
    address.setEstabType("XXX");
    return address;
  }

  public static Contact createContact() {
    Contact contact = new Contact();
    contact.setTitle("Ms");
    contact.setForename("jo");
    contact.setSurname("smith");
    contact.setTelNo("+447890000000");
    return contact;
  }

  public static AddressNotValid createAddressNotValid() {
    CollectionCaseCompact collectionCase = new CollectionCaseCompact(UUID.randomUUID());
    return AddressNotValid.builder()
        .collectionCase(collectionCase)
        .notes("buy weetabix")
        .reason("DERELICT")
        .build();
  }

  public static RespondentRefusalDetails createRespondentRefusalDetails() {
    RespondentRefusalDetails respondentRefusalDetails = new RespondentRefusalDetails();
    respondentRefusalDetails.setAgentId("x1");
    respondentRefusalDetails.setType("EXTRAORDINARY_REFUSAL");
    return respondentRefusalDetails;
  }

  public static AddressModification createAddressModification() {
    AddressCompact originalAddress = new AddressCompact();
    AddressCompact newAddress = new AddressCompact();
    originalAddress.setUprn(UPRN_1);
    newAddress.setUprn(UPRN_2);

    return AddressModification.builder()
        .collectionCase(new CollectionCaseCompact())
        .originalAddress(originalAddress)
        .newAddress(newAddress)
        .build();
  }

  public static Feedback createFeedback() {
    Feedback feedbackResponse = new Feedback();
    feedbackResponse.setPageUrl("url-x");
    feedbackResponse.setPageTitle("randomPage");
    feedbackResponse.setFeedbackText("Bla bla bla");
    return feedbackResponse;
  }

  public static QuestionnaireLinkedDetails createQuestionnaireLinkedDetails() {

    QuestionnaireLinkedDetails questionnaireLinked = new QuestionnaireLinkedDetails();
    questionnaireLinked.setQuestionnaireId(QUESTIONNAIRE_ID);
    questionnaireLinked.setCaseId(CASE_ID);
    questionnaireLinked.setIndividualCaseId(INDIVIDUAL_CASE_ID);
    return questionnaireLinked;
  }

  public static SurveyLaunchedResponse createSurveyLaunchedResponse() {
    SurveyLaunchedResponse surveyLaunchedResponse =
        SurveyLaunchedResponse.builder().questionnaireId(QUESTIONNAIRE_ID).caseId(CASE_ID).build();
    return surveyLaunchedResponse;
  }

  public static RespondentAuthenticatedResponse createRespondentAuthenticatedResponse() {
    RespondentAuthenticatedResponse respondentAuthenticatedResponse =
        RespondentAuthenticatedResponse.builder()
            .questionnaireId(QUESTIONNAIRE_ID)
            .caseId(CASE_ID)
            .build();
    return respondentAuthenticatedResponse;
  }
}
