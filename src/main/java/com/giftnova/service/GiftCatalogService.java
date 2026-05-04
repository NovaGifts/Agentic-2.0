package com.giftnova.service;

import org.springframework.stereotype.Service;
import java.util.*;

/**
 * In-memory gift catalog — no HRIS or vendor integrations for MVP.
 * 50 gifts across 6 categories spanning $10–$150 price points.
 * The AI uses this list to pick 3 appropriate gifts per event.
 */
@Service
public class GiftCatalogService {

    // Each gift: id, name, category, price (USD), tags for AI context
    private static final List<Map<String, Object>> CATALOG = List.of(
        // $10–$20 range
        gift("gift_01", "Starbucks Gift Card",           "Remote Friendly", 10,  "coffee, quick, everyday"),
        gift("gift_02", "Google Play Credit",            "Entertainment",   15,  "apps, games, digital"),
        gift("gift_03", "Kindle Book Credit",            "Learning",        15,  "reading, knowledge, quiet"),
        gift("gift_04", "Apple TV+ (1 month)",           "Entertainment",   10,  "streaming, family, leisure"),
        gift("gift_05", "Grubhub Gift Card",             "Remote Friendly", 20,  "food, convenient, flexible"),

        // $25–$35 range
        gift("gift_06", "Audible Credit (1 month)",      "Learning",        25,  "audiobooks, commute, self-improvement"),
        gift("gift_07", "Headspace Premium (1 mo)",      "Wellness",        25,  "mental health, mindfulness, calm"),
        gift("gift_08", "Calm App (1 month)",            "Wellness",        25,  "sleep, meditation, stress relief"),
        gift("gift_09", "Udemy Course Credit",           "Learning",        30,  "skills, growth, online learning"),
        gift("gift_10", "Netflix Gift Card",             "Entertainment",   30,  "streaming, binge, relaxation"),
        gift("gift_11", "DoorDash Gift Card",            "Remote Friendly", 30,  "food delivery, local, casual"),
        gift("gift_12", "Aromatherapy Gift Set",         "Wellness",        35,  "relaxation, spa, self-care"),
        gift("gift_13", "Nespresso Pod Sampler",         "Wellness",        35,  "coffee, daily ritual, office"),

        // $40–$50 range
        gift("gift_14", "Yeti Rambler Tumbler",          "Lifestyle",       40,  "daily use, quality, popular"),
        gift("gift_15", "DoorDash eGift Card",           "Remote Friendly", 40,  "food, convenience, flexible"),
        gift("gift_16", "Anker USB-C Hub",               "Tech",            45,  "productivity, remote work, practical"),
        gift("gift_17", "LinkedIn Learning (1 mo)",      "Learning",        45,  "career, networking, professional growth"),
        gift("gift_18", "Amazon Gift Card",              "Remote Friendly", 50,  "versatile, popular, instant delivery"),
        gift("gift_19", "Coursera Plus (1 month)",       "Learning",        50,  "career, certification, professional"),
        gift("gift_20", "Uber Eats Credit",              "Remote Friendly", 50,  "food delivery, flexible, team friendly"),
        gift("gift_21", "O'Reilly Learning (1 mo)",      "Learning",        50,  "tech, engineering, deep reading"),

        // $55–$70 range
        gift("gift_22", "Spotify Premium (6 mo)",        "Entertainment",   60,  "music, podcast, daily use"),
        gift("gift_23", "Masterclass Subscription",      "Learning",        60,  "creative, expert-led, inspiring"),
        gift("gift_24", "Instacart Gift Card",           "Remote Friendly", 60,  "groceries, convenience, home"),
        gift("gift_25", "Blue Apron Gift Card",          "Lifestyle",       65,  "cooking, home chef, experience"),
        gift("gift_26", "Fitbit Inspire 3",              "Wellness",        70,  "fitness, health tracking, active lifestyle"),
        gift("gift_27", "Kindle Paperwhite",             "Learning",        70,  "reading, portable, long battery"),

        // $75–$100 range
        gift("gift_28", "Airbnb Experience Credit",      "Experience",      75,  "adventure, memorable, travel"),
        gift("gift_29", "Patagonia Gift Card",           "Lifestyle",       75,  "outdoor, sustainable, premium"),
        gift("gift_30", "Nintendo eShop Card",           "Entertainment",   75,  "gaming, casual, family"),
        gift("gift_31", "Polaroid Now Camera",           "Experience",      80,  "memories, creative, fun"),
        gift("gift_32", "Apple Music (6 mo)",            "Entertainment",   80,  "music, ios, family sharing"),
        gift("gift_33", "GoPro Accessories Bundle",      "Experience",      85,  "adventure, outdoor, action"),
        gift("gift_34", "Theragun Mini",                 "Wellness",        90,  "recovery, muscle, active lifestyle"),
        gift("gift_35", "Bose Soundlink Flex Speaker",   "Entertainment",   90,  "portable, music, outdoor"),
        gift("gift_36", "Nike Gift Card",                "Lifestyle",       100, "fitness, apparel, popular"),
        gift("gift_37", "Logitech MX Keys Mini",         "Tech",            100, "typing, ergonomic, professional"),
        gift("gift_38", "Airbnb Gift Card",              "Experience",      100, "travel, stay, weekend getaway"),
        gift("gift_39", "Amazon Prime (1 year)",         "Remote Friendly", 100, "shipping, streaming, everyday value"),
        gift("gift_40", "Peloton App (1 year)",          "Wellness",        100, "fitness, cycling, motivation"),

        // $110–$150 range
        gift("gift_41", "Apple AirPods (3rd gen)",       "Tech",            110, "audio, wireless, commute"),
        gift("gift_42", "Away Luggage Tag Set",          "Lifestyle",       115, "travel, stylish, durable"),
        gift("gift_43", "Vitamix Blender (personal)",    "Lifestyle",       120, "health, kitchen, daily use"),
        gift("gift_44", "DJI Mini Drone",                "Experience",      125, "photography, adventure, outdoor"),
        gift("gift_45", "Dyson Supersonic (rental)",     "Lifestyle",       130, "grooming, premium, popular"),
        gift("gift_46", "Ergotron Laptop Stand",         "Tech",            130, "posture, remote work, ergonomic"),
        gift("gift_47", "Garmin Vivofit 4",              "Wellness",        135, "fitness, steps, battery life"),
        gift("gift_48", "Sony WH-1000XM5 Headphones",   "Tech",            140, "noise cancel, audio, travel"),
        gift("gift_49", "Marriott Gift Card",            "Experience",      150, "hotel, travel, luxury"),
        gift("gift_50", "Lululemon Gift Card",           "Lifestyle",       150, "activewear, premium, popular")
    );

    public List<Map<String, Object>> getAll() { return CATALOG; }

    // Look up a specific gift by ID for displaying on the recommendation page
    public Optional<Map<String, Object>> findById(String id) {
        return CATALOG.stream().filter(g -> id.equals(g.get("id"))).findFirst();
    }

    private static Map<String, Object> gift(String id, String name, String category,
                                             int price, String tags) {
        Map<String, Object> g = new LinkedHashMap<>();
        g.put("id",       id);
        g.put("name",     name);
        g.put("category", category);
        g.put("price",    price);
        g.put("tags",     tags);
        return g;
    }
}
