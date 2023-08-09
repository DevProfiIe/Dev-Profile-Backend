package com.devprofile.DevProfile.service.search;


import com.devprofile.DevProfile.entity.CommitKeywordsEntity;
import com.devprofile.DevProfile.entity.UserDataEntity;
import com.devprofile.DevProfile.entity.WordEntity;
import com.devprofile.DevProfile.repository.CommitKeywordsRepository;
import com.devprofile.DevProfile.repository.UserDataRepository;
import com.devprofile.DevProfile.repository.WordRepository;
import com.devprofile.DevProfile.search.JaccardSimilarity;
import com.devprofile.DevProfile.search.LevenshteinDistance;
import lombok.AllArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

import static java.lang.Double.max;

@Service
@AllArgsConstructor
public class SearchService {

    private CommitKeywordsRepository commitKeywordsRepository;
    private final WordRepository wordRepository;
    private final SparqlService sparqlService;
    private final UserDataRepository userDataRepository;

    public List<String> getTop10JaccardSimilarEntity(String word) {
        Set<String> wordNGrams = JaccardSimilarity.getNGrams(word, 2);
        Map<String, Double> commitSimilarities = new HashMap<>();

        for (UserDataEntity userData : userDataRepository.findAll()) {
            double maxSimilarity = 0;
            for (String keyword : userData.getCs().keySet()) {
                Set<String> keywordNGrams = JaccardSimilarity.getNGrams(keyword, 2);
                double similarity = JaccardSimilarity.jaccardSimilarity(wordNGrams, keywordNGrams);
                maxSimilarity = Math.max(maxSimilarity, similarity);
            }
            commitSimilarities.put(userData.getUserName(), maxSimilarity);
        }

        List<String> sortedCommits = commitSimilarities.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .limit(10)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());

        return sortedCommits;
    }

    //TODO: 소문자로 탐색, userName 붙이기
    public List<Pair<String,String>> getTop10LevenshteinSimilarEntity(String word, String userName) {
        Map<Pair<String, String>, Integer> commitSimilarities = new HashMap<>();
        List<CommitKeywordsEntity> commitKeywordsEntities = commitKeywordsRepository.findByUserName(userName);
        for (CommitKeywordsEntity commit : commitKeywordsEntities) {
            int minSimilarity = 100;
            String mostSimilarKeyword = "";
            if (commit.getCs() != null) {
                for (String keyword : commit.getCs()) {
                    int similarity = LevenshteinDistance.levenshteinDistance(keyword.toLowerCase(),word.toLowerCase());
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


    public List<String> getCloseWords(String inputWord) {
        inputWord = inputWord.toLowerCase();
        char firstChar = inputWord.charAt(0);
        List<WordEntity> candidateWords = wordRepository.findByFirstChar(firstChar);
        Map<String, Double> resultWordList = new HashMap<>();

        for (WordEntity wordEntity : candidateWords) {
            double minSimilarity = 0.80;
            double currentDistance = StringUtils.getJaroWinklerDistance(inputWord, wordEntity.getKeyword().toLowerCase());
            if (minSimilarity <= currentDistance) {
                String redirectWord = sparqlService.findRedirect(wordEntity);
                resultWordList.put(redirectWord, max(resultWordList.getOrDefault(redirectWord,0.8),currentDistance));
            }
        }

        List<String> sortedWords = resultWordList.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .limit(5)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());

        return sortedWords;
    }
}
