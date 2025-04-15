package petitus.petcareplus.dto.annotation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import petitus.petcareplus.dto.validator.DifferentPasswordValidator;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Constraint(validatedBy = DifferentPasswordValidator.class)
@Target({ ElementType.TYPE, ElementType.ANNOTATION_TYPE })
@Retention(RetentionPolicy.RUNTIME)
public @interface DifferentPassword {
    String message() default "{new_password_must_be_different}";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
    String currentPassword();
    String newPassword();

    @Target({ ElementType.TYPE, ElementType.ANNOTATION_TYPE })
    @Retention(RetentionPolicy.RUNTIME)
    @interface List {
        DifferentPassword[] value();
    }
} 