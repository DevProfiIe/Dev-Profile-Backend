package com.devprofile.DevProfile.service.search;


import com.devprofile.DevProfile.entity.CommitKeywordsEntity;

import com.devprofile.DevProfile.repository.CommitKeywordsRepository;
import com.devprofile.DevProfile.repository.CommitRepository;
import com.devprofile.DevProfile.search.JaccardSimilarity;
import com.devprofile.DevProfile.search.LevenshteinDistance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class SearchService {


    @Autowired
    private CommitRepository commitRepository;
    @Autowired
    private MongoTemplate mongoTemplate;
    @Autowired
    private CommitKeywordsRepository commitKeywordsRepository;

    public List<String> getTop10JaccardSimilarEntity(String word) {
        Set<String> wordNGrams = JaccardSimilarity.getNGrams(word, 2);
        Map<String, Double> commitSimilarities = new HashMap<>();

        for (CommitKeywordsEntity commit : commitKeywordsRepository.findAll()) {
            double maxSimilarity = 0;
            for (String keyword : commit.getCs()) {
                Set<String> keywordNGrams = JaccardSimilarity.getNGrams(keyword, 2);
                double similarity = JaccardSimilarity.jaccardSimilarity(wordNGrams, keywordNGrams);
                maxSimilarity = Math.max(maxSimilarity, similarity);
            }
            commitSimilarities.put(commit.getOid(), maxSimilarity);
        }

        List<String> sortedCommits = new ArrayList<>(commitSimilarities.keySet());
        sortedCommits.sort(Comparator.comparingDouble(commitSimilarities::get).reversed());

        return sortedCommits.size() > 10 ? sortedCommits.subList(0, 10) : sortedCommits;
    }

    public List<Pair<String,String>> getTop10LevenshteinSimilarEntity(String word) {
        Map<Pair<String, String>, Integer> commitSimilarities = new HashMap<>();

        for (CommitKeywordsEntity commit : commitKeywordsRepository.findAll()) {
            int minSimilarity = 100;
            String mostSimilarKeyword = "";
            Map<String, String> commitKeyword = new HashMap<>();

            if (commit.getCs() != null) {
                for (String keyword : commit.getCs()) {
                    int similarity = LevenshteinDistance.levenshteinDistance(keyword,word);
                    if(similarity < minSimilarity){
                        mostSimilarKeyword = keyword;
                        minSimilarity = similarity;
                    }
                }
            }
            if (commit.getLangFramework() != null) {
                for (String keyword : commit.getLangFramework()) {
                    int similarity = LevenshteinDistance.levenshteinDistance(keyword,word);
                    if(similarity < minSimilarity){
                        mostSimilarKeyword = keyword;
                        minSimilarity = similarity;
                    }
                }
            }

            commitSimilarities.put(Pair.of(commit.getOid(), mostSimilarKeyword), minSimilarity);
        }

        List<Pair<String, String>> sortedCommits = new ArrayList<>(commitSimilarities.keySet());
        sortedCommits.sort(Comparator.comparingInt(commitSimilarities::get));

        return sortedCommits.size() > 10 ? sortedCommits.subList(0, 10) : sortedCommits;
    }
}
