package com.apihub.dto;

import com.apihub.entity.Category;

public record CategoryResponse(
        Long id,
        String name,
        String slug,
        String description,
        String icon,
        Integer displayOrder
) {
    public static CategoryResponse from(Category c) {
        return new CategoryResponse(c.getId(), c.getName(), c.getSlug(),
                c.getDescription(), c.getIcon(), c.getDisplayOrder());
    }
}
