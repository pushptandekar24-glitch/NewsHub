package com.apihub.entity;

import jakarta.persistence.*;

/**
 * News categories live in MySQL (not hardcoded in React) so an ADMIN can add,
 * rename or deactivate one without a frontend redeploy.
 *
 * providerTopic maps our category to whatever the upstream API understands —
 * e.g. our "Artificial Intelligence" becomes a keyword search, while
 * "Technology" maps to NewsAPI's native `technology` category.
 */
@Entity
@Table(name = "categories",
       uniqueConstraints = @UniqueConstraint(name = "uk_categories_slug", columnNames = "slug"))
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 80)
    private String name;

    @Column(nullable = false, length = 80)
    private String slug;

    @Column(length = 300)
    private String description;

    /** Emoji or icon name rendered by the frontend chip. */
    @Column(length = 16)
    private String icon;

    /** Native provider category ("technology", "sports", ...) or null. */
    @Column(name = "provider_category", length = 40)
    private String providerCategory;

    /** Fallback keyword query used when the provider has no native category. */
    @Column(name = "provider_query", length = 200)
    private String providerQuery;

    @Column(name = "display_order")
    private Integer displayOrder = 0;

    @Column(nullable = false)
    private boolean active = true;

    protected Category() { }

    public Category(String name, String slug, String icon, String providerCategory,
                    String providerQuery, int displayOrder) {
        this.name = name;
        this.slug = slug;
        this.icon = icon;
        this.providerCategory = providerCategory;
        this.providerQuery = providerQuery;
        this.displayOrder = displayOrder;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getSlug() { return slug; }
    public void setSlug(String slug) { this.slug = slug; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getIcon() { return icon; }
    public void setIcon(String icon) { this.icon = icon; }
    public String getProviderCategory() { return providerCategory; }
    public void setProviderCategory(String providerCategory) { this.providerCategory = providerCategory; }
    public String getProviderQuery() { return providerQuery; }
    public void setProviderQuery(String providerQuery) { this.providerQuery = providerQuery; }
    public Integer getDisplayOrder() { return displayOrder; }
    public void setDisplayOrder(Integer displayOrder) { this.displayOrder = displayOrder; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}
