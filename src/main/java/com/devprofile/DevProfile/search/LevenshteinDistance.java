package com.devprofile.DevProfile.search;

import org.springframework.stereotype.Component;

@Component
public class LevenshteinDistance {

    public static int levenshteinDistance(String str1, String str2){
        int[][] distance = new int[str1.length() + 1][str2.length() + 1];

        for (int i = 0; i <= str1.length(); i++) {
            distance[i][0] = i;
        }
        for (int j = 1; j <= str2.length(); j++) {
            distance[0][j] = j;
        }

        for (int i = 1; i <= str1.length(); i++) {
            for (int j = 1; j <= str2.length(); j++) {
                int cost;
                if (str1.charAt(i - 1) == str2.charAt(j - 1)) {
                    cost = 0;
                } else {
                    cost = 1;
                }
                distance[i][j] = Math.min(Math.min(distance[i - 1][j] + 1, distance[i][j - 1] + 1), distance[i - 1][j - 1] + cost);
            }
        }

        return distance[str1.length()][str2.length()];
    }
}
