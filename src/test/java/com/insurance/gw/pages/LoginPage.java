package com.insurance.gw.pages;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

public class LoginPage extends BasePage {

    private static final By TITLE = By.id("Login:Title");
    private static final By USERNAME = By.id("Login:LoginScreen:LoginDV:username");
    private static final By PASSWORD = By.id("Login:LoginScreen:LoginDV:password");
    private static final By LOGIN_BUTTON = By.id("Login:LoginScreen:LoginDV:submit");

    public LoginPage(WebDriver driver) {
        super(driver);
        waitForPage(TITLE);
    }

    /** Fills the form and clicks Log In. Returns void because the outcome (Desktop or error) depends on the data. */
    public void login(String username, String password) {
        type(USERNAME, username);
        type(PASSWORD, password);
        click(LOGIN_BUTTON);
    }
}
