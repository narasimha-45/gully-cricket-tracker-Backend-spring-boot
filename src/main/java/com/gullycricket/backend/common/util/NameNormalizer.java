package com.gullycricket.backend.common.util;

/**
 * Single source of truth for turning a raw, user/JSON-supplied name (player name,
 * team name, etc.) into the canonical form we store and look names up by.
 * <p>
 * Applied consistently at every write path — live match creation
 * ({@code MatchService#saveMatch}) and the Mongo→Postgres migration (which itself
 * routes through {@code MatchService#saveMatch}) — plus defensively again in
 * {@code PlayerService} and {@code TeamService} so any other future write path gets
 * the same guarantee for free. Normalizing twice is a no-op, so it's always safe to
 * call this again before a lookup or save.
 * <p>
 * Rules: trim leading/trailing whitespace, collapse any internal run of whitespace
 * to a single space, then lowercase. "  Virat   Kohli " and "virat kohli" and
 * "VIRAT KOHLI" all normalize to "virat kohli", so they resolve to the same
 * {@code Player}/{@code Team} row instead of silently creating duplicates.
 */
public final class NameNormalizer {

    private NameNormalizer() {
    }

    public static String normalize(String rawName) {
        if (rawName == null) {
            return null;
        }
        String collapsed = rawName.trim().replaceAll("\\s+", " ");
        return collapsed.toLowerCase();
    }
}
