package com.insurance.gw.parametertypes;

import com.insurance.gw.model.UserRole;
import com.insurance.gw.utils.MoneyUtils;
import io.cucumber.java.ParameterType;

import java.math.BigDecimal;

/**
 * Custom Cucumber parameter types: turn plain text in a step into a typed Java object so step methods
 * receive a BigDecimal / enum instead of raw Strings. The text in the feature stays business-readable.
 *   "$1,234.50" -> {money} -> BigDecimal      "Claims Adjuster" -> {role} -> UserRole.CLAIMS_ADJUSTER
 */
public class ParameterTypes {

    @ParameterType(name = "money", value = "-?\\$[\\d,]+(?:\\.\\d{1,2})?")
    public BigDecimal money(String text) {
        return MoneyUtils.parse(text);
    }

    @ParameterType(name = "role", value = "Producer|Underwriter|Claims Adjuster|Claims Supervisor|Billing Clerk")
    public UserRole role(String text) {
        return UserRole.fromLabel(text);
    }
}
