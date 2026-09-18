package com.apihub.news;

import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Decides whether an article actually belongs to the requested category.
 *
 * This class is the direct fix for the Cricket bug. Provider "sports" results
 * contain MLB, NFL and NBA coverage; a boolean query narrows that but does not
 * eliminate it, because providers match loosely and syndicated round-ups
 * mention many sports at once. So we check the returned text ourselves.
 *
 * Rules, in order:
 *   1. any excludeAny term present  -> reject
 *   2. mustMatchAny non-empty and none present -> reject when strict
 *   3. otherwise accept
 */
@Component
public class ArticleRelevanceFilter {

    public List<NormalizedArticle> filter(List<NormalizedArticle> articles, CategoryQuery category) {
        if (category == null || !category.hasRelevanceRules()) {
            return articles;
        }
        return articles.stream().filter(article -> isRelevant(article, category)).toList();
    }

    public boolean isRelevant(NormalizedArticle article, CategoryQuery category) {
        String text = article.searchableText();

        // Pad so " ai " and " ev " style terms can match at string boundaries.
        String padded = " " + text + " ";

        for (String excluded : category.excludeAny()) {
            if (padded.contains(excluded.toLowerCase())) {
                return false;
            }
        }

        if (!category.hasRelevanceRules()) {
            return true;
        }

        boolean matched = category.mustMatchAny().stream()
                .anyMatch(term -> padded.contains(term.toLowerCase()));

        // Non-strict categories keep everything; the terms are only used for ranking.
        return matched || !category.strict();
    }

    /** How many distinct category terms an article hits — used for ranking. */
    public int relevanceScore(NormalizedArticle article, CategoryQuery category) {
        if (category == null || !category.hasRelevanceRules()) return 0;
        String padded = " " + article.searchableText() + " ";
        return (int) category.mustMatchAny().stream()
                .filter(term -> padded.contains(term.toLowerCase()))
                .count();
    }
}
