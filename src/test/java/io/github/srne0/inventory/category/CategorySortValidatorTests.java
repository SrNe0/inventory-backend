package io.github.srne0.inventory.category;

import io.github.srne0.inventory.common.pagination.InvalidSortException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CategorySortValidatorTests {

    private final CategorySortValidator validator = new CategorySortValidator();

    @Test
    @DisplayName("acepta los campos permitidos, en cualquier dirección")
    void acceptsAllowedFields() {
        assertThatCode(() -> validator.validate(PageRequest.of(0, 10,
                Sort.by(Sort.Order.asc("name"), Sort.Order.desc("createdAt"),
                        Sort.Order.asc("updatedAt"), Sort.Order.asc("id")))))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("acepta una petición sin orden")
    void acceptsUnsorted() {
        assertThatCode(() -> validator.validate(PageRequest.of(0, 10))).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("rechaza un campo que no está permitido y lista los válidos en orden")
    void rejectsUnknownField() {
        assertThatThrownBy(() -> validator.validate(PageRequest.of(0, 10, Sort.by("password"))))
                .isInstanceOf(InvalidSortException.class)
                .hasMessage("No se puede ordenar por \"password\". Campos permitidos: createdAt, id, name, updatedAt");
    }

    @Test
    @DisplayName("valida todos los órdenes, no solo el primero")
    void validatesEveryOrder() {
        assertThatThrownBy(() -> validator.validate(PageRequest.of(0, 10, Sort.by("name", "otroCampo"))))
                .isInstanceOf(InvalidSortException.class)
                .hasMessageContaining("otroCampo");
    }

    @Test
    @DisplayName("distingue mayúsculas: createdat no es createdAt")
    void isCaseSensitive() {
        assertThatThrownBy(() -> validator.validate(PageRequest.of(0, 10, Sort.by("createdat"))))
                .isInstanceOf(InvalidSortException.class);
    }
}
