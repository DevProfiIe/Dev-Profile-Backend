package com.devprofile.DevProfile.service.userData;


import com.devprofile.DevProfile.entity.*;
import com.devprofile.DevProfile.repository.*;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import org.apache.catalina.User;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.LocalDate;
import java.util.*;

@Service
@AllArgsConstructor
public class UserStyleService {

    private final CommitRepository commitRepository;

    private final UserRepository userRepository;

    private final GitRepository gitRepository;

    private final UserDataRepository userDataRepository;

    private final PatchRepository patchRepository;

    private final MongoTemplate mongoTemplate;

    private final WebClient webClient;

    private final String gitHubApiUrl = "https://api.github.com/";


    public void addKeywordsToUser(String userName, List<String> styles){
        Query query = new Query(Criteria.where("userName").is(userName));
        Update update = new Update();
        styles.forEach(style -> {if(style != null) update.addToSet("style", style);});
        mongoTemplate.updateFirst(query,update,UserDataEntity.class);
    }

    public void generateAllStyles(String searchUserName){
        List<UserDataEntity> userDataEntities = userDataRepository.findAll();
        for(UserDataEntity userDataEntity : userDataEntities) {
            List<String> styles = new ArrayList<>();

            styles.add(generateContinuous(userDataEntity));
            styles.add(generateExplain(userDataEntity));
            styles.add(generateReadMe(userDataEntity, searchUserName));

            addKeywordsToUser(userDataEntity.getUserName(), styles);
        }
    }

    public String generateReadMe(UserDataEntity userDataEntity, String searchUserName){
        String gitHubToken = userRepository.findByLogin(searchUserName).getGitHubToken();
        String userName = userDataEntity.getUserName();
        Integer userId = userRepository.findByLogin(userName).getId();

        List<RepositoryEntity> repositoryEntities = gitRepository.findByUserId(userId);
        int readmeCount =0;
        int repoCount =0;

        for(RepositoryEntity repositoryEntity : repositoryEntities){
            String owner =repositoryEntity.getOrgName();
            if(owner != null) owner = userName;

            JsonNode readmeResponse = webClient.get()
                    .uri(gitHubApiUrl +"repos/"+owner+ "/"+ repositoryEntity.getRepoName() + "/readme")
                    .header("Authorization", "Bearer " + gitHubToken)
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .block();
            repoCount++;
            if(readmeResponse == null) continue;
            String content = readmeResponse.get("content").asText();
            String[] lines = content.split("\n");
            if(lines.length > 50) readmeCount++;
        }
        if(repoCount*7 < readmeCount*10) return "\"리드미\" 리드미";
        return null;
    }

    public String generateContinuous(UserDataEntity userDataEntity){

        String userName = userDataEntity.getUserName();
        List<CommitEntity> commitEntities = commitRepository.findByUserName(userName);
        Map<LocalDate, Boolean> visited = new HashMap<>();
        Integer commitDays = 0;
        for(CommitEntity commit : commitEntities){
            LocalDate commitDate = commit.getCommitDate();
            if(commitDate.isAfter(LocalDate.now().minusMonths(6))){
                if(!visited.getOrDefault(commitDate, false)){
                    commitDays++;
                    visited.put(commitDate, true);
                }
            }
        }
        if(commitDays >= 144) return "지속적인 개발자";
        return null;
    }


    //TODO: 주석이 사이에 있는 경우를 커버해 주어야한다. + 깃커밋메세지 평가
    public String generateExplain(UserDataEntity userDataEntity){
        String userName = userDataEntity.getUserName();
        List<PatchEntity> patchEntities = getPatchEntities(userName);
        Long countCommentLines = 0L;
        Long countLines = 0L;
        for(PatchEntity patchEntity : patchEntities){
            Pair<Long, Long> lines = countComments(patchEntity.getPatch());
            countCommentLines += lines.getFirst();
            countLines += lines.getSecond();
        }
        if(2 * countLines > 10* countCommentLines) return "설명충";
        return null;
    }

    public List<PatchEntity> getPatchEntities(String userName){
        List<CommitEntity> commitEntities = commitRepository.findByUserName(userName);
        List<PatchEntity> patchEntities = new ArrayList<>();
        commitEntities.forEach(commitEntity -> {patchEntities.addAll(patchRepository.findByCommitOid(commitEntity.getCommitOid()));});
        return patchEntities;
    }

    public Pair<Long, Long> countComments(String patch) {
        Long count = 0L;
        Long allCount = 0L;
        String[] lines = patch.split("\n");
        boolean multiLineComment = false;
        for (String line : lines) {
            line = line.trim();
            if (multiLineComment || line.startsWith("/*")) {
                count++;
                multiLineComment = !line.endsWith("*/");
            } else if (line.startsWith("//")) {
                count++;
            }
            allCount++;
        }
        return Pair.of(count, allCount);
    }
}
