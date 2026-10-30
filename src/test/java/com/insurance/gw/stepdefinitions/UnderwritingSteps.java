package com.insurance.gw.stepdefinitions;

import com.insurance.gw.context.TestContext;
import com.insurance.gw.model.UserRole;
import com.insurance.gw.pages.CommonPage;
import com.insurance.gw.pages.DesktopPage;
import com.insurance.gw.pages.QuotePage;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import static org.assertj.core.api.Assertions.assertThat;

/** Underwriting referral workflow: the work item reaches the Underwriter's Desktop as an Activity. */
public class UnderwritingSteps {

    private final TestContext ctx;

    public UnderwritingSteps(TestContext ctx) {
        this.ctx = ctx;
    }

    @Then("a referral activity for the submission should be waiting on the Underwriter desktop")
    public void referralActivityShouldExist() {
        DesktopPage desktop = new CommonPage(ctx.driver()).ensureLoggedInAs(UserRole.UNDERWRITER);
        assertThat(desktop.hasActivityContaining(ctx.submissionNumber()))
                .as("UW referral activity for " + ctx.submissionNumber()).isTrue();
    }

    @When("I open the referral activity for the submission")
    public void iOpenTheReferralActivity() {
        new DesktopPage(ctx.driver()).openActivityContaining(ctx.submissionNumber());
        new QuotePage(ctx.driver()); // wait until the quote screen is shown
    }

    @When("I approve the referred submission")
    public void iApproveTheReferredSubmission() {
        new QuotePage(ctx.driver()).approve();
    }

    @When("I reject the referred submission")
    public void iRejectTheReferredSubmission() {
        new QuotePage(ctx.driver()).reject();
    }
}
