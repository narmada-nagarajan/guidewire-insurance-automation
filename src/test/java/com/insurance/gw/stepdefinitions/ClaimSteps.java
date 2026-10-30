package com.insurance.gw.stepdefinitions;

import com.insurance.gw.context.TestContext;
import com.insurance.gw.model.UserRole;
import com.insurance.gw.pages.ClaimSummaryPage;
import com.insurance.gw.pages.CommonPage;
import com.insurance.gw.pages.DesktopPage;
import com.insurance.gw.pages.FnolPage;
import com.insurance.gw.utils.MoneyUtils;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** ClaimCenter: FNOL -> exposure -> reserve -> payment -> close. */
public class ClaimSteps {

    private final TestContext ctx;

    public ClaimSteps(TestContext ctx) {
        this.ctx = ctx;
    }

    // ------------------------------------------------------------------ FNOL
    /** Setup step used as a Background by the reserve/payment feature. */
    @Given("a collision claim has been reported on the policy")
    public void aCollisionClaimHasBeenReported() {
        new CommonPage(ctx.driver()).ensureLoggedInAs(UserRole.CLAIMS_ADJUSTER);
        FnolPage fnol = new CommonPage(ctx.driver()).openFnol();
        fnol.searchPolicy(ctx.policyNumber(), LocalDate.now());
        assertThat(fnol.isPolicyVerified()).isTrue();
        fnol.enterLossDetails("Collision", "Setup claim created by automation");
        fnol.finish();
        ClaimSummaryPage claim = new ClaimSummaryPage(ctx.driver());
        ctx.setClaimNumber(claim.claimNumber());
    }

    @When("I search the policy in FNOL with a loss date {int} days from today")
    public void iSearchThePolicyInFnol(int daysFromToday) {
        new CommonPage(ctx.driver()).openFnol().searchPolicy(ctx.policyNumber(), LocalDate.now().plusDays(daysFromToday));
    }

    @Then("the policy should be verified for the claim")
    public void thePolicyShouldBeVerified() {
        assertThat(new FnolPage(ctx.driver()).isPolicyVerified()).isTrue();
    }

    /** DocString (the triple-quoted block) carries a multi-line free-text value. */
    @When("I report a {string} loss with the description:")
    public void iReportALoss(String lossType, String description) {
        new FnolPage(ctx.driver()).enterLossDetails(lossType, description);
    }

    @When("injuries were reported")
    public void injuriesWereReported() {
        new FnolPage(ctx.driver()).setInjuriesReported(true);
    }

    @When("I finish the first notice of loss")
    public void iFinishTheFnol() {
        new FnolPage(ctx.driver()).finish();
    }

    @Then("a claim should be created with status {string}")
    public void aClaimShouldBeCreated(String status) {
        ClaimSummaryPage claim = new ClaimSummaryPage(ctx.driver());
        ctx.setClaimNumber(claim.claimNumber());
        assertThat(claim.claimNumber()).matches("CLM-\\d+");
        assertThat(claim.status()).isEqualTo(status);
    }

    @Then("the claim severity should be {string}")
    public void theClaimSeverityShouldBe(String severity) {
        assertThat(new ClaimSummaryPage(ctx.driver()).severity()).isEqualTo(severity);
    }

    @Then("the claim exposure should be {string}")
    public void theClaimExposureShouldBe(String exposure) {
        assertThat(new ClaimSummaryPage(ctx.driver()).exposureType()).isEqualTo(exposure);
    }

    @Then("the claim deductible should be {money}")
    public void theClaimDeductibleShouldBe(BigDecimal deductible) {
        assertThat(new ClaimSummaryPage(ctx.driver()).deductible()).isEqualByComparingTo(deductible);
    }

    // ------------------------------------------------------------------ reserves
    @When("I set the claim reserve to {money}")
    public void iSetTheClaimReserve(BigDecimal amount) {
        new ClaimSummaryPage(ctx.driver()).setReserve(amount);
    }

    @Then("the open reserve should be {money}")
    public void theOpenReserveShouldBe(BigDecimal amount) {
        assertThat(new ClaimSummaryPage(ctx.driver()).openReserve()).isEqualByComparingTo(amount);
    }

    @Then("the reserve status should be {string}")
    public void theReserveStatusShouldBe(String status) {
        assertThat(new ClaimSummaryPage(ctx.driver()).reserveStatus()).isEqualTo(status);
    }

    @When("I open the reserve approval activity for the claim")
    public void iOpenTheReserveApprovalActivity() {
        new DesktopPage(ctx.driver()).openActivityContaining(ctx.claimNumber());
        new ClaimSummaryPage(ctx.driver());
    }

    @When("I approve the pending reserve")
    public void iApproveThePendingReserve() {
        new ClaimSummaryPage(ctx.driver()).approvePendingReserve();
    }

    // ------------------------------------------------------------------ payments & closure
    @When("I make a claim payment of {money}")
    public void iMakeAClaimPayment(BigDecimal gross) {
        new ClaimSummaryPage(ctx.driver()).makePayment(gross);
    }

    @Then("the latest claim payment should have a net amount of {money}")
    public void theLatestPaymentShouldHaveNetAmount(BigDecimal net) {
        List<List<String>> payments = new ClaimSummaryPage(ctx.driver()).payments();
        assertThat(payments).isNotEmpty();
        List<String> latest = payments.get(payments.size() - 1);
        assertThat(MoneyUtils.parse(latest.get(3))).isEqualByComparingTo(net);
        assertThat(latest.get(4)).isEqualTo("Issued");
    }

    @Then("the total paid on the claim should be {money}")
    public void theTotalPaidShouldBe(BigDecimal amount) {
        assertThat(new ClaimSummaryPage(ctx.driver()).totalPaid()).isEqualByComparingTo(amount);
    }

    @When("I close the claim")
    public void iCloseTheClaim() {
        new ClaimSummaryPage(ctx.driver()).closeClaim();
    }

    @Then("the claim status should be {string}")
    public void theClaimStatusShouldBe(String status) {
        assertThat(new ClaimSummaryPage(ctx.driver()).status()).isEqualTo(status);
    }
}
