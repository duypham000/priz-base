package com.priz.base.application.features.files.helper;

import com.priz.base.application.features.files.dto.FileFilterRequest;
import com.priz.base.domain.mysql_priz_base.model.FileModel;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public final class FileSpecification {

    private FileSpecification() {}

    public static Specification<FileModel> withFilters(
            String userId, FileFilterRequest filter) {

        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            predicates.add(cb.equal(root.get("userId"), userId));

            if (filter.getOriginalName() != null && !filter.getOriginalName().isBlank()) {
                predicates.add(cb.like(
                        cb.lower(root.get("originalName")),
                        "%" + filter.getOriginalName().toLowerCase() + "%"
                ));
            }

            if (filter.getFileType() != null && !filter.getFileType().isBlank()) {
                predicates.add(cb.equal(root.get("fileType"), filter.getFileType()));
            }

            if (filter.getContentType() != null && !filter.getContentType().isBlank()) {
                predicates.add(cb.like(root.get("contentType"),
                        "%" + filter.getContentType() + "%"));
            }

            if (filter.getIsSynced() != null) {
                predicates.add(cb.equal(root.get("isSynced"), filter.getIsSynced()));
            }

            if (filter.getMinSize() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("fileSize"),
                        filter.getMinSize()));
            }

            if (filter.getMaxSize() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("fileSize"),
                        filter.getMaxSize()));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
