package com.apihub.news;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * THE single source of truth for "what does category X actually mean?".
 *
 * Every category owns three things:
 *   1. a boolean search query, because provider native categories are far too
 *      coarse for 25 topics;
 *   2. mustMatchAny terms used to filter the results that come back;
 *   3. excludeAny terms used to reject near-miss topics.
 *
 * WHY it lives here and not in the Category entity: the entity is admin-editable
 * display data (name, icon, order). The query strategy is provider-integration
 * logic that needs to be versioned with the code and unit-testable. The seeder
 * copies `query` into the DB column so an admin can still override it, and
 * NewsService falls back to the DB value when a slug is missing here.
 */
@Component
public class CategoryQueryRegistry {

    /** Sports leagues that are emphatically NOT cricket. */
    private static final List<String> NON_CRICKET_SPORTS = List.of(
            "nfl", "mlb", "nba", "nhl", "american football", "baseball",
            "basketball", "quarterback", "touchdown", "home run", "super bowl",
            "world series", "ice hockey", "golf", "tennis", "formula 1", "nascar");

    private final Map<String, CategoryQuery> bySlug = new LinkedHashMap<>();

    public CategoryQueryRegistry() {
        add(new CategoryQuery("world", "general",
                "\"world news\" OR international OR global OR diplomacy OR \"foreign policy\"",
                List.of(), List.of(), false));

        add(new CategoryQuery("india", "general",
                "India OR Indian OR Delhi OR Mumbai OR Bengaluru OR \"Narendra Modi\" OR Lok Sabha",
                List.of("india", "indian", "delhi", "mumbai", "bengaluru", "bangalore",
                        "chennai", "kolkata", "hyderabad", "modi", "rupee", "bharat"),
                List.of(), true));

        add(new CategoryQuery("politics", null,
                "politics OR election OR parliament OR senate OR government OR legislation OR \"prime minister\" OR president",
                List.of("politic", "election", "parliament", "senate", "congress", "government",
                        "minister", "president", "policy", "vote", "campaign", "legislation"),
                List.of(), true));

        add(new CategoryQuery("business", "business",
                "business OR corporate OR earnings OR revenue OR merger OR acquisition OR CEO",
                List.of(), List.of(), false));

        add(new CategoryQuery("finance", "business",
                "\"stock market\" OR stocks OR economy OR inflation OR \"interest rates\" OR "
                + "\"central bank\" OR nasdaq OR nifty OR sensex OR bonds OR currency",
                List.of("stock", "market", "econom", "inflation", "interest rate", "bank",
                        "nasdaq", "nifty", "sensex", "dow jones", "bond", "currency", "trading",
                        "investor", "fed", "gdp", "recession", "earnings"),
                List.of(), true));

        add(new CategoryQuery("technology", "technology",
                "technology OR software OR hardware OR semiconductor OR cloud OR chip OR developer",
                List.of(), List.of(), false));

        add(new CategoryQuery("ai", "technology",
                "\"artificial intelligence\" OR AI OR LLM OR \"machine learning\" OR OpenAI OR "
                + "ChatGPT OR Gemini OR Anthropic OR Claude OR \"neural network\" OR \"generative AI\"",
                List.of("artificial intelligence", " ai ", "ai-", "a.i.", "llm", "machine learning",
                        "openai", "chatgpt", "gemini", "anthropic", "claude", "neural network",
                        "generative", "deep learning", "chatbot", "copilot", "nvidia"),
                List.of(), true));

        add(new CategoryQuery("cybersecurity", "technology",
                "cybersecurity OR hacking OR ransomware OR \"data breach\" OR malware OR "
                + "phishing OR \"zero-day\" OR vulnerability OR hackers",
                List.of("cyber", "hack", "ransomware", "breach", "malware", "phishing",
                        "zero-day", "vulnerabilit", "exploit", "security flaw", "ddos",
                        "encryption", "spyware", "infosec"),
                List.of(), true));

        add(new CategoryQuery("science", "science",
                "science OR research OR study OR discovery OR scientists OR laboratory",
                List.of(), List.of(), false));

        add(new CategoryQuery("space", "science",
                "NASA OR SpaceX OR ISRO OR astronomy OR \"space mission\" OR satellite OR "
                + "rocket OR telescope OR Mars OR orbit",
                List.of("nasa", "spacex", "isro", "astronom", "space", "satellite", "rocket",
                        "telescope", "mars", "lunar", "moon", "orbit", "galaxy", "asteroid",
                        "cosmic", "launch pad", "blue origin"),
                List.of(), true));

        add(new CategoryQuery("health", "health",
                "health OR medicine OR hospital OR disease OR vaccine OR treatment OR doctors",
                List.of(), List.of(), false));

        add(new CategoryQuery("environment", "science",
                "\"climate change\" OR environment OR emissions OR \"global warming\" OR "
                + "renewable OR pollution OR biodiversity OR sustainability",
                List.of("climate", "environment", "emission", "global warming", "renewable",
                        "pollution", "biodiversity", "sustainab", "carbon", "wildfire",
                        "deforestation", "conservation", "solar", "wind power"),
                List.of(), true));

        add(new CategoryQuery("sports", "sports",
                "sports OR match OR tournament OR championship OR league OR athlete",
                List.of(), List.of(), false));

        // ------------------------------------------------------------------
        // CRICKET — the category that was returning MLB/NFL stories.
        //
        // Three defences, all needed:
        //   1. a targeted boolean query instead of the native "sports" category
        //   2. strict mustMatchAny so a story must mention something cricket-specific
        //   3. excludeAny so American sports coverage is rejected outright even if
        //      it happens to contain a word like "innings" or "pitch"
        // ------------------------------------------------------------------
        add(new CategoryQuery("cricket", "sports",
                "cricket OR IPL OR ICC OR BCCI OR \"Test cricket\" OR ODI OR T20 OR "
                + "\"county cricket\" OR \"Ranji Trophy\" OR \"Big Bash\" OR wicket OR batsman",
                List.of("cricket", "ipl ", "indian premier league", " icc ", "bcci", "t20",
                        "odi ", "test match", "test cricket", "wicket", "batsman", "batter",
                        "bowler", "bowling", "all-rounder", "ranji", "big bash", "the ashes",
                        "county championship", "kohli", "rohit sharma", "bumrah", "dhoni",
                        "babar azam", "stokes", "root", "smith", "warner", "crease", "over rate"),
                NON_CRICKET_SPORTS, true));

        add(new CategoryQuery("gaming", "entertainment",
                "gaming OR esports OR PlayStation OR Xbox OR Nintendo OR Steam OR "
                + "\"video game\" OR Twitch OR \"game studio\"",
                List.of("gaming", "esports", "playstation", "xbox", "nintendo", "steam deck",
                        "video game", "videogame", "twitch", "game studio", "ps5", "switch 2",
                        "epic games", "valve", "console", "gamer"),
                List.of(), true));

        add(new CategoryQuery("entertainment", "entertainment",
                "movie OR film OR cinema OR Hollywood OR Bollywood OR Netflix OR "
                + "\"box office\" OR streaming OR series",
                List.of(), List.of(), false));

        add(new CategoryQuery("music", "entertainment",
                "music OR album OR concert OR tour OR Spotify OR Grammy OR singer OR band",
                List.of("music", "album", "concert", "tour", "spotify", "grammy", "singer",
                        "band", "song", "billboard", "rapper", "musician", "record label"),
                List.of(), true));

        add(new CategoryQuery("automotive", null,
                "automotive OR \"electric vehicle\" OR EV OR Tesla OR BYD OR car OR "
                + "automaker OR \"auto industry\" OR hybrid",
                List.of("automotive", "electric vehicle", " ev ", "ev-", "tesla", "byd",
                        " car ", "cars", "automaker", "auto industry", "hybrid", "vehicle",
                        "toyota", "hyundai", "volkswagen", "ford", "charging station"),
                List.of(), true));

        add(new CategoryQuery("travel", null,
                "travel OR tourism OR airline OR flights OR \"travel industry\" OR destination OR hotel",
                List.of("travel", "tourism", "tourist", "airline", "flight", "destination",
                        "hotel", "resort", "passport", "visa", "airport", "cruise"),
                List.of(), true));

        add(new CategoryQuery("education", null,
                "education OR university OR school OR students OR \"higher education\" OR curriculum OR exam",
                List.of("education", "universit", "school", "student", "curriculum", "exam",
                        "college", "campus", "teacher", "tuition", "degree", "academic"),
                List.of(), true));

        add(new CategoryQuery("startups", "business",
                "startup OR \"venture capital\" OR \"funding round\" OR Series A OR "
                + "unicorn OR founder OR accelerator OR seed funding",
                List.of("startup", "venture capital", "funding round", "series a", "series b",
                        "unicorn", "founder", "accelerator", "seed funding", "vc firm",
                        "y combinator", "raised $", "valuation"),
                List.of(), true));

        add(new CategoryQuery("social-media", "technology",
                "\"social media\" OR Instagram OR TikTok OR Facebook OR Meta OR X OR "
                + "Reddit OR YouTube OR WhatsApp OR LinkedIn",
                List.of("social media", "instagram", "tiktok", "facebook", "meta ", "twitter",
                        "reddit", "youtube", "whatsapp", "linkedin", "snapchat", "threads",
                        "influencer", "content moderation"),
                List.of(), true));

        add(new CategoryQuery("law", null,
                "court OR lawsuit OR \"legal ruling\" OR \"supreme court\" OR judge OR "
                + "verdict OR litigation OR prosecutor",
                List.of("court", "lawsuit", "legal", "judge", "verdict", "litigation",
                        "prosecut", "attorney", "justice", "trial", "sue", "ruling",
                        "indict", "plea", "settlement"),
                List.of(), true));

        add(new CategoryQuery("agriculture", null,
                "agriculture OR farming OR farmers OR crops OR harvest OR \"food security\" OR irrigation",
                List.of("agricultur", "farming", "farmer", "crop", "harvest", "food security",
                        "irrigation", "livestock", "wheat", "rice", "soybean", "fertilizer",
                        "monsoon", "agri"),
                List.of(), true));

        add(new CategoryQuery("lifestyle", null,
                "lifestyle OR culture OR fashion OR food OR wellness OR design OR art",
                List.of(), List.of(), false));
    }

    private void add(CategoryQuery query) {
        bySlug.put(query.slug(), query);
    }

    public Optional<CategoryQuery> find(String slug) {
        return slug == null ? Optional.empty() : Optional.ofNullable(bySlug.get(slug.toLowerCase()));
    }

    public Map<String, CategoryQuery> all() {
        return Map.copyOf(bySlug);
    }
}
