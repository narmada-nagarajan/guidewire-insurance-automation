package com.insurance.gw.stepdefinitions;

import com.insurance.gw.context.TestContext;
import com.insurance.gw.model.SubmissionData;
import com.insurance.gw.model.UserRole;
import com.insurance.gw.pages.CancelPolicyPage;
import com.insurance.gw.pages.CommonPage;
import com.insurance.gw.pages.NewSubmissionPage;
import com.insurance.gw.pages.PolicySummaryPage;
import com.insurance.gw.pages.QuotePage;
import com.insurance.gw.pages.SearchPage;
import com.insurance.gw.utils.PremiumCalculator;
import com.insurance.gw.utils.TestDataReader;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * PolicyCenter policy lifecycle: issue (bind), change (endorsement), cancel, renew, search.
 * Step classes are created per scenario by PicoContainer, so private fields below are safe scenario state.
 */
public class PolicySteps {

    private static final BigDecimal ONE_CENT = new BigDecimal("0.01");

    private final TestContext ctx;

    // scenario-scoped working data (not needed by other step classes)
    private LocalDate changeDate;
    private BigDecimal premiumBeforeChange;
    private CancelPolicyPage cancelPage;
    private String cancelMethod;
    private LocalDate cancelDate;

    public PolicySteps(TestContext ctx) {
        this.ctx = ctx;
    }

    // ------------------------------------------------------------------ issuance
    /** Setup step ("Given"): creates a policy through the UI. In a real project this is usually done by API/DB. */
    @Given("an in-force personal auto policy exists for the {string} customer")
    public void anInForcePolicyExists(String profile) {
        SubmissionData data = TestDataReader.profile(profile);
        ctx.setSubmission(data);
        CommonPage common = new CommonPage(ctx.driver());
        common.ensureLoggedInAs(UserRole.PRODUCER);
        common.openNewSubmission().fillSubmission(data).clickQuote();
        QuotePage quote = new QuotePage(ctx.driver());
        ctx.setSubmissionNumber(quote.submissionNumber());
        captureIssuedPolicy(quote.bind());
    }

    @When("I bind the quote and issue the policy")
    public void iBindTheQuote() {
        captureIssuedPolicy(new QuotePage(ctx.driver()).bind());
    }

    private void captureIssuedPolicy(PolicySummaryPage policy) {
        ctx.setPolicyNumber(policy.policyNumber());
        ctx.setPolicyPremium(policy.totalPremium());
        ctx.setPolicyEffective(policy.effectiveDate());
        ctx.setPolicyExpiration(policy.expirationDate());
    }

    @Then("the policy should be issued with status {string}")
    public void thePolicyShouldBeIssued(String status) {
        PolicySummaryPage policy = new PolicySummaryPage(ctx.driver());
        assertThat(policy.policyNumber()).matches("PA-\\d{10}");
        assertThat(policy.status()).isEqualTo(status);
        assertThat(policy.totalPremium()).isEqualByComparingTo(ctx.policyPremium());
        captureIssuedPolicy(policy);
    }

    @Then("the policy period should span {int} months")
    public void thePolicyPeriodShouldSpan(int months) {
        PolicySummaryPage policy = new PolicySummaryPage(ctx.driver());
        assertThat(policy.expirationDate()).isEqualTo(policy.effectiveDate().plusMonths(months));
    }

    @Then("the policy transactions should include {string}")
    public void theTransactionsShouldInclude(String type) {
        assertThat(new PolicySummaryPage(ctx.driver()).transactionTypes()).contains(type);
    }

    @Then("the policy transactions should not include {string}")
    public void theTransactionsShouldNotInclude(String type) {
        assertThat(new PolicySummaryPage(ctx.driver()).transactionTypes()).doesNotContain(type);
    }

    @Then("the policy status should be {string}")
    public void thePolicyStatusShouldBe(String status) {
        assertThat(new PolicySummaryPage(ctx.driver()).status()).isEqualTo(status);
    }

    // ------------------------------------------------------------------ policy change (endorsement)
    @When("I add a {int} {string} {string} vehicle to the policy effective {int} days after inception")
    public void iAddAVehicle(int year, String make, String model, int daysAfterInception) {
        PolicySummaryPage policy = new PolicySummaryPage(ctx.driver());
        premiumBeforeChange = policy.totalPremium();
        changeDate = ctx.policyEffective().plusDays(daysAfterInception);
        policy.startPolicyChange().addVehicle(changeDate, year, make, model);
    }

    @Then("the policy should have {int} vehicles")
    public void thePolicyShouldHaveVehicles(int count) {
        assertThat(new PolicySummaryPage(ctx.driver()).vehicleCount()).isEqualTo(count);
    }

