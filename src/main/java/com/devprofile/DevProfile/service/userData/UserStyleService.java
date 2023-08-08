package com.devprofile.DevProfile.service.userData;


import com.devprofile.DevProfile.entity.*;
import com.devprofile.DevProfile.repository.*;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

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

            styles.add(generateContinuous(userDataEntity));             //지속적인 개발자
            styles.add(generateExplain(userDataEntity));                //설명충
            styles.add(generateReadMe(userDataEntity, searchUserName)); //"리드미" 리드미
            styles.add(generateDayCommits(userDataEntity));             //1일 1커밋
            styles.add(generateNoPointCommits(userDataEntity));         //실속없는 커밋자
            styles.add(generateSoloMultiplayer(userDataEntity));        //싱글 멀티 플레이어
            styles.add(generateCommitPattern(userDataEntity));          //새벽형 개발자, 아침형 개발자
            styles.add(generateRefactor(userDataEntity));               //Refactory
            styles.add(generateInfluence(userDataEntity));              //인기 개발자, 영향력 있는 개발자
            styles.add(generateWeekendCommitter(userDataEntity));       //주말 커밋 전문가, 주말 커밋자

            addKeywordsToUser(userDataEntity.getUserName(), styles);
        }
    }

    public String generateCommitPattern(UserDataEntity userDataEntity) {
        String userName = userDataEntity.getUserName();
        String gitHubToken = userRepository.findByLogin(userName).getGitHubToken();
        Integer userId = userRepository.findByLogin(userName).getId();
        List<RepositoryEntity> repositoryEntities = gitRepository.findByUserId(userId);

        int earlyMorningCommits = 0;
        int lateNightCommits = 0;

        for (RepositoryEntity repositoryEntity : repositoryEntities) {
            String owner = repositoryEntity.getOrgName();
            if (owner == null) owner = userName;

            JsonNode commitsResponse = webClient.get()
                    .uri(gitHubApiUrl + "repos/" + owner + "/" + repositoryEntity.getRepoName() + "/commits")
                    .header("Authorization", "Bearer " + gitHubToken)
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .block();

            if (commitsResponse != null) {
                for (JsonNode commitNode : commitsResponse) {
                    String commitTime = commitNode.get("commit").get("author").get("date").asText();
                    LocalDateTime commitDateTime = LocalDateTime.parse(commitTime, DateTimeFormatter.ISO_OFFSET_DATE_TIME);
                    int hour = commitDateTime.getHour();

                    if (hour >= 0 && hour < 6) {
                        lateNightCommits++;
                    } else if (hour >= 6 && hour < 10) {
                        earlyMorningCommits++;
                    }
                }
            }
        }
        if (lateNightCommits > earlyMorningCommits && lateNightCommits > 30) {
            return "새벽형 개발자";
        } else if (earlyMorningCommits > lateNightCommits && earlyMorningCommits > 30) {
            return "아침형 개발자";
        }
        return null;
    }

    public String generateInfluence(UserDataEntity userDataEntity) {
        String userName = userDataEntity.getUserName();
        Integer userId = userRepository.findByLogin(userName).getId();
        String gitHubToken = userRepository.findByLogin(userName).getGitHubToken();
        List<RepositoryEntity> repositoryEntities = gitRepository.findByUserId(userId);

        int totalStars = 0;
        int totalForks = 0;

        for (RepositoryEntity repositoryEntity : repositoryEntities) {
            String owner = repositoryEntity.getOrgName();
            if (owner == null) owner = userName;

            JsonNode repoResponse = webClient.get()
                    .uri(gitHubApiUrl + "repos/" + owner + "/" + repositoryEntity.getRepoName())
                    .header("Authorization", "Bearer " + gitHubToken)
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .block();

            if (repoResponse != null) {
                totalStars += repoResponse.get("stargazers_count").asInt();
                totalForks += repoResponse.get("forks_count").asInt();
            }
        }

        if (totalStars > 50 || totalForks > 20) {
            return "영향력 있는 제작자";
        } else if (totalStars > 20 || totalForks > 10) {
            return "인기 개발자";
        }

        return null;
    }


    public String generateWeekendCommitter(UserDataEntity userDataEntity) {
        String userName = userDataEntity.getUserName();
        List<CommitEntity> commitEntities = commitRepository.findByUserName(userName);
        int weekendCommits = 0;

        for (CommitEntity commit : commitEntities) {
            LocalDate commitDate = commit.getCommitDate();
            DayOfWeek dayOfWeek = commitDate.getDayOfWeek();

            if (dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY) {
                weekendCommits++;
            }
        }

        if (weekendCommits > 50) {
            return "주말 커밋 전문가";
        }else if(weekendCommits >= 10){
            return "주말 커밋자";
        }

        return null;
    }

    public String generateRefactor(UserDataEntity userDataEntity){
        String userName = userDataEntity.getUserName();
        List<CommitEntity> commits= commitRepository.findByUserName(userName);
        Integer inserted = 0;
        Integer deleted = 0;
        for(CommitEntity commit : commits){
            List<PatchEntity> patches = patchRepository.findByCommitOid(commit.getCommitOid());
            for(PatchEntity patch: patches){
                String patchContent = patch.getPatch();
                List<String> patchLines = Stream.of(patchContent.split("\n")).toList();
                for(String line : patchLines) {
                    if (line.startsWith("+")) {
                        inserted++;
                    } else if (line.startsWith("-")) {
                        deleted++;
                    }
                }
            }
        }
        if(inserted*0.5 < deleted) return "ReFactory";
        return null;

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

    public String generateSoloMultiplayer(UserDataEntity userDataEntity){
        String userName = userDataEntity.getUserName();
        UserEntity user = userRepository.findByLogin(userName);
        List<RepositoryEntity> repositories = gitRepository.findByUserId(user.getId());
        int multiCount = 0;
        int singleCount = 0;
        for(RepositoryEntity repo : repositories){
            if(repo.getTotalContributors() > 1){
                if(repo.getMyCommitCnt() > 10){
                    multiCount++;
                }
            }else{
                if(repo.getMyCommitCnt() > 10){
                    singleCount++;
                }
            }
        }
        if(singleCount > multiCount+2) {
            return "싱글 플레이어";
        }else{
            return "멀티 플레이어";
        }
    }

    public String generateDayCommits(UserDataEntity userDataEntity) {
        String userName = userDataEntity.getUserName();
        List<CommitEntity> commitEntities = commitRepository.findByUserName(userName);
        Map<LocalDate, Boolean> visited = new HashMap<>();
        Integer commitDays = 0;

        LocalDate startDate = LocalDate.now().minusDays(30);

        for (CommitEntity commit : commitEntities) {
            LocalDate commitDate = commit.getCommitDate();
            if (commitDate.isAfter(startDate) && commitDate.isBefore(LocalDate.now()) || commitDate.equals(LocalDate.now())) {
                if (!visited.getOrDefault(commitDate, false)) {
                    commitDays++;
                    visited.put(commitDate, true);
                }
            }
        }

        if (commitDays >= 14) return "1일 1커밋";
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

    public String generateNoPointCommits(UserDataEntity userDataEntity) {
        String userName = userDataEntity.getUserName();
        List<CommitEntity> commitEntities = commitRepository.findByUserName(userName);
        int totalCommits = commitEntities.size();
        int emptyCommits = 0;

        for (CommitEntity commit : commitEntities) {
            int length = commit.getLength();
            if (length <= 5) {
                emptyCommits++;
            }
        }

        if (totalCommits > 0 && ((float) emptyCommits / totalCommits) >= 0.10) {
            return "실속없는 커밋자";
        }

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

