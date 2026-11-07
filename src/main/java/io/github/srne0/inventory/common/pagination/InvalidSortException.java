package io.github.srne0.inventory.common.pagination;

import java.util.Set;
import java.util.stream.Collectors;

public class InvalidSortException extends RuntimeException {

    public InvalidSortException(String property, Set<String> allowed) {
        super("No se puede ordenar por \"" + property + "\". Campos permitidos: " + allowed.stream().sorted().collect(Collectors.joining(", ")));
    }
}
