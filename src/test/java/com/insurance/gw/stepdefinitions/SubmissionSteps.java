package com.insurance.gw.stepdefinitions;

import com.insurance.gw.context.TestContext;
import com.insurance.gw.model.SubmissionData;
import com.insurance.gw.pages.CommonPage;
import com.insurance.gw.pages.NewSubmissionPage;
import com.insurance.gw.pages.QuotePage;
import com.insurance.gw.utils.TestDataReader;
import io.cucumber.datatable.DataTable;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

/** PolicyCenter: New Submission -> Quote. The submission data is built up step by step in the TestContext. */
public class SubmissionSteps {

    private final TestContext ctx;

    public SubmissionSteps(TestContext ctx) {
        this.ctx = ctx;
    }

    @Given("I start a new personal auto submission")
    public void iStartANewSubmission() {
        ctx.setSubmission(new SubmissionData());
        new CommonPage(ctx.driver()).openNewSubmission();
    }

    @Given("I start a new personal auto submission for the {string} customer")
    public void iStartANewSubmissionForProfile(String profile) {
        ctx.setSubmission(TestDataReader.profile(profile)); // data-driven: profile comes from customers.json
        new CommonPage(ctx.driver()).openNewSubmission();
    }

    /** DataTable (key/value form) -> asMap. */
    @Given("I enter the submission details:")
    public void iEnterTheSubmissionDetails(DataTable table) {
        ctx.submission().apply(table.asMap(String.class, String.class));
    }

    @Given("the driver is {string} aged {int} living in {string} with {int} violations")
    public void theDriverIs(String fullName, int age, String state, int violations) {
        String[] name = fullName.split(" ", 2);
        SubmissionData data = ctx.submission();
        data.firstName = name[0];
        data.lastName = name.length > 1 ? name[1] : "Tester";
        data.driverAge = age;
        data.state = state;
        data.violations = violations;
    }

    /** Vehicle age (not model year) keeps the rating examples valid whatever year the suite runs in. */
    @Given("the vehicle is a {int} year old {string} {string}")
    public void theVehicleIs(int ageInYears, String make, String model) {
        ctx.submission().vehicleYear = LocalDate.now().getYear() - ageInYears;
        ctx.submission().vehicleMake = make;
        ctx.submission().vehicleModel = model;
    }

    @Given("the coverages are liability {string}, collision deductible {string} and comprehensive {string}")
    public void theCoveragesAre(String liability, String collision, String comprehensive) {
        ctx.submission().liability = liability;
        ctx.submission().collision = collision;
        ctx.submission().comprehensive = comprehensive.equalsIgnoreCase("Yes");
    }

    @Given("the policy term is {string} with payment plan {string}")
    public void thePolicyTermIs(String term, String paymentPlan) {
        ctx.submission().term = term;
        ctx.submission().paymentPlan = paymentPlan;
    }

    @When("I request the quote")
    public void iRequestTheQuote() {
        new NewSubmissionPage(ctx.driver()).fillSubmission(ctx.submission()).clickQuote();
    }

    @Then("the quote should be created with status {string}")
    public void theQuoteShouldBeCreated(String status) {
        QuotePage quote = new QuotePage(ctx.driver());
        ctx.setSubmissionNumber(quote.submissionNumber());
        ctx.setPolicyPremium(quote.premium());
        assertThat(quote.submissionNumber()).matches("SUB-\\d+");
        assertThat(quote.status()).isEqualTo(status);
    }

    @Then("the total premium should be {money}")
    public void theTotalPremiumShouldBe(BigDecimal expected) {
        assertThat(new QuotePage(ctx.driver()).premium()).isEqualByComparingTo(expected);
    }

    @Then("the quote should list the underwriting issue {string}")
    public void theQuoteShouldListTheIssue(String issue) {
        assertThat(new QuotePage(ctx.driver()).underwritingIssues()).contains(issue);
    }

    @Then("the quote should have no underwriting issues")
    public void theQuoteShouldHaveNoIssues() {
        assertThat(new QuotePage(ctx.driver()).underwritingIssues()).containsExactly("No underwriting issues");
    }

    @Then("the Bind button should be {word}")
    public void theBindButtonShouldBe(String state) {
        boolean enabled = new QuotePage(ctx.driver()).isBindEnabled();
        assertThat(enabled).as("Bind button enabled?").isEqualTo(state.equals("enabled"));
    }

    @Then("the submission status should be {string}")
    public void theSubmissionStatusShouldBe(String status) {
        assertThat(new QuotePage(ctx.driver()).status()).isEqualTo(status);
    }
}
