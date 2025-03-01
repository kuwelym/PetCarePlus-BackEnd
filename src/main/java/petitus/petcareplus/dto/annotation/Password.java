package petitus.petcareplus.dto.annotation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import petitus.petcareplus.dto.validator.PasswordAnnotationValidator;

import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = PasswordAnnotationValidator.class)
@Target({ ElementType.METHOD, ElementType.FIELD, ElementType.ANNOTATION_TYPE, ElementType.PARAMETER })
@Retention(RetentionPolicy.RUNTIME)
public @interface Password {
    String message() default "Invalid password";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}