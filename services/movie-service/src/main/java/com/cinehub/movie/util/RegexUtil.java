package com.cinehub.movie.util;

public final class RegexUtil {

    private RegexUtil() {
        // utility class, no instance
    }

    public static String escape(String input) {
        if (input == null)
            return null;

        return input.replaceAll("([\\\\+*?\\[\\]^$(){}=!<>|:\\-])", "\\\\$1");
    }
}
