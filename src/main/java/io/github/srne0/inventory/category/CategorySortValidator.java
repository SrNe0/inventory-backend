package io.github.srne0.inventory.category;

import io.github.srne0.inventory.common.pagination.InvalidSortException;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class CategorySortValidator {

    private static final Set<String> ALLOWED = Set.of("id", "name", "createdAt", "updatedAt");

    public void validate(Pageable pageable) {
        for (Sort.Order order : pageable.getSort()) {
            String property = order.getProperty();
            if (!ALLOWED.contains(property)) {
                throw new InvalidSortException(property, ALLOWED);
            }
        }
    }
}
