package com.carolinarollergirls.scoreboard.utils;

import java.util.Random;

public class HumanIdGenerator {

    public static String generate() {
        // This intended to create a unique ID across ~100 active devices,
        // which is about the most a good WiFi AP can handle.
        // Given the birthday paradox, ~10k unique values are required.
        // This is intended to be more approachable than a UUID and a
        // bit of fun, not cryptographically secure.
        int i1 = rand.nextInt(terms.length);
        int i2 = rand.nextInt(terms.length);
        if (i1 != i2) {
            return terms[i1] + "-" + terms[i2];
        } else {
            return terms[i1] + "-" + overflow[rand.nextInt(overflow.length)];
        }
    }

    // This avoids terms like "bench", "box", or "GTO", as that could be confused with a
    // tablet/laptop in that location or for that person.
    private static String[] terms = {
        "skater", "jammer", "pivot", "blocker", "alternate", "captain",
        "jam", "period", "timeout", "lineup", "team", "review", "start", "stop",
        "seconds", "whistle", "rolling", "stoppage", "clock", "tweet",

        "illegal", "violation", "target", "blocking", "zone", "position",
        "multiplayer", "pass", "penalty", "score", "trip", "point", "initial",
        "interference", "delay", "procedure", "expulsion", "gross", "foulout", "warning", "block",
        "gaining", "report", "return", "impact", "high", "low", "contact", "direction",
        "clockwise", "impenetrable", "pack", "split", "play", "out", "in", "skating",
        "destruction", "bounds", "failure", "yield", "miscounduct", "false",
        "line", "stay", "lead", "lost", "call", "engagement", "complete", "incomplete", "stand", "done",
        "overtime", "reentry", "insubordination", "unsporting", "cut", "swap", "spectrum",

        "head", "back", "shoulder", "knee", "toe", "torso", "finger", "leg", "chin", "thigh",
        "pads", "mouth", "guard", "wrist", "elbow", "forearm", "hand", "shin", "wheel", "truck",
        "star", "stripe", "helmet", "cover", "toestop", "face", "nose", "uniform", "number",

        "bridge", "goat", "wall", "tripod", "recycle", "runback", "lane", "power",

        "short", "flat", "banked", "minor", "major",
    };
    // If there's a duplicate, we take from this list.
    private static String[] overflow = {"ball", "offside", "touchdown", "goalie", "racket", "grass"};

    private static Random rand = new Random();
}
