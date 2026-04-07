package fmcg.distribution.util;

import org.springframework.stereotype.Component;
import java.util.regex.Pattern;

@Component
public class PasswordValidator {

    private static final Pattern UPPERCASE_PATTERN = Pattern.compile("[A-Z]");
    private static final Pattern LOWERCASE_PATTERN = Pattern.compile("[a-z]");
    private static final Pattern DIGIT_PATTERN = Pattern.compile("\\d");

    public boolean isValid(String password) {
        if (password == null || password.length() < 8) {
            return false;
        }
        if (!UPPERCASE_PATTERN.matcher(password).find()) {
            return false;
        }
        if (!LOWERCASE_PATTERN.matcher(password).find()) {
            return false;
        }
        if (!DIGIT_PATTERN.matcher(password).find()) {
            return false;
        }
        return true;
    }

    public String getValidationMessage() {
        return "Password must be at least 8 characters and contain uppercase, lowercase, and number";
    }
}