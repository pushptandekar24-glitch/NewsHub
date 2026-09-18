package com.apihub.news;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * Removes duplicate stories that appear across providers or across pages.
 *
 * Two independent keys, because neither alone is enough:
 *   1. CANONICAL URL — the same article syndicated with different tracking
 *      params (?utm_source=...) is one story.
 *   2. NORMALIZED TITLE + SOURCE — the same publisher often has two URLs for
 *      one story (amp/, /amp, mobile subdomain), and NewsAPI and GNews
 *      frequently return slightly different URLs for identical coverage.
 *
 * The FIRST occurrence wins, so callers should pass the list already sorted
 * with the preferred article first.
 */
@Component
public class ArticleDeduplicator {

    public List<NormalizedArticle> dedupe(List<NormalizedArticle> articles) {
        Set<String> seenUrls = new HashSet<>();
        Set<String> seenTitles = new HashSet<>();
        List<NormalizedArticle> result = new ArrayList<>(articles.size());

        for (NormalizedArticle article : articles) {
            String urlKey = canonicalUrl(article.url());
            String titleKey = normalizeTitle(article.title()) + "|" + safeLower(article.source());

            if (seenUrls.contains(urlKey) || seenTitles.contains(titleKey)) {
                continue;
            }
            seenUrls.add(urlKey);
            seenTitles.add(titleKey);
            result.add(article);
        }
        return result;
    }

    /** Strips scheme, www, query string, trailing slash and amp markers. */
    public String canonicalUrl(String raw) {
        if (raw == null) return "";
        try {
            URI uri = URI.create(raw.trim());
            String host = uri.getHost() == null ? "" : uri.getHost().toLowerCase().replaceFirst("^www\\.", "");
            String path = uri.getPath() == null ? "" : uri.getPath().toLowerCase();

            path = path.replaceAll("/amp/?$", "")
                       .replaceAll("^/amp/", "/")
                       .replaceAll("/+$", "");

            return host + path;
        } catch (IllegalArgumentException ex) {
            return raw.trim().toLowerCase();
        }
    }

    /**
     * Lowercase, strip the publisher suffix ("… - Reuters"), drop punctuation
     * and collapse whitespace, then keep the first 90 chars.
     */
    public String normalizeTitle(String title) {
        if (title == null) return "";
        String cleaned = title.toLowerCase()
                .replaceAll("\\s+[-|–—]\\s+[^-|–—]{2,40}$", "")   // trailing " - Publisher"
                .replaceAll("[^a-z0-9 ]", " ")
                .replaceAll("\\s+", " ")
                .trim();
        return cleaned.length() > 90 ? cleaned.substring(0, 90) : cleaned;
    }

    private static String safeLower(String value) {
        return value == null ? "" : value.toLowerCase();
    }
}
