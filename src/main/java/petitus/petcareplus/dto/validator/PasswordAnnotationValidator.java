package petitus.petcareplus.dto.validator;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.passay.*;
import petitus.petcareplus.dto.annotation.Password;

import java.util.Arrays;

public final class PasswordAnnotationValidator implements ConstraintValidator<Password, String> {
    private static final int MIN_LENGTH = 8;

    private static final int MAX_LENGTH = 32;

    @Override
    public void initialize(Password password) {
    }

    @Override
    public boolean isValid(String passwordField, ConstraintValidatorContext context) {
        if (passwordField == null) {
            return false;
        }

        PasswordValidator validator = new PasswordValidator(Arrays.asList(
                // Length constraints
                new LengthRule(MIN_LENGTH, MAX_LENGTH),

                // At least one uppercase letter
                new CharacterRule(EnglishCharacterData.UpperCase, 1),

                // At least one lowercase letter
                new CharacterRule(EnglishCharacterData.LowerCase, 1),

                // At least one digit
                new CharacterRule(EnglishCharacterData.Digit, 1),

                // At least one special character
                new CharacterRule(EnglishCharacterData.Special, 1),

                // No whitespace allowed
                new WhitespaceRule()
        ));

        RuleResult result = validator.validate(new PasswordData(passwordField));
        if (result.isValid()) {
            return true;
        }

        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate(
                String.join(" ", validator.getMessages(result))
        ).addConstraintViolation();
        return false;
    }
}