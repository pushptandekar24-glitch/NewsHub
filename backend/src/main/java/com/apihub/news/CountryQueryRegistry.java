package com.apihub.news;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * Country-name/demonym/capital keyword sets, used ONLY as a query boost for
 * providers whose search endpoint has no native `country` parameter.
 *
 * WHY THIS EXISTS (this is the fix for the "Netherlands returns nothing" bug):
 * NewsAPI's /everything endpoint — which is what actually serves every
 * category or keyword request — has no `country` field in its API at all.
 * The old code silently dropped the country whenever a category or search
 * term was present, which is nearly always. India appeared to "work" only by
 * coincidence: the India category's own boolean query already contains
 * "India OR Indian OR Delhi OR Mumbai...", so it looked country-scoped even
 * though the country parameter itself was never sent. Every other country had
 * no such luck.
 *
 * This registry gives NewsApiOrgProvider a real, if approximate, way to bias
 * /everything toward a country: AND the query with (Country OR Demonym OR
 * Capital). GNewsProvider does NOT need this — its /search and /top-headlines
 * endpoints both accept a genuine `country` parameter and are queried directly.
 *
 * DELIBERATELY NOT used as a post-fetch relevance filter (unlike
 * CategoryQueryRegistry's mustMatchAny): a legitimate Dutch story ("Ajax sign
 * new striker") often will not contain the word "Netherlands" or "Dutch" in
 * its headline, so filtering on this list after the fact would throw away
 * good results. It only ever narrows what NewsAPI is *asked* for.
 */
@Component
public class CountryQueryRegistry {

    private final Map<String, String> boostByCode = new LinkedHashMap<>();

    public CountryQueryRegistry() {
        add("in", "India OR Indian OR Delhi OR Mumbai OR Bengaluru");
        add("us", "\"United States\" OR American OR Washington OR \"New York\"");
        add("gb", "\"United Kingdom\" OR Britain OR British OR London OR UK");
        add("ca", "Canada OR Canadian OR Ottawa OR Toronto");
        add("au", "Australia OR Australian OR Sydney OR Canberra");
        add("de", "Germany OR German OR Berlin");
        add("fr", "France OR French OR Paris");
        add("jp", "Japan OR Japanese OR Tokyo");
        add("cn", "China OR Chinese OR Beijing OR Shanghai");
        add("br", "Brazil OR Brazilian OR \"Rio de Janeiro\" OR \"Sao Paulo\"");
        add("it", "Italy OR Italian OR Rome OR Milan");
        add("es", "Spain OR Spanish OR Madrid OR Barcelona");
        add("nl", "Netherlands OR Dutch OR Amsterdam OR \"The Hague\"");
        add("se", "Sweden OR Swedish OR Stockholm");
        add("ru", "Russia OR Russian OR Moscow");
        add("za", "\"South Africa\" OR \"South African\" OR Johannesburg");
        add("sg", "Singapore OR Singaporean");
        add("ae", "\"United Arab Emirates\" OR UAE OR Dubai OR \"Abu Dhabi\"");
        add("kr", "\"South Korea\" OR Korean OR Seoul");
        add("mx", "Mexico OR Mexican");
        add("ar", "Argentina OR Argentine OR \"Buenos Aires\"");
        add("ng", "Nigeria OR Nigerian OR Lagos OR Abuja");
        add("id", "Indonesia OR Indonesian OR Jakarta");
    }

    private void add(String code, String boost) {
        boostByCode.put(code, boost);
    }

    /** The boolean query fragment for a country code, if we have one. */
    public Optional<String> queryBoost(String countryCode) {
        if (countryCode == null) return Optional.empty();
        return Optional.ofNullable(boostByCode.get(countryCode.toLowerCase()));
    }

    public boolean hasBoost(String countryCode) {
        return queryBoost(countryCode).isPresent();
    }
}
