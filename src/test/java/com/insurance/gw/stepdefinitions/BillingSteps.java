package com.insurance.gw.stepdefinitions;

import com.insurance.gw.context.TestContext;
import com.insurance.gw.pages.BillingPage;
import com.insurance.gw.pages.CommonPage;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import java.math.BigDecimal;
import java.math.RoundingMode;

import static org.assertj.core.api.Assertions.assertThat;

/** BillingCenter: invoices created at issuance and payments applied to them. */
public class BillingSteps {

    private final TestContext ctx;

    public BillingSteps(TestContext ctx) {
        this.ctx = ctx;
    }

    @When("I open the billing account of the policy")
    public void iOpenTheBillingAccount() {
        new CommonPage(ctx.driver()).openBilling().findAccount(ctx.policyNumber());
    }

    @Then("{int} invoice(s) should be billed for the policy")
    public void invoicesShouldBeBilled(int count) {
        assertThat(new BillingPage(ctx.driver()).invoices()).hasSize(count);
    }

    @Then("the invoices should add up to the policy premium")
    public void theInvoicesShouldAddUp() {
        BigDecimal sum = new BillingPage(ctx.driver()).invoiceAmounts().stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        assertThat(sum).isEqualByComparingTo(ctx.policyPremium());
    }

    @When("I pay {int} percent of the policy premium")
    public void iPayPercentOfThePremium(int percent) {
        BigDecimal amount = ctx.policyPremium().multiply(BigDecimal.valueOf(percent))
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        new BillingPage(ctx.driver()).pay(amount);
        ctx.addBillingPayment(amount);
    }

    @When("I pay the full outstanding balance")
    public void iPayTheFullOutstandingBalance() {
        BillingPage billing = new BillingPage(ctx.driver());
        BigDecimal balance = billing.outstandingBalance();
        billing.pay(balance);
        ctx.addBillingPayment(balance);
    }

    @When("I try to pay {money} more than the outstanding balance")
    public void iTryToOverpay(BigDecimal extra) {
        BillingPage billing = new BillingPage(ctx.driver());
        billing.pay(billing.outstandingBalance().add(extra));
    }

    @Then("the billing status should be {string}")
    public void theBillingStatusShouldBe(String status) {
        assertThat(new BillingPage(ctx.driver()).billingStatus()).isEqualTo(status);
    }

    @Then("the outstanding balance should be the policy premium less the payments made")
    public void theOutstandingBalanceShouldBeReduced() {
        BigDecimal expected = ctx.policyPremium().subtract(ctx.totalPaidToBilling());
        assertThat(new BillingPage(ctx.driver()).outstandingBalance()).isEqualByComparingTo(expected);
    }

    @Then("the outstanding balance should be {money}")
    public void theOutstandingBalanceShouldBe(BigDecimal expected) {
        assertThat(new BillingPage(ctx.driver()).outstandingBalance()).isEqualByComparingTo(expected);
    }
}
