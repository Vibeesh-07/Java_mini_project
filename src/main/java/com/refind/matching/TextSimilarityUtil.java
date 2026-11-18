package com.refind.matching;

import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class TextSimilarityUtil {

    private static final Set<String> STOP_WORDS = Set.of(
            "a", "an", "the", "and", "or", "but", "in", "on", "at", "to", "for", "with",
            "is", "was", "are", "were", "it", "its", "my", "of", "by", "from", "as", "into",
            "like", "through", "after", "over", "between", "out", "about", "have", "has", "had",
            "lost", "found", "item", "belonging", "someone", "please", "help"
    );

    private static final Pattern WORD_SPLIT = Pattern.compile("[^a-zA-Z0-9]+");

    public static Set<String> extractKeywords(String text) {
        if (text == null || text.isBlank()) {
            return Collections.emptySet();
        }
        return Arrays.stream(WORD_SPLIT.split(text.toLowerCase(Locale.ROOT)))
                .filter(w -> w.length() >= 2 && !STOP_WORDS.contains(w))
                .collect(Collectors.toSet());
    }

    public static int calculateTextSimilarityScore(
            String title1, String desc1,
            String title2, String desc2,
            List<String> explanations
    ) {
        Set<String> titleWords1 = extractKeywords(title1);
        Set<String> titleWords2 = extractKeywords(title2);

        Set<String> descWords1 = extractKeywords(desc1);
        Set<String> descWords2 = extractKeywords(desc2);

        // Combined words
        Set<String> allWords1 = new HashSet<>(titleWords1);
        allWords1.addAll(descWords1);

        Set<String> allWords2 = new HashSet<>(titleWords2);
        allWords2.addAll(descWords2);

        if (allWords1.isEmpty() || allWords2.isEmpty()) {
            return 0;
        }

        // Title overlap is strongly weighted (up to 15 points)
        int titlePoints = 0;
        Set<String> commonTitleWords = new HashSet<>(titleWords1);
        commonTitleWords.retainAll(titleWords2);

        if (!commonTitleWords.isEmpty()) {
            double jaccardTitle = (double) commonTitleWords.size() / Math.max(1, (titleWords1.size() + titleWords2.size() - commonTitleWords.size()));
            if (jaccardTitle >= 0.75) {
                titlePoints = 15;
            } else if (jaccardTitle >= 0.45) {
                titlePoints = 12;
            } else if (!commonTitleWords.isEmpty()) {
                titlePoints = Math.min(10, 5 + commonTitleWords.size() * 2);
            }
            explanations.add("Matching title keywords [" + String.join(", ", commonTitleWords) + "] (+" + titlePoints + " pts)");
        }

        // Description and cross-keyword overlap (up to 10 points)
        int descPoints = 0;
        Set<String> commonAllWords = new HashSet<>(allWords1);
        commonAllWords.retainAll(allWords2);

        // Words present in both but not already credited in title
        Set<String> otherCommonWords = new HashSet<>(commonAllWords);
        otherCommonWords.removeAll(commonTitleWords);

        if (!otherCommonWords.isEmpty()) {
            descPoints = Math.min(10, otherCommonWords.size() * 2);
            explanations.add("Additional keyword overlap [" + String.join(", ", otherCommonWords) + "] (+" + descPoints + " pts)");
        }

        int total = Math.min(25, titlePoints + descPoints);
        return total;
    }
}