    /**
     * The new vehicle has the same rating profile as the first one, so its full-term premium equals the
     * issuance premium; the endorsement must charge only the remaining part of the term (pro-rata).
     */
    @Then("the endorsement premium should be pro-rated for the remaining policy term")
    public void theEndorsementPremiumShouldBeProRated() {
        PolicySummaryPage policy = new PolicySummaryPage(ctx.driver());
        BigDecimal expected = PremiumCalculator.proRata(ctx.policyPremium(), ctx.policyEffective(),
                ctx.policyExpiration(), changeDate);
        assertThat(policy.transactionTypes()).last().isEqualTo("Endorsement");
        assertThat(policy.lastTransactionAmount()).isCloseTo(expected, within(ONE_CENT));
        assertThat(policy.totalPremium()).isCloseTo(premiumBeforeChange.add(expected), within(ONE_CENT));
    }

    // ------------------------------------------------------------------ cancellation
    @When("I start the cancellation with reason {string} using the {string} method effective {int} days after inception")
    public void iStartTheCancellation(String reason, String method, int daysAfterInception) {
        cancelMethod = method;
        cancelDate = ctx.policyEffective().plusDays(daysAfterInception);
        cancelPage = new PolicySummaryPage(ctx.driver()).startCancellation();
        cancelPage.startCancellation(reason, method, cancelDate);
    }

    @When("I accept the cancellation confirmation")
    public void iAcceptTheCancellationConfirmation() {
        assertThat(cancelPage.confirmationText()).contains(ctx.policyNumber());
        cancelPage.acceptConfirmation();
    }

    @When("I decline the cancellation confirmation")
    public void iDeclineTheCancellationConfirmation() {
        cancelPage.dismissConfirmation();
    }

    @When("I return to the policy summary")
    public void iReturnToThePolicySummary() {
        cancelPage.backToPolicy();
    }

    @Given("the policy is cancelled with the {string} method")
    public void thePolicyIsCancelled(String method) {
        iStartTheCancellation("Insured Request", method, 0);
        iAcceptTheCancellationConfirmation();
        new PolicySummaryPage(ctx.driver()); // wait for the summary of the cancelled policy
    }

    @Then("the return premium should be calculated correctly for the chosen method")
    public void theReturnPremiumShouldBeCalculated() {
        PolicySummaryPage policy = new PolicySummaryPage(ctx.driver());
        BigDecimal expected = PremiumCalculator.returnPremium(cancelMethod, ctx.policyPremium(),
                ctx.policyEffective(), ctx.policyExpiration(), cancelDate);
        assertThat(policy.returnPremium()).isCloseTo(expected, within(ONE_CENT));
        assertThat(policy.transactionTypes()).last().isEqualTo("Cancellation");
    }

    // ------------------------------------------------------------------ renewal
    @When("I renew the policy")
    public void iRenewThePolicy() {
        PolicySummaryPage policy = new PolicySummaryPage(ctx.driver());
        ctx.setPolicyPremium(policy.totalPremium());          // expiring premium
        ctx.setPolicyExpiration(policy.expirationDate());     // expiring term end = renewal start
        policy.renew();
    }

    @Then("the policy should be renewed into term {int}")
    public void thePolicyShouldBeRenewed(int term) {
        assertThat(new CommonPage(ctx.driver()).alertText()).contains("renewed");
        assertThat(new PolicySummaryPage(ctx.driver()).termNumber()).isEqualTo(term);
    }

    @Then("the renewal term should start when the expiring term ends")
    public void theRenewalShouldStartWhenExpiringTermEnds() {
        assertThat(new PolicySummaryPage(ctx.driver()).effectiveDate()).isEqualTo(ctx.policyExpiration());
    }

    @Then("the renewal premium should be {int} percent higher than the expiring premium")
    public void theRenewalPremiumShouldBeHigher(int percent) {
        BigDecimal expected = ctx.policyPremium()
                .multiply(BigDecimal.valueOf(100 + percent)).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        assertThat(new PolicySummaryPage(ctx.driver()).totalPremium()).isCloseTo(expected, within(ONE_CENT));
    }

    // ------------------------------------------------------------------ search
    @When("I search for the policy by its policy number")
    public void iSearchByPolicyNumber() {
        new CommonPage(ctx.driver()).openSearch().searchByPolicyNumber(ctx.policyNumber());
    }

    @Then("the search results should contain the policy with status {string}")
    public void theSearchResultsShouldContainThePolicy(String status) {
        List<List<String>> rows = new SearchPage(ctx.driver()).results();
        assertThat(rows).as("search results").anySatisfy(row -> {
            assertThat(row.get(0)).isEqualTo(ctx.policyNumber());
            assertThat(row.get(2)).isEqualTo(status);
        });
    }
}
