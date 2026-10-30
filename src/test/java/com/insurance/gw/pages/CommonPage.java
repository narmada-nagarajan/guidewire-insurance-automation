package com.insurance.gw.pages;

import com.insurance.gw.model.UserRole;
import org.openqa.selenium.WebDriver;

/** Page-less helper used for things that exist on every screen (message banner, switching users). */
public class CommonPage extends BasePage {

    public CommonPage(WebDriver driver) {
        super(driver);
    }

    /** Makes sure the browser is logged in as the requested role, logging out of another user first if needed. */
    public DesktopPage ensureLoggedInAs(UserRole role) {
        waitForLoading();
        if (isLoggedIn()) {
            if (loggedInUserInfo().contains("(" + role.label() + ")")) {
                return new DesktopPage(driver);
            }
            logout();
        }
        LoginPage login = new LoginPage(driver);
        login.login(role.username(), UserRole.DEFAULT_PASSWORD);
        return new DesktopPage(driver);
    }
}
