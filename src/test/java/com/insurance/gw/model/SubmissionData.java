package com.insurance.gw.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.LocalDate;
import java.util.Map;

/**
 * Test-data object (a.k.a. data model / POJO) describing a Personal Auto submission.
 * Public fields keep the Jackson mapping trivial; sensible defaults mean a scenario only states what matters to it.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class SubmissionData {

    public String firstName = "John";
    public String lastName = "Doe";
    public int driverAge = 35;
    public String state = "OH";
    public int violations = 0;
    public int vehicleYear = 2020;
    public String vehicleMake = "Toyota";
    public String vehicleModel = "Corolla";
    public String liability = "100/300";
    public String collision = "500";
    public boolean comprehensive = false;
    public String term = "12 Months";
    public String paymentPlan = "Full Pay";

    @JsonIgnore
    public LocalDate effectiveDate = LocalDate.now();

    /** Birthday was 10 days before the effective date, so the driver's age on that date is exactly driverAge. */
    public LocalDate dateOfBirth() {
        return effectiveDate.minusYears(driverAge).minusDays(10);
    }

    public String fullName() {
        return firstName + " " + lastName;
    }

    /** Applies the rows of a Cucumber key/value DataTable (keys are matched ignoring case and spaces). */
    public SubmissionData apply(Map<String, String> row) {
        row.forEach((rawKey, value) -> {
            switch (rawKey.replace(" ", "").toLowerCase()) {
                case "firstname": firstName = value; break;
                case "lastname": lastName = value; break;
                case "driverage": driverAge = Integer.parseInt(value); break;
                case "state": state = value; break;
                case "violations": violations = Integer.parseInt(value); break;
                case "vehicleyear": vehicleYear = Integer.parseInt(value); break;
                case "vehiclemake": vehicleMake = value; break;
                case "vehiclemodel": vehicleModel = value; break;
                case "liability": liability = value; break;
                case "collisiondeductible": collision = value; break;
                case "comprehensive": comprehensive = value.equalsIgnoreCase("Yes") || value.equalsIgnoreCase("true"); break;
                case "term": term = value; break;
                case "paymentplan": paymentPlan = value; break;
                default: throw new IllegalArgumentException("Unknown submission field: " + rawKey);
            }
        });
        return this;
    }

    public int termMonths() {
        return Integer.parseInt(term.split(" ")[0]);
    }
}
