package com.apihub.service;

import com.apihub.entity.NewsArticle;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * Builds the "Summary / Key Points / Why It Matters" block on the detail page.
 *
 * IMPORTANT, and deliberate: this implementation is DERIVED, not invented. It
 * only restructures metadata the provider already gave us (title, description,
 * source, date). It never asserts facts that are not in the source material,
 * and the response carries summaryGenerated=true so the UI can label it.
 *
 * COPYRIGHT: we do not fetch or store the publisher's full article body. The
 * detail page always links out to the original.
 *
 * If you later plug in a real LLM, implement it behind this same class so the
 * controller and frontend do not change.
 */
@Service
public class ArticleSummaryService {

    public String buildSummary(NewsArticle article) {
        StringBuilder sb = new StringBuilder();

        if (article.getSource() != null) {
            sb.append(article.getSource()).append(" reports: ");
        }
        sb.append(article.getTitle().trim());
        if (!article.getTitle().trim().endsWith(".")) sb.append('.');

        if (article.getDescription() != null && !article.getDescription().isBlank()) {
            sb.append(' ').append(article.getDescription().trim());
        }
        return sb.toString();
    }

    public List<String> buildKeyPoints(NewsArticle article) {
        List<String> points = new ArrayList<>();

        if (article.getSource() != null) {
            points.add("Reported by " + article.getSource()
                    + (article.getAuthor() != null ? " (" + article.getAuthor() + ")" : ""));
        }
        if (article.getCategory() != null) {
            points.add("Filed under " + article.getCategory().getName());
        }
        if (article.getCountry() != null) {
            points.add("Region: " + article.getCountry().toUpperCase());
        }

        // Split the provider snippet into sentences — still the publisher's words,
        // so we keep it short and always show the source link alongside.
        if (article.getDescription() != null) {
            Arrays.stream(article.getDescription().split("(?<=[.!?])\\s+"))
                  .map(String::trim)
                  .filter(s -> s.length() > 30)
                  .limit(3)
                  .forEach(points::add);
        }
        return points;
    }

    public String buildWhyItMatters(NewsArticle article) {
        String topic = article.getCategory() != null
                ? article.getCategory().getName().toLowerCase()
                : "this topic";
        String where = article.getCountry() != null
                ? article.getCountry().toUpperCase()
                : "multiple regions";

        return "This story sits in " + topic + " coverage for " + where
                + ". Open the original report from " + (article.getSource() == null ? "the publisher" : article.getSource())
                + " for the full context and the publisher's own analysis.";
    }
}
