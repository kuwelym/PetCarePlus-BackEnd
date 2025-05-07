package petitus.petcareplus.exceptions;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.security.SignatureException;
import jakarta.annotation.Nonnull;
import jakarta.validation.ConstraintViolationException;
import lombok.RequiredArgsConstructor;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.authentication.InternalAuthenticationServiceException;
import org.springframework.validation.BindException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import petitus.petcareplus.dto.response.DetailedErrorResponse;
import petitus.petcareplus.dto.response.ErrorResponse;
import petitus.petcareplus.service.MessageSourceService;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private final MessageSourceService messageSourceService;

    @Override
    @Nonnull
    protected ResponseEntity<Object> handleHttpRequestMethodNotSupported(
            @Nonnull HttpRequestMethodNotSupportedException ex,
            @Nonnull HttpHeaders headers,
            @Nonnull HttpStatusCode status,
            @Nonnull WebRequest request) {
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body(ErrorResponse.builder()
                .message(messageSourceService.get("method_not_supported"))
                .build());
    }

    // @Override
    // protected ResponseEntity<Object>
    // handleHttpMessageNotReadable(HttpMessageNotReadableException ex,
    // HttpHeaders headers, HttpStatusCode status, WebRequest request) {
    // return
    // ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ErrorResponse.builder()
    // .message(messageSourceService.get("malformed_json_request"))
    // .build());
    // }

    @Override
    @Nonnull
    protected ResponseEntity<Object> handleHttpMessageNotReadable(
            @Nonnull HttpMessageNotReadableException ex,
            @Nonnull HttpHeaders headers,
            @Nonnull HttpStatusCode status,
            @Nonnull WebRequest request) {
        String errorMessage = "Malformed JSON request";
        Throwable cause = ex.getMostSpecificCause();

        if (cause instanceof com.fasterxml.jackson.core.JsonParseException) {
            errorMessage = "Invalid JSON syntax: " + cause.getMessage();
        } else if (cause instanceof com.fasterxml.jackson.databind.exc.InvalidFormatException invalidFormatEx) {
            String fieldName = invalidFormatEx.getPath().stream()
                    .map(ref -> ref.getFieldName())
                    .reduce((first, second) -> second) // lấy field cuối cùng
                    .orElse("unknown");

            if (invalidFormatEx.getTargetType().isEnum()) {
                Object[] acceptedValues = invalidFormatEx.getTargetType().getEnumConstants();
                errorMessage = "Invalid value for '" + fieldName + "'. Accepted values: "
                        + Arrays.toString(acceptedValues);
            } else {
                errorMessage = "Invalid value for '" + fieldName + "'. Expected type: "
                        + invalidFormatEx.getTargetType().getSimpleName();
            }
        } else if (cause instanceof com.fasterxml.jackson.databind.exc.MismatchedInputException mismatchedInputEx) {
            errorMessage = "Invalid or missing value in request body: " + mismatchedInputEx.getOriginalMessage();
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ErrorResponse.builder()
                .message(errorMessage)
                .build());
    }

    @Override
    @Nonnull
    protected ResponseEntity<Object> handleHandlerMethodValidationException(
            @Nonnull HandlerMethodValidationException ex,
            @Nonnull HttpHeaders headers,
            @Nonnull HttpStatusCode status,
            @Nonnull WebRequest request) {
        Map<String, String> errors = new HashMap<>();
        ex.getParameterValidationResults().forEach(validationResult -> {
            validationResult.getResolvableErrors().forEach(error -> {
                errors.put(validationResult.getMethodParameter().getParameterName(), error.getDefaultMessage());
            });
        });

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(DetailedErrorResponse.builder()
                .message(messageSourceService.get("validation_error"))
                .items(errors)
                .build());
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResponseEntity<ErrorResponse> handleMethodArgumentTypeMismatch(MethodArgumentTypeMismatchException ex) {
        Map<String, String> errors = new HashMap<>();
        String paramName = ex.getName();
        String invalidValue = ex.getValue() != null ? ex.getValue().toString() : "null";

        // Handle enum-specific type mismatch
        if (ex.getRequiredType() != null && ex.getRequiredType().isEnum()) {
            String enumName = ex.getRequiredType().getSimpleName();
            String acceptedValues = Arrays.toString(ex.getRequiredType().getEnumConstants());
            String message = String.format("Invalid value '%s' for %s. Accepted values: %s",
                    invalidValue, enumName, acceptedValues);
            errors.put(paramName, message);
        } else {
            // Handle other type mismatches
            String expectedType = ex.getRequiredType() != null ? ex.getRequiredType().getSimpleName() : "unknown";
            String message = String.format("Invalid value '%s' for parameter '%s'. Expected type: %s",
                    invalidValue, paramName, expectedType);
            errors.put(paramName, message);
        }

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(DetailedErrorResponse.builder()
                .message(messageSourceService.get("invalid_parameter"))
                .items(errors)
                .build());
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public final ResponseEntity<ErrorResponse> handleNotFoundException(final ResourceNotFoundException e) {
        return build(HttpStatus.NOT_FOUND, e.getMessage());
    }

    @ExceptionHandler(BindException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public final ResponseEntity<ErrorResponse> handleBindException(final BindException e) {
        Map<String, String> errors = extractErrors(e.getBindingResult());
        return build(HttpStatus.UNPROCESSABLE_ENTITY, messageSourceService.get("validation_error"), errors);
    }

    @ExceptionHandler(DataExistedException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public final ResponseEntity<ErrorResponse> handleDataExistedException(final DataExistedException e) {
        return build(HttpStatus.CONFLICT, e.getMessage());
    }

    @ExceptionHandler({
            BadRequestException.class,
            MultipartException.class,
            IllegalArgumentException.class,
            InvalidDataAccessApiUsageException.class,
            ConstraintViolationException.class,
            MissingRequestHeaderException.class,
            MalformedJwtException.class,
            SignatureException.class,
            BadCredentialsException.class
    })
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public final ResponseEntity<ErrorResponse> handleBadRequestException(final Exception e) {
        return build(HttpStatus.BAD_REQUEST, e.getCause() != null ? e.getCause().getMessage() : e.getMessage());
    }

    @ExceptionHandler({
            UnauthorizedException.class,
            TokenExpireException.class,
            RefreshTokenExpireException.class
    })
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public final ResponseEntity<ErrorResponse> handleUnauthorizedException(final UnauthorizedException e) {
        return build(HttpStatus.UNAUTHORIZED, e.getMessage());
    }

    @ExceptionHandler({
            InternalAuthenticationServiceException.class
    })
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public final ResponseEntity<ErrorResponse> handleAuthenticationCredentialsNotFoundException(
            final Exception e) {
        return build(HttpStatus.UNAUTHORIZED, e.getMessage());
    }

    @ExceptionHandler(AccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public final ResponseEntity<ErrorResponse> handleAccessDeniedException(final Exception e) {
        return build(HttpStatus.FORBIDDEN, messageSourceService.get("access_denied"));
    }

    @ExceptionHandler({
            InsufficientAuthenticationException.class,
            AuthenticationCredentialsNotFoundException.class,
            ExpiredJwtException.class
    })
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public final ResponseEntity<ErrorResponse> handleInsufficientAuthenticationException(final Exception e) {
        return build(HttpStatus.UNAUTHORIZED, messageSourceService.get("insufficient_authentication"));
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public final ResponseEntity<ErrorResponse> handleException(final Exception e) {
        return build(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
    }

    @Nonnull
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex,
            @Nonnull HttpHeaders headers,
            @Nonnull HttpStatusCode status,
            @Nonnull WebRequest request) {
        Map<String, String> errors = extractErrors(ex.getBindingResult());
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(DetailedErrorResponse.builder()
                .message(messageSourceService.get("validation_error"))
                .items(errors)
                .build());
    }

    private Map<String, String> extractErrors(BindingResult bindingResult) {
        Map<String, String> errors = new HashMap<>();
        bindingResult.getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String message = error.getDefaultMessage();
            errors.put(fieldName, message);
        });
        return errors;
    }

    private ResponseEntity<ErrorResponse> build(final HttpStatus httpStatus,
            final String message,
            final Map<String, String> errors) {
        if (!errors.isEmpty()) {
            return ResponseEntity.status(httpStatus).body(DetailedErrorResponse.builder()
                    .message(message)
                    .items(errors)
                    .build());
        }

        return ResponseEntity.status(httpStatus).body(ErrorResponse.builder()
                .message(message)
                .build());
    }

    private ResponseEntity<ErrorResponse> build(final HttpStatus httpStatus, final String message) {
        return build(httpStatus, message, new HashMap<>());
    }

}