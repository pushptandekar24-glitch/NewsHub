package com.apihub.news;

import java.util.List;
import java.util.Optional;

/**
 * Supported countries for the Country Explorer.
 *
 * Kept as a code constant rather than a DB table because this list changes only
 * when the upstream provider changes its coverage — it is provider metadata,
 * not user data. Categories, by contrast, ARE in MySQL because admins edit them.
 */
public final class Countries {

    public record Country(String code, String name, String flag) { }

    public static final List<Country> ALL = List.of(
            new Country("world", "Worldwide", "\uD83C\uDF0E"),
            new Country("in", "India", "\uD83C\uDDEE\uD83C\uDDF3"),
            new Country("us", "United States", "\uD83C\uDDFA\uD83C\uDDF8"),
            new Country("gb", "United Kingdom", "\uD83C\uDDEC\uD83C\uDDE7"),
            new Country("ca", "Canada", "\uD83C\uDDE8\uD83C\uDDE6"),
            new Country("au", "Australia", "\uD83C\uDDE6\uD83C\uDDFA"),
            new Country("de", "Germany", "\uD83C\uDDE9\uD83C\uDDEA"),
            new Country("fr", "France", "\uD83C\uDDEB\uD83C\uDDF7"),
            new Country("jp", "Japan", "\uD83C\uDDEF\uD83C\uDDF5"),
            new Country("cn", "China", "\uD83C\uDDE8\uD83C\uDDF3"),
            new Country("br", "Brazil", "\uD83C\uDDE7\uD83C\uDDF7"),
            new Country("it", "Italy", "\uD83C\uDDEE\uD83C\uDDF9"),
            new Country("es", "Spain", "\uD83C\uDDEA\uD83C\uDDF8"),
            new Country("nl", "Netherlands", "\uD83C\uDDF3\uD83C\uDDF1"),
            new Country("se", "Sweden", "\uD83C\uDDF8\uD83C\uDDEA"),
            new Country("ru", "Russia", "\uD83C\uDDF7\uD83C\uDDFA"),
            new Country("za", "South Africa", "\uD83C\uDDFF\uD83C\uDDE6"),
            new Country("sg", "Singapore", "\uD83C\uDDF8\uD83C\uDDEC"),
            new Country("ae", "United Arab Emirates", "\uD83C\uDDE6\uD83C\uDDEA"),
            new Country("kr", "South Korea", "\uD83C\uDDF0\uD83C\uDDF7"),
            new Country("mx", "Mexico", "\uD83C\uDDF2\uD83C\uDDFD"),
            new Country("ar", "Argentina", "\uD83C\uDDE6\uD83C\uDDF7"),
            new Country("ng", "Nigeria", "\uD83C\uDDF3\uD83C\uDDEC"),
            new Country("id", "Indonesia", "\uD83C\uDDEE\uD83C\uDDE9")
    );

    public static Optional<Country> byCode(String code) {
        if (code == null) return Optional.empty();
        return ALL.stream().filter(c -> c.code().equalsIgnoreCase(code)).findFirst();
    }

    public static boolean isSupported(String code) {
        return byCode(code).isPresent();
    }

    private Countries() { }
}
