package io.github.srne0.inventory.category;

import io.github.srne0.inventory.category.dto.CategoryRequest;
import io.github.srne0.inventory.category.dto.CategoryResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class CategoryService {

    private final CategoryRepository repository;

    public CategoryService(CategoryRepository repository) {
        this.repository = repository;
    }

    public CategoryResponse create(CategoryRequest request) {
        String name = trim(request.name());
        if (repository.existsByNameIgnoreCase(name)) {
            throw new DuplicateCategoryNameException(name);
        }
        Category category = new Category(name, trim(request.description()));
        // Si dos peticiones pasan la comprobación a la vez, el índice único
        // lower(name) rechaza la segunda y el advice lo traduce a 409.
        return CategoryResponse.from(repository.save(category));
    }

    @Transactional(readOnly = true)
    public CategoryResponse get(Long id) {
        return CategoryResponse.from(find(id));
    }

    public CategoryResponse update(Long id, CategoryRequest request) {
        Category category = find(id);
        String name = trim(request.name());
        if (repository.existsByNameIgnoreCaseAndIdNot(name, id)) {
            throw new DuplicateCategoryNameException(name);
        }
        category.setName(name);
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

    private Category find(Long id) {
        return repository.findById(id).orElseThrow(() -> new CategoryNotFoundException(id));
    }

    private static String trim(String value) {
        return value == null ? null : value.trim();
    }
}
