package com.devprofile.DevProfile.service.search;

import com.devprofile.DevProfile.entity.CommitEntity;
import com.devprofile.DevProfile.entity.CommitKeywordsEntity;
import com.devprofile.DevProfile.entity.UserDataEntity;
import com.devprofile.DevProfile.repository.CommitKeywordsRepository;
import com.devprofile.DevProfile.repository.CommitRepository;
import com.devprofile.DevProfile.search.JaccardSimilarity;
import com.devprofile.DevProfile.search.LevenshteinDistance;
import com.devprofile.DevProfile.similaritySearch.Embedding;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class SearchService {

    @Autowired
    private Embedding embedding;
    @Autowired
    private CommitRepository commitRepository;
    @Autowired
    private MongoTemplate mongoTemplate;
    @Autowired
    private CommitKeywordsRepository commitKeywordsRepository;

    public List<CommitEntity> getTop10CosineSimilarEntity(String query){
        List<CommitKeywordsEntity> commitKeywordsEntityList = mongoTemplate.find(new Query(), CommitKeywordsEntity.class);
        List<CommitEntity> allCommits = commitRepository.findAll();


        double[] vector = embedding.getVector(query);

        List<Pair<Double, CommitEntity>> similarities = allCommits.stream()
                .map(entity -> Pair.of(embedding.cosineSimilarity(embedding.getVector(entity.getCommitMessage()),vector), entity))
                .collect(Collectors.toList());

        // Sort by cosine similarity in descending order and pick the top 10
        similarities.sort((p1, p2) -> Double.compare(p2.getFirst(), p1.getFirst()));
        return similarities.stream()
                .limit(10)
                .map(Pair::getSecond)
                .collect(Collectors.toList());
    }

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

    public List<String> getTop10LevenshteinSimilarEntity(String word) {
        Map<String, Integer> commitSimilarities = new HashMap<>();

        for (CommitKeywordsEntity commit : commitKeywordsRepository.findAll()) {
            int minSimilarity = 100;
            for (String keyword : commit.getCs()) {
                int similarity = LevenshteinDistance.levenshteinDistance(keyword,word);
                minSimilarity = Math.min(minSimilarity, similarity);
            }
            for (String keyword : commit.getLangFramework()) {
                int similarity = LevenshteinDistance.levenshteinDistance(keyword,word);
                minSimilarity = Math.min(minSimilarity, similarity);
            }
            commitSimilarities.put(commit.getOid(), minSimilarity);
        }

        List<String> sortedCommits = new ArrayList<>(commitSimilarities.keySet());
        sortedCommits.sort(Comparator.comparingInt(commitSimilarities::get));

        return sortedCommits.size() > 10 ? sortedCommits.subList(0, 10) : sortedCommits;
    }
}
