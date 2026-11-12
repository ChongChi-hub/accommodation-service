package vn.cozynest.listing.domain.model;

import java.util.List;
import java.util.function.Function;

/**
 * Pure domain page container so domain repositories do not depend on Spring Data.
 */
public record DomainPage<T>(List<T> content, int page, int size, long totalElements) {

    public <U> DomainPage<U> map(Function<T, U> mapper) {
        return new DomainPage<>(content.stream().map(mapper).toList(), page, size, totalElements);
    }
}

