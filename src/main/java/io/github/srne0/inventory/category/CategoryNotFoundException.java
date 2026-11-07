package io.github.srne0.inventory.category;

public class CategoryNotFoundException extends RuntimeException {

    public CategoryNotFoundException(Long id) {
        super("No existe una categoría con id " + id);
    }
}
