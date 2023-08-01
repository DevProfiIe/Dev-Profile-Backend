package com.devprofile.DevProfile.service;

import com.devprofile.DevProfile.dto.response.analyze.CommitKeywordsDTO;
import com.devprofile.DevProfile.dto.response.analyze.RepositoryEntityDTO;
import com.devprofile.DevProfile.entity.CommitEntity;
import com.devprofile.DevProfile.entity.RepoFrameworkEntity;
import com.devprofile.DevProfile.entity.RepositoryEntity;
import com.devprofile.DevProfile.repository.CommitKeywordsRepository;
import com.devprofile.DevProfile.repository.CommitRepository;
import com.devprofile.DevProfile.repository.RepoFrameworkRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class RepositoryService {
    private final RepoFrameworkRepository repoFrameworkRepository;
    private final CommitRepository commitRepository;
    private final CommitKeywordsRepository commitKeywordsRepository;
    private final FrameworkService frameworkService;

    public RepositoryService(RepoFrameworkRepository repoFrameworkRepository, CommitRepository commitRepository, CommitKeywordsRepository commitKeywordsRepository, FrameworkService frameworkService) {
        this.repoFrameworkRepository = repoFrameworkRepository;
        this.commitRepository = commitRepository;
        this.commitKeywordsRepository = commitKeywordsRepository;
        this.frameworkService = frameworkService;
    }

    public List<RepositoryEntityDTO> createRepositoryEntityDTOs(List<RepositoryEntity> repositoryEntities, List<CommitEntity> commitEntities, Map<String, CommitKeywordsDTO> oidAndKeywordsMap) {
        List<RepositoryEntityDTO> extendedEntities = new ArrayList<>();

        if (commitEntities == null || repositoryEntities == null || oidAndKeywordsMap == null) {
            return extendedEntities;
        }

        Map<String, CommitEntity> oidAndCommitMap = commitEntities.stream()
                .collect(Collectors.toMap(CommitEntity::getCommitOid, Function.identity()));

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

            for (Map.Entry<String, CommitKeywordsDTO> entry : oidAndKeywordsMap.entrySet()) {
                CommitEntity commitEntity = oidAndCommitMap.get(entry.getKey());

                if (commitEntity != null && commitEntity.getRepoName().equals(repositoryEntity.getRepoName())) {
                    dto.setFeatured(entry.getValue().getFeatured() != null ? new ArrayList<>(entry.getValue().getFeatured()) : new ArrayList<>());
                    dto.setLangFramework(entry.getValue().getLangFramework() != null ? new ArrayList<>(entry.getValue().getLangFramework()) : new ArrayList<>());
                    break;
                }
            }

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
                            RepoFrameworkEntity repoFrameworkEntity = new RepoFrameworkEntity();
                            repoFrameworkEntity.setRepoName(commitEntity.getRepoName());
                            repoFrameworkEntity.setFramework(langFramework);
                            repoFrameworkRepository.save(repoFrameworkEntity);
                        }
                    }
                }
            }
        }
    }
}
