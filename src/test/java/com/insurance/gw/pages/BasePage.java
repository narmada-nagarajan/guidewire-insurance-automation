package com.insurance.gw.pages;

import com.insurance.gw.config.ConfigManager;
import org.openqa.selenium.By;
import org.openqa.selenium.ElementClickInterceptedException;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Parent of every page object (Page Object Model).
 * - holds the only copy of the wait / click / type logic, so page classes contain just locators + business actions
 * - every interaction first waits for the application's "Loading" overlay to disappear (typical of Guidewire
 *   screens, which re-render via AJAX) and then for the element itself (explicit waits, never Thread.sleep)
 * - page classes verify they are on the right page in their constructor via waitForPage(marker)
 */
public abstract class BasePage {

    private static final By LOADING = By.id("Loading");
    private static final By ALERT = By.id("Alert");
    private static final By TAB_BAR = By.id("TabBar");

    protected final WebDriver driver;
    protected final WebDriverWait wait;

    protected BasePage(WebDriver driver) {
        this.driver = driver;
        this.wait = new WebDriverWait(driver, Duration.ofSeconds(ConfigManager.getInt("timeout.explicit")));
    }

    // ------------------------------------------------------------------ waits
    protected void waitForPage(By marker) {
        wait.until(ExpectedConditions.visibilityOfElementLocated(marker));
        waitForLoading();
    }

    protected void waitForLoading() {
        wait.until(ExpectedConditions.invisibilityOfElementLocated(LOADING));
    }

    protected WebElement find(By locator) {
        waitForLoading();
        return wait.until(ExpectedConditions.visibilityOfElementLocated(locator));
    }

    // ------------------------------------------------------------------ element actions
    protected void click(By locator) {
        waitForLoading();
        WebElement element = wait.until(ExpectedConditions.elementToBeClickable(locator));
        try {
            element.click();
        } catch (ElementClickInterceptedException e) {
            // last-resort fallback when an overlay briefly covers the element
            ((JavascriptExecutor) driver).executeScript("arguments[0].click();", element);
        }
    }

    protected void type(By locator, String text) {
        WebElement element = find(locator);
        element.clear();
        element.sendKeys(text);
    }

    protected void selectByText(By locator, String visibleText) {
        new Select(find(locator)).selectByVisibleText(visibleText);
    }

    protected void setCheckbox(By locator, boolean wanted) {
        WebElement box = find(locator);
        if (box.isSelected() != wanted) {
            box.click();
        }
    }

    protected String text(By locator) {
        return find(locator).getText().trim();
    }

    protected boolean isEnabled(By locator) {
        return find(locator).isEnabled();
    }

    /** Texts of the first column... of every row (td cells) of a table found by xpath of its body rows. */
    protected List<List<String>> tableRows(String tableId) {
        waitForLoading();
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath("//table[@id='" + tableId + "']")));
        return driver.findElements(By.xpath("//table[@id='" + tableId + "']/tbody/tr")).stream()
                .map(row -> row.findElements(By.tagName("td")).stream()
                        .map(cell -> cell.getText().trim())
                        .collect(Collectors.toList()))
                .collect(Collectors.toList());
    }

    // ------------------------------------------------------------------ messages
    /** Text of the global message banner (error / success / info). Waits until a message is actually shown. */
    public String alertText() {
        waitForLoading();
        return wait.until(d -> {
            String text = d.findElement(ALERT).getText().trim();
            return text.isEmpty() ? null : text;
        });
    }

    // ------------------------------------------------------------------ global navigation (TabBar)
    public boolean isLoggedIn() {
        return driver.findElement(TAB_BAR).isDisplayed();
    }

    public String loggedInUserInfo() {
        return text(By.id("TabBar:UserInfo"));
    }

    public DesktopPage openDesktop() {
        click(By.id("TabBar:DesktopTab"));
        return new DesktopPage(driver);
    }

    public NewSubmissionPage openNewSubmission() {
        click(By.id("TabBar:PolicyTab"));
        return new NewSubmissionPage(driver);
    }

    public FnolPage openFnol() {
        click(By.id("TabBar:ClaimTab"));
        return new FnolPage(driver);
    }

    public BillingPage openBilling() {
        click(By.id("TabBar:BillingTab"));
        return new BillingPage(driver);
    }

    public SearchPage openSearch() {
        click(By.id("TabBar:SearchTab"));
        return new SearchPage(driver);
    }

    public LoginPage logout() {
        click(By.id("TabBar:Logout"));
        return new LoginPage(driver);
    }
}
