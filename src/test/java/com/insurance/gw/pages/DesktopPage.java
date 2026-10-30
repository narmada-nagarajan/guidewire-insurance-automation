package com.insurance.gw.pages;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

public class DesktopPage extends BasePage {

    private static final By TITLE = By.id("Desktop:Title");
    private static final By USER_ROLE = By.id("Desktop:UserRole");

    public DesktopPage(WebDriver driver) {
        super(driver);
        waitForPage(TITLE);
    }

    public String role() {
        return text(USER_ROLE);
    }

    private By activityLink(String subjectFragment) {
        return By.xpath("//tr[contains(@class,'activity-row')][contains(.,'" + subjectFragment + "')]//a[@class='activity-link']");
    }

    public boolean hasActivityContaining(String subjectFragment) {
        waitForLoading();
        return !driver.findElements(activityLink(subjectFragment)).isEmpty();
    }

    /** Opens the work item (submission / claim) behind an activity on the user's Desktop. */
    public void openActivityContaining(String subjectFragment) {
        click(activityLink(subjectFragment));
    }
}
