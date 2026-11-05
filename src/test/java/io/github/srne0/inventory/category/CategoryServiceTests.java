package io.github.srne0.inventory.category;

import io.github.srne0.inventory.category.dto.CategoryRequest;
import io.github.srne0.inventory.category.dto.CategoryResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.NoSuchElementException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTests {

    @Mock
    private CategoryRepository repository;

    private CategoryService service() {
        return new CategoryService(repository);
    }

    @Test
    @DisplayName("create recorta los espacios del nombre y la descripción")
    void createTrimsFields() {
        when(repository.save(any(Category.class))).thenAnswer(i -> i.getArgument(0));

        service().create(new CategoryRequest("  Bebidas ", " Líquidos  "));

        ArgumentCaptor<Category> captor = ArgumentCaptor.forClass(Category.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getName()).isEqualTo("Bebidas");
        assertThat(captor.getValue().getDescription()).isEqualTo("Líquidos");
    }

    @Test
    @DisplayName("create deja la descripción en null si no viene")
    void createKeepsNullDescription() {
        when(repository.save(any(Category.class))).thenAnswer(i -> i.getArgument(0));

        CategoryResponse response = service().create(new CategoryRequest("Snacks", null));

        assertThat(response.description()).isNull();
    }

    @Test
    @DisplayName("update recorta los espacios")
    void updateTrimsFields() {
        Category existing = new Category("Viejo", null);
        when(repository.findById(1L)).thenReturn(Optional.of(existing));

        service().update(1L, new CategoryRequest(" Nuevo ", null));

        assertThat(existing.getName()).isEqualTo("Nuevo");
    }

    @Test
    @DisplayName("get de un id inexistente lanza NoSuchElementException")
    void getMissingThrows() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service().get(99L)).isInstanceOf(NoSuchElementException.class);
    }
}
