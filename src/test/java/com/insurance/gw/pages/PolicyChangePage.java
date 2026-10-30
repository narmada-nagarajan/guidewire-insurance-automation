package com.insurance.gw.pages;

import com.insurance.gw.utils.DateUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

import java.time.LocalDate;

/** PolicyCenter > Change Policy (mid-term endorsement: add a vehicle). */
public class PolicyChangePage extends BasePage {

    private static final By TITLE = By.id("PolicyChange:Title");
    private static final By EFFECTIVE_DATE = By.id("PolicyChange:effectiveDate");
    private static final By VEHICLE_YEAR = By.id("PolicyChange:vehicleYear");
    private static final By VEHICLE_MAKE = By.id("PolicyChange:vehicleMake");
    private static final By VEHICLE_MODEL = By.id("PolicyChange:vehicleModel");
    private static final By SUBMIT = By.id("PolicyChange:Submit");

    public PolicyChangePage(WebDriver driver) {
        super(driver);
        waitForPage(TITLE);
    }

    public void addVehicle(LocalDate changeEffective, int year, String make, String model) {
        type(EFFECTIVE_DATE, DateUtils.format(changeEffective));
        type(VEHICLE_YEAR, String.valueOf(year));
        selectByText(VEHICLE_MAKE, make);
        type(VEHICLE_MODEL, model);
        click(SUBMIT);
    }
}
