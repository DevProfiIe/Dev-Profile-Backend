package com.devprofile.DevProfile.service;

import com.devprofile.DevProfile.dto.response.analyze.CommitKeywordsDTO;
import com.devprofile.DevProfile.dto.response.analyze.RepositoryEntityDTO;
import com.devprofile.DevProfile.entity.CommitEntity;
import com.devprofile.DevProfile.entity.LanguageDuration;
import com.devprofile.DevProfile.entity.RepoFrameworkEntity;
import com.devprofile.DevProfile.entity.RepositoryEntity;
import com.devprofile.DevProfile.repository.CommitKeywordsRepository;
import com.devprofile.DevProfile.repository.CommitRepository;
import com.devprofile.DevProfile.repository.GitRepository;
import com.devprofile.DevProfile.repository.RepoFrameworkRepository;
import org.springframework.stereotype.Service;

import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class RepositoryService {
    private final RepoFrameworkRepository repoFrameworkRepository;
    private final GitRepository gitRepository;
    private final CommitRepository commitRepository;
    private final CommitKeywordsRepository commitKeywordsRepository;
    private final FrameworkService frameworkService;


    public RepositoryService(RepoFrameworkRepository repoFrameworkRepository, CommitRepository commitRepository, CommitKeywordsRepository commitKeywordsRepository, FrameworkService frameworkService,GitRepository gitRepository) {
        this.repoFrameworkRepository = repoFrameworkRepository;
        this.commitRepository = commitRepository;
        this.commitKeywordsRepository = commitKeywordsRepository;
        this.frameworkService = frameworkService;
        this.gitRepository = gitRepository;
    }

    public List<RepositoryEntityDTO> createRepositoryEntityDTOs(List<RepositoryEntity> repositoryEntities, List<CommitEntity> commitEntities, Map<String, CommitKeywordsDTO> oidAndKeywordsMap) {
        List<RepositoryEntityDTO> extendedEntities = new ArrayList<>();

        if (commitEntities == null || repositoryEntities == null || oidAndKeywordsMap == null) {
            return extendedEntities;
        }

        for (RepositoryEntity repositoryEntity : repositoryEntities) {
            RepositoryEntityDTO dto = new RepositoryEntityDTO();

            dto.setId(repositoryEntity.getId());
            dto.setRepoName(repositoryEntity.getRepoName());
            dto.setStartDate(repositoryEntity.getStartDate());
            dto.setEndDate(repositoryEntity.getEndDate());
            dto.setTotalCommitCnt(repositoryEntity.getTotalCommitCnt());
            dto.setMyCommitCnt(repositoryEntity.getMyCommitCnt());
            dto.setTotalContributors(repositoryEntity.getTotalContributors());
            dto.setLanguageDurations(repositoryEntity.getLanguageDurations());
            dto.setRepoDesc(repositoryEntity.getRepoDesc());
            List<String> repoLanguages = repositoryEntity.getLanguageDurations().stream()
                    .map(LanguageDuration::getLanguage)
                    .collect(Collectors.toList());

            dto.setRepoLanguages(repoLanguages);

            List<RepoFrameworkEntity> repoFrameworks = repoFrameworkRepository.findAllByRepoName(repositoryEntity.getRepoName());
            List<String> frameworkNames = repoFrameworks.stream().map(RepoFrameworkEntity::getFramework).collect(Collectors.toList());
            Map<String, String> frameworkUrls = frameworkService.getFrameworkUrls(frameworkNames);
            dto.setFrameworkUrls(frameworkUrls);

            extendedEntities.add(dto);
        }
        return extendedEntities;
    }

    public void saveFrameworksToNewTable(List<CommitEntity> commitEntities, Map<String, CommitKeywordsDTO> oidAndKeywordsMap) {
        List<String> existingFrameworks = frameworkService.getAllFrameworkNames();
        Map<String, CommitEntity> oidAndCommitMap = commitEntities.stream()
                .collect(Collectors.toMap(CommitEntity::getCommitOid, Function.identity()));

        for (Map.Entry<String, CommitKeywordsDTO> entry : oidAndKeywordsMap.entrySet()) {
            CommitEntity commitEntity = oidAndCommitMap.get(entry.getKey());

            if (commitEntity != null) {
                Set<String> langFrameworks = entry.getValue().getLangFramework();

                if (langFrameworks != null) {
                    for (String langFramework : langFrameworks) {
                        if (existingFrameworks.contains(langFramework)) {
                            if (!repoFrameworkRepository.existsByRepoNameAndFramework(commitEntity.getRepoName(), langFramework)) {
                                RepoFrameworkEntity repoFrameworkEntity = new RepoFrameworkEntity();
                                repoFrameworkEntity.setRepoName(commitEntity.getRepoName());
                                repoFrameworkEntity.setFramework(langFramework);

                                Optional<RepositoryEntity> optionalRepositoryEntity = gitRepository.findByRepoName(commitEntity.getRepoName());

                                if (optionalRepositoryEntity.isPresent()) {
                                    RepositoryEntity repositoryEntity = optionalRepositoryEntity.get();
                                    Long duration = ChronoUnit.DAYS.between(repositoryEntity.getStartDate(), repositoryEntity.getEndDate());
                                    repoFrameworkEntity.setRepoDuration(duration);

                                    Long repositoryId = repositoryEntity.getId();
                                    repoFrameworkEntity.setRepoId(repositoryId);


                                }

                                repoFrameworkRepository.save(repoFrameworkEntity);
                            }
                        }
                    }
                }
            }
        }
    }
}
