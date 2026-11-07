package io.github.srne0.inventory.category;

public class DuplicateCategoryNameException extends RuntimeException {

    public DuplicateCategoryNameException(String name) {
        super("Ya existe una categoría con el nombre \"" + name + "\"");
    }
}
