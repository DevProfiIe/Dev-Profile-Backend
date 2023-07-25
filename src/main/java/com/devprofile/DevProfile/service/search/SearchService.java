package com.devprofile.DevProfile.service.search;

import com.devprofile.DevProfile.entity.CommitEntity;
import com.devprofile.DevProfile.entity.CommitKeywordsEntity;
import com.devprofile.DevProfile.entity.UserDataEntity;
import com.devprofile.DevProfile.repository.CommitRepository;
import com.devprofile.DevProfile.similaritySearch.Embedding;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class SearchService {

    @Autowired
    private Embedding embedding;
    @Autowired
    private CommitRepository commitRepository;
    @Autowired
    private MongoTemplate mongoTemplate;

    public List<CommitEntity> getTop10SimilarEntity(String query){
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
}
