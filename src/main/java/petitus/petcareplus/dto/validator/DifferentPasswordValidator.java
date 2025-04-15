package petitus.petcareplus.dto.validator;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.apache.commons.beanutils.BeanUtils;
import petitus.petcareplus.dto.annotation.DifferentPassword;

public class DifferentPasswordValidator implements ConstraintValidator<DifferentPassword, Object> {

    private String currentPasswordField;
    private String newPasswordField;
    private String message;

    @Override
    public void initialize(DifferentPassword constraintAnnotation) {
        this.currentPasswordField = constraintAnnotation.currentPassword();
        this.newPasswordField = constraintAnnotation.newPassword();
        this.message = constraintAnnotation.message();
    }

    @Override
    public boolean isValid(Object value, ConstraintValidatorContext context) {
        try {
            final Object firstObj = BeanUtils.getProperty(value, currentPasswordField);
            final Object secondObj = BeanUtils.getProperty(value, newPasswordField);

            boolean isValid = !firstObj.equals(secondObj);

            if (!isValid) {
                context.disableDefaultConstraintViolation();
                context.buildConstraintViolationWithTemplate(message)
                        .addPropertyNode(newPasswordField)
                        .addConstraintViolation();
            }

            return isValid;
        } catch (Exception e) {
            return false;
        }
    }
} 