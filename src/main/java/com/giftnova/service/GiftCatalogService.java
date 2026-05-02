package com.giftnova.service;

import org.springframework.stereotype.Service;
import java.util.*;

/**
 * In-memory gift catalog — no HRIS or vendor integrations for MVP.
 * 24 gifts across 4 categories spanning $25–$100 price points.
 * The AI uses this list to pick 3 appropriate gifts per event.
 */
@Service
public class GiftCatalogService {

    // Each gift: id, name, category, price (USD), tags for AI context
    private static final List<Map<String, Object>> CATALOG = List.of(
        gift("gift_01", "Amazon Gift Card",           "Remote Friendly", 50,  "versatile, popular, instant delivery"),
        gift("gift_02", "Airbnb Experience Credit",   "Experience",       75,  "adventure, memorable, travel"),
        gift("gift_03", "Kindle Book Credit",          "Learning",         30,  "reading, knowledge, quiet"),
        gift("gift_04", "Udemy Course Credit",         "Learning",         30,  "skills, growth, self-improvement"),
        gift("gift_05", "Coursera Plus (1 month)",     "Learning",         50,  "career, certification, professional"),
        gift("gift_06", "Headspace Premium (3 mo)",    "Wellness",         35,  "mental health, mindfulness, calm"),
        gift("gift_07", "Calm App Subscription",       "Wellness",         35,  "sleep, meditation, stress relief"),
        gift("gift_08", "DoorDash Gift Card",          "Remote Friendly",  40,  "food, convenience, local"),
        gift("gift_09", "Spotify Premium (6 mo)",      "Entertainment",    60,  "music, podcast, daily use"),
        gift("gift_10", "Masterclass Subscription",    "Learning",         60,  "creative, expert-led, inspiring"),
        gift("gift_11", "Nespresso Pod Set",           "Wellness",         40,  "coffee, daily ritual, office"),
        gift("gift_12", "Anker USB-C Hub",             "Tech",             45,  "productivity, remote work, practical"),
        gift("gift_13", "Logitech MX Keys Mini",       "Tech",             100, "typing, ergonomic, professional"),
        gift("gift_14", "Patagonia Gift Card",         "Lifestyle",        75,  "outdoor, sustainable, premium"),
        gift("gift_15", "Yeti Rambler Tumbler",        "Lifestyle",        40,  "daily use, quality, popular"),
        gift("gift_16", "Airbnb Gift Card",            "Experience",       100, "travel, stay, weekend getaway"),
        gift("gift_17", "LinkedIn Learning (3 mo)",    "Learning",         45,  "career, networking, professional growth"),
        gift("gift_18", "Fitbit Inspire 3",            "Wellness",         70,  "fitness, health tracking, active lifestyle"),
        gift("gift_19", "Apple TV+ Subscription",      "Entertainment",    30,  "streaming, family, leisure"),
        gift("gift_20", "Uber Eats Credit",            "Remote Friendly",  50,  "food delivery, flexible, team friendly"),
        gift("gift_21", "Polaroid Now Camera",         "Experience",       80,  "memories, creative, fun"),
        gift("gift_22", "O'Reilly Learning Platform",  "Learning",         50,  "tech, engineering, deep reading"),
        gift("gift_23", "Aromatherapy Gift Set",       "Wellness",         35,  "relaxation, spa, self-care"),
        gift("gift_24", "Amazon Prime (1 year)",       "Remote Friendly",  100, "shipping, streaming, everyday value")
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
