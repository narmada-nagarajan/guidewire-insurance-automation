package com.insurance.gw.stepdefinitions;

import com.insurance.gw.context.TestContext;
import com.insurance.gw.model.UserRole;
import com.insurance.gw.pages.CommonPage;
import com.insurance.gw.pages.DesktopPage;
import com.insurance.gw.pages.LoginPage;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import static org.assertj.core.api.Assertions.assertThat;

/** Steps reused by every feature: login, switching roles, reading the message banner. */
public class CommonSteps {

    private final TestContext ctx;

    public CommonSteps(TestContext ctx) {
        this.ctx = ctx;
    }

    @Given("the InsureSuite application is open")
    public void theApplicationIsOpen() {
        new LoginPage(ctx.driver()); // constructor waits for the login screen
    }

    @Given("I am logged in as a(n) {role}")
    public void iAmLoggedInAs(UserRole role) {
        new CommonPage(ctx.driver()).ensureLoggedInAs(role);
    }

    @When("I switch to the {role} user")
    public void iSwitchToUser(UserRole role) {
        new CommonPage(ctx.driver()).ensureLoggedInAs(role);
    }

    @When("I log in with username {string} and password {string}")
    public void iLogInWith(String username, String password) {
        new LoginPage(ctx.driver()).login(username, password);
    }

    @Then("I should land on the Desktop")
    public void iShouldLandOnTheDesktop() {
        DesktopPage desktop = new DesktopPage(ctx.driver());
        assertThat(desktop.isLoggedIn()).isTrue();
    }

    @Then("the logged-in role should be {string}")
    public void theLoggedInRoleShouldBe(String role) {
        assertThat(new DesktopPage(ctx.driver()).role()).isEqualTo(role);
    }

    @Then("I should see the error {string}")
    public void iShouldSeeTheError(String message) {
        assertThat(new CommonPage(ctx.driver()).alertText()).contains(message);
    }

    @Then("I should see the message {string}")
    public void iShouldSeeTheMessage(String message) {
        assertThat(new CommonPage(ctx.driver()).alertText()).contains(message);
    }
}
