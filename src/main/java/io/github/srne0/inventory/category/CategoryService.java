package io.github.srne0.inventory.category;

import io.github.srne0.inventory.category.dto.CategoryRequest;
import io.github.srne0.inventory.category.dto.CategoryResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.NoSuchElementException;

@Service
@Transactional
public class CategoryService {

    private final CategoryRepository repository;

    public CategoryService(CategoryRepository repository) {
        this.repository = repository;
    }

    public CategoryResponse create(CategoryRequest request) {
        Category category = new Category(trim(request.name()), trim(request.description()));
        return CategoryResponse.from(repository.save(category));
    }

    @Transactional(readOnly = true)
    public CategoryResponse get(Long id) {
        return CategoryResponse.from(find(id));
    }

    public CategoryResponse update(Long id, CategoryRequest request) {
        Category category = find(id);
        category.setName(trim(request.name()));
        category.setDescription(trim(request.description()));
        // Sin save(): la entidad está gestionada y Hibernate detecta el cambio.
        // El flush adelanta el UPDATE para que updatedAt (@PreUpdate) ya esté
        // asignado al construir la respuesta.
        repository.flush();
        return CategoryResponse.from(category);
    }

    public void delete(Long id) {
        repository.delete(find(id));
    }

    // Provisional: en el #16 pasa a una excepción de dominio que se traduce a 404.
    private Category find(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Categoría no encontrada: " + id));
    }

    private static String trim(String value) {
        return value == null ? null : value.trim();
    }
}
