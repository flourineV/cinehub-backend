package com.cinehub.movie.util;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Chuẩn hoá rating TMDb (vd: "PG-13", "R", "PG12", "R15+", "FSK 16", "14A", "NR", ...)
 * về dạng "X+" hoặc "NR".
 *
 * Cách dùng:
 *   String ageRaw = extractAgeRating(releaseDates);   // "R", "PG-13", "PG12", ...
 *   String age    = AgeRatingNormalizer.normalize(ageRaw); // "17+", "13+", "12+", ...
 */
public final class AgeRatingNormalizer {
    private AgeRatingNormalizer() {}

    // Nhận diện "không xếp hạng"
    private static final Set<String> NR_SET = Set.of(
            "NR", "UNRATED", "NOT RATED", "N/A", "NA", "NONE", ""
    );

    // US (MPA)
    private static final Map<String,String> US = mapOf(
            "G","0+", "PG","10+", "PG-13","13+", "R","17+", "NC-17","18+"
    );

    // JP (Eirin): PG12 -> 12+, R15+ -> 15+, R18+ -> 18+
    private static final Map<String,String> JP = mapOf(
            "G","0+", "PG12","12+", "R15+","15+", "R15","15+", "R18+","18+", "R18","18+"
    );

    // DE (FSK): chấp nhận "FSK 16" / "16"
    private static final Map<String,String> DE = mapOf(
            "FSK 0","0+", "0","0+",
            "FSK 6","6+", "6","6+",
            "FSK 12","12+", "12","12+",
            "FSK 16","16+", "16","16+",
            "FSK 18","18+", "18","18+"
    );

    // FR
    private static final Map<String,String> FR = mapOf(
            "U","0+", "TOUS PUBLICS","0+",
            "10","10+", "12","12+", "16","16+", "18","18+"
    );

    // AU
    private static final Map<String,String> AU = mapOf(
            "G","0+", "PG","10+", "M","15+", "MA15+","15+", "R18+","18+", "X18+","18+"
    );

    // CA
    private static final Map<String,String> CA = mapOf(
            "G","0+", "PG","10+", "14A","14+", "18A","18+", "R","18+", "A","18+"
    );

    // BR
    private static final Map<String,String> BR = mapOf(
            "L","0+", "10","10+", "12","12+", "14","14+", "16","16+", "18","18+"
    );

    // KR
    private static final Map<String,String> KR = mapOf(
            "ALL","0+", "12","12+", "15","15+", "18","18+", "19","18+"  // 19 gộp 18+
    );

    // ES
    private static final Map<String,String> ES = mapOf(
            "APTA","0+", "7","7+", "12","12+", "16","16+", "18","18+"
    );

    // IT
    private static final Map<String,String> IT = mapOf(
            "T","0+", "6","6+", "12","12+", "14","14+", "18","18+"
    );

    // IN (CBFC)
    private static final Map<String,String> IN = mapOf(
            "U","0+", "UA","13+", "A","18+", "S","18+"
    );

    // TV (có thể lẫn vào dữ liệu)
    private static final Map<String,String> TV = mapOf(
            "TV-Y","0+", "TV-Y7","7+", "TV-G","0+",
            "TV-PG","10+", "TV-14","14+", "TV-MA","17+"
    );

    // Gom lại để fallback khi không biết region
    private static final List<Map<String,String>> ALL_MAPS = List.of(
            US, JP, DE, FR, AU, CA, BR, KR, ES, IT, IN, TV
    );

    // Bắt số ở bất kỳ đâu trong chuỗi (PG12, R15+, FSK 16, 14A, "12+")
    private static final Pattern ANY_DIGIT = Pattern.compile(".*?(\\d{1,2}).*");

    /**
     * Chuẩn hoá mặc định (không cần truyền region).
     * Ưu tiên map cố định (US/JP/DE/...), nếu không khớp mà có "số" → dùng số, cuối cùng "NR".
     */
    public static String normalize(String raw) {
        String c = norm(raw);
        if (c == null || NR_SET.contains(c)) return "NR";

        // 1) US trước (vì bạn đang ưu tiên US khi extract)
        String hit = US.get(c);
        if (hit != null) return hit;

        // 2) Các map còn lại (JP/DE/FR/AU/CA/BR/KR/ES/IT/IN/TV)
        for (Map<String,String> m : ALL_MAPS) {
            hit = m.get(c);
            if (hit != null) return hit;
        }

        // 3) Nếu có số ở trong chuỗi -> lấy số đó làm "X+"
        Matcher m = ANY_DIGIT.matcher(c);
        if (m.matches()) {
            try {
                int age = Integer.parseInt(m.group(1));
                if (age >= 18) return "18+";
                if (age <= 0)  return "0+";
                return age + "+";
            } catch (NumberFormatException ignored) {}
        }

        // 4) Không nhận diện được -> NR
        return "NR";
    }

    /** Lấy số tuổi tối thiểu từ "X+" (vd: "13+" -> 13). NR -> empty. */
    public static OptionalInt extractMinAge(String normalized) {
        String n = norm(normalized);
        if (n == null || "NR".equals(n)) return OptionalInt.empty();
        Matcher m = ANY_DIGIT.matcher(n);
        if (m.matches()) {
            try {
                int age = Integer.parseInt(m.group(1));
                if (age < 0) return OptionalInt.empty();
                return OptionalInt.of(Math.min(age, 18));
            } catch (NumberFormatException ignored) {}
        }
        return OptionalInt.empty();
    }

    // ===== Helpers =====

    private static String norm(String s) {
        return s == null ? null : s.trim().toUpperCase(Locale.ROOT).replaceAll("\\s+", " ");
    }

    private static Map<String,String> mapOf(String... kv) {
        Map<String,String> m = new HashMap<>();
        for (int i = 0; i + 1 < kv.length; i += 2) {
            String k = norm(kv[i]);
            String v = kv[i + 1];
            m.put(k, v);
        }
        return Collections.unmodifiableMap(m);
    }
}