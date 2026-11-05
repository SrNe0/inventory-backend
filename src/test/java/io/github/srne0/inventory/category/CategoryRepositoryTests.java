package io.github.srne0.inventory.category;

import io.github.srne0.inventory.TestcontainersConfiguration;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// replace = NONE: sin esto, @DataJpaTest sustituiría el datasource por una H2 en memoria.
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TestcontainersConfiguration.class)
class CategoryRepositoryTests {

    @Autowired
    private CategoryRepository repository;

    @Autowired
    private EntityManager entityManager;

    @Test
    @DisplayName("guardar asigna id y fechas, y se puede buscar por id")
    void saveAssignsIdAndTimestamps() {
        Category saved = repository.saveAndFlush(new Category("Bebidas", "Líquidos"));

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isEqualTo(saved.getCreatedAt());
        assertThat(repository.findById(saved.getId()))
                .hasValueSatisfying(c -> assertThat(c.getName()).isEqualTo("Bebidas"));
    }

    @Test
    @DisplayName("la descripción es opcional")
    void descriptionIsOptional() {
        Category saved = repository.saveAndFlush(new Category("Snacks", null));

        assertThat(saved.getDescription()).isNull();
    }

    @Test
    @DisplayName("el nombre es único sin distinguir mayúsculas")
    void nameIsUniqueIgnoringCase() {
        repository.saveAndFlush(new Category("Bebidas", null));

        assertThatThrownBy(() -> repository.saveAndFlush(new Category("bebidas", null)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("actualizar cambia updatedAt y conserva createdAt")
    void updateKeepsCreatedAt() {
        Category saved = repository.saveAndFlush(new Category("Lácteos", null));
        entityManager.clear();

        Category loaded = repository.findById(saved.getId()).orElseThrow();
        loaded.setName("Lácteos y huevos");
        repository.saveAndFlush(loaded);
        entityManager.clear();

        Category reloaded = repository.findById(saved.getId()).orElseThrow();
        assertThat(reloaded.getName()).isEqualTo("Lácteos y huevos");
        assertThat(reloaded.getCreatedAt()).isEqualTo(saved.getCreatedAt());
        assertThat(reloaded.getUpdatedAt()).isAfterOrEqualTo(reloaded.getCreatedAt());
    }
}
