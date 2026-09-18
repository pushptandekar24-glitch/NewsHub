package com.apihub.config;

import com.apihub.entity.Category;
import com.apihub.news.CategoryQuery;
import com.apihub.news.CategoryQueryRegistry;
import com.apihub.repository.CategoryRepository;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Seeds the 25 news categories on first startup.
 *
 * DISPLAY data (name, icon, order) is defined here.
 * QUERY data (provider category, boolean query) comes from CategoryQueryRegistry,
 * so there is exactly ONE place where "what does Cricket mean?" is answered.
 *
 * Seeding is idempotent by slug, so an admin's later edits survive a restart.
 * Existing rows do get their query columns refreshed, because those are
 * code-owned and an out-of-date query is the thing that broke Cricket.
 */
@Component
@Order(1)
public class CategorySeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(CategorySeeder.class);

    /** slug, display name, icon, display order. */
    private record Display(String slug, String name, String icon, int order) { }

    private static final List<Display> CATEGORIES = List.of(
            new Display("world",         "World",                   "\uD83C\uDF0E", 1),
            new Display("india",         "India",                   "\uD83C\uDDEE\uD83C\uDDF3", 2),
            new Display("politics",      "Politics",                "\uD83C\uDFDB\uFE0F", 3),
            new Display("business",      "Business",                "\uD83D\uDCBC", 4),
            new Display("finance",       "Finance & Markets",       "\uD83D\uDCB0", 5),
            new Display("technology",    "Technology",              "\uD83D\uDCBB", 6),
            new Display("ai",            "Artificial Intelligence", "\uD83E\uDD16", 7),
            new Display("cybersecurity", "Cybersecurity",           "\uD83D\uDD10", 8),
            new Display("science",       "Science",                 "\uD83D\uDD2C", 9),
            new Display("space",         "Space & Astronomy",       "\uD83D\uDE80", 10),
            new Display("health",        "Health & Medicine",       "\uD83C\uDFE5", 11),
            new Display("environment",   "Environment & Climate",   "\uD83C\uDF31", 12),
            new Display("sports",        "Sports",                  "\u26BD",       13),
            new Display("cricket",       "Cricket",                 "\uD83C\uDFCF", 14),
            new Display("gaming",        "Gaming & Esports",        "\uD83C\uDFAE", 15),
            new Display("entertainment", "Movies & Entertainment",  "\uD83C\uDFAC", 16),
            new Display("music",         "Music",                   "\uD83C\uDFB5", 17),
            new Display("automotive",    "Automotive & EVs",        "\uD83D\uDE97", 18),
            new Display("travel",        "Travel & Tourism",        "\u2708\uFE0F", 19),
            new Display("education",     "Education",               "\uD83C\uDF93", 20),
            new Display("startups",      "Startups & Innovation",   "\uD83C\uDF1F", 21),
            new Display("social-media",  "Social Media & Internet", "\uD83D\uDCF1", 22),
            new Display("law",           "Law & Justice",           "\u2696\uFE0F", 23),
            new Display("agriculture",   "Agriculture",             "\uD83C\uDF3E", 24),
            new Display("lifestyle",     "Lifestyle & Culture",     "\uD83C\uDFE0", 25)
    );

    private final CategoryRepository repository;
    private final CategoryQueryRegistry queryRegistry;

    public CategorySeeder(CategoryRepository repository, CategoryQueryRegistry queryRegistry) {
        this.repository = repository;
        this.queryRegistry = queryRegistry;
    }

    @Override
    public void run(String... args) {
        int created = 0;
        int refreshed = 0;

        for (Display display : CATEGORIES) {
            CategoryQuery query = queryRegistry.find(display.slug()).orElse(null);
            String providerCategory = query == null ? null : query.providerCategory();
            String providerQuery = query == null ? display.name() : query.query();

            Category existing = repository.findBySlugIgnoreCase(display.slug()).orElse(null);

            if (existing == null) {
                repository.save(new Category(display.name(), display.slug(), display.icon(),
                        providerCategory, providerQuery, display.order()));
                created++;
            } else {
                // Refresh code-owned query columns only; leave name/icon/active alone
                // so admin edits are preserved.
                existing.setProviderCategory(providerCategory);
                existing.setProviderQuery(providerQuery);
                repository.save(existing);
                refreshed++;
            }
        }

        log.info("Categories seeded: {} created, {} query definitions refreshed", created, refreshed);
    }
}
