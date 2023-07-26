package com.devprofile.DevProfile.search;

import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Set;



@Component
public class JaccardSimilarity {



    public static Set<String> getNGrams(String word, int n) {
        Set<String> nGrams = new HashSet<>();
        for (int i = 0; i < word.length() - n + 1; i++)
            nGrams.add(word.substring(i, i + n));
        return nGrams;
    }

    public static double jaccardSimilarity(Set<String> a, Set<String> b) {
        Set<String> intersection = new HashSet<>(a);
        intersection.retainAll(b);
        Set<String> union = new HashSet<>(a);
        union.addAll(b);
        return (double) intersection.size() / union.size();
    }
}
