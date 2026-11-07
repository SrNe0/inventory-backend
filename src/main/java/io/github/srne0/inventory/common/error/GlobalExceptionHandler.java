package io.github.srne0.inventory.common.error;

import io.github.srne0.inventory.category.CategoryNotFoundException;
import io.github.srne0.inventory.category.DuplicateCategoryNameException;
import io.github.srne0.inventory.common.pagination.InvalidSortException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    public record FieldError(String field, String message) {
    }

    @ExceptionHandler(CategoryNotFoundException.class)
    public ProblemDetail handleCategoryNotFound(CategoryNotFoundException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(InvalidSortException.class)
    public ProblemDetail handleInvalidSort(InvalidSortException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(DuplicateCategoryNameException.class)
    public ProblemDetail handleDuplicateCategoryName(DuplicateCategoryNameException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
    }

    // Red de seguridad (p. ej. carrera sobre el índice único). Cubre cualquier
    // restricción de la BD (única, NOT NULL, longitud), no solo duplicados: la
    // causa real se registra aquí y al cliente solo le llega un detalle genérico.
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ProblemDetail handleDataIntegrityViolation(DataIntegrityViolationException ex) {
        log.warn("Violación de integridad en la base de datos", ex);
        return ProblemDetail.forStatusAndDetail(
                HttpStatus.CONFLICT,
                "La operación viola una restricción de integridad"
        );
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request
    ) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, "La petición tiene campos inválidos");
        List<FieldError> errors = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> new FieldError(error.getField(), error.getDefaultMessage()))
                .toList();
        problem.setProperty("errors", errors);
        return createResponseEntity(problem, headers, status, request);
    }
}
