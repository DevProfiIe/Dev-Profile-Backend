package com.devprofile.DevProfile.controller;


import com.devprofile.DevProfile.dto.response.ApiResponse;
import com.devprofile.DevProfile.dto.response.analyze.SearchResultDTO;
import com.devprofile.DevProfile.entity.CommitEntity;
import com.devprofile.DevProfile.entity.FileTreeNode;
import com.devprofile.DevProfile.repository.CommitRepository;
import com.devprofile.DevProfile.service.patch.PatchService;
import com.devprofile.DevProfile.service.search.SearchService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.util.Pair;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


@Controller
public class SearchController {

    @Autowired
    CommitRepository commitRepository;

    @Autowired
    SearchService searchService;


    @Autowired
    private final PatchService patchService;

    public SearchController(PatchService patchService) {
        this.patchService = patchService;
    }

    @GetMapping("/search")
    public ResponseEntity<ApiResponse> searchCommit(@RequestParam String query) {
        query = "%" + query + "%";
        List<CommitEntity> commitEntityList = commitRepository.findExistingCommitMessage(query);
        ApiResponse<List<CommitEntity>> apiResponse = new ApiResponse<>();
        apiResponse.setMessage(null);
        apiResponse.setData(commitEntityList);
        apiResponse.setToken(null);
        apiResponse.setResult(true);

        return ResponseEntity.ok(apiResponse);
    }

    @GetMapping("/search/similarity")
    public ResponseEntity<ApiResponse> searchSimilarCommit(@RequestParam String userName, @RequestParam String query) {
        ApiResponse<List<SearchResultDTO>> apiResponse = new ApiResponse<>();

        List<SearchResultDTO> searchResultList = new ArrayList<>();
        for (Pair<String, String> oidKeyword : searchService.getTop10LevenshteinSimilarEntity(query)) {
            SearchResultDTO searchResultDTO = new SearchResultDTO();
            CommitEntity commitEntity = commitRepository.findByCommitOid(oidKeyword.getFirst()).orElseThrow();
            searchResultDTO.setCommitDate(commitEntity.getCommitDate());
            searchResultDTO.setCommitMessage(commitEntity.getCommitMessage());
            searchResultDTO.setRepoName(commitEntity.getRepoName());
            searchResultDTO.setKeywordSet(oidKeyword.getSecond());
            searchResultDTO.setCommitOid(oidKeyword.getFirst());
            searchResultList.add(searchResultDTO);
        }
        apiResponse.setMessage(null);
        apiResponse.setData(searchResultList);
        apiResponse.setToken(null);
        apiResponse.setResult(true);
        return ResponseEntity.ok(apiResponse);
    }


    @GetMapping("/search/commits")
    public Mono<ResponseEntity<ApiResponse<Map<String, Object>>>> searchDetailCommits(@RequestParam String commitOid, @RequestHeader String Authorization) {
        ApiResponse<Map<String, Object>> apiResponse = new ApiResponse<>();
        Map<String, Object> combinedData = new HashMap<>();
        List<Map<String, Object>> diffList = new ArrayList<>();

        return patchService.getPatchesByCommitOid(commitOid)
                .flatMap(patch -> {
                    String contentsUrl = patch.getContentsUrl();
                    String fullPath = patch.getFileName();
                    String filename = extractFileName(fullPath);
                    return patchService.fetchCode(contentsUrl, Authorization)
                            .map(decodedCode -> {
                                Map<String, Object> diff = patchService.analyzeDiffWithContent(patch.getPatch(), decodedCode);
                                diff.put("filename", filename); // 파일 이름 대신 전체 경로 사용
                                diff.put("fullPath", fullPath);
                                diff.put("filetype", extractFileType(fullPath)); // 파일 확장자 추출
                                synchronized (diffList) {
                                    diffList.add(diff);
                                }
                                return diff;
                            });
                })
                .then(Mono.just(diffList))
                .map(diffListData -> {
                    // Build the file tree from filenames
                    List<String> fullPaths = diffListData.stream().map(diff -> (String) diff.get("fullPath")).collect(Collectors.toList());
                    FileTreeNode fileTree = buildFileTree(fullPaths); // 전체 경로를 사용하여 트리 구성

                    combinedData.put("diffs", diffListData);
                    combinedData.put("fileTree", fileTree);

                    apiResponse.setResult(true);
                    apiResponse.setData(combinedData);
                    return ResponseEntity.ok(apiResponse);
                });
    }

    private String extractFileName(String filename) {
        int lastSlashIndex = filename.lastIndexOf("/");
        if (lastSlashIndex != -1 && lastSlashIndex < filename.length() - 1) {
            return filename.substring(lastSlashIndex + 1);
        } else {
            return filename;
        }
    }

    private String extractFileType(String filename) {
        int lastDotIndex = filename.lastIndexOf(".");
        if (lastDotIndex != -1 && lastDotIndex < filename.length() - 1) {
            return filename.substring(lastDotIndex + 1);
        } else {
            return "";
        }
    }

    private FileTreeNode buildFileTree(List<String> filenames) {
        // 공통 접두사 찾기
        String commonPrefix = findCommonPrefix(filenames);

        // 루트 노드를 공통 접두사로 설정
        FileTreeNode root = new FileTreeNode(commonPrefix, "folder");

        for (String filename : filenames) {
            // 공통 접두사를 제외한 부분을 처리
            String relativePath = filename.substring(commonPrefix.length());
            FileTreeNode current = root;
            String[] parts = relativePath.split("/");

            for (int i = 0; i < parts.length; i++) {
                String part = parts[i];
                boolean isFile = (i == parts.length - 1);
                boolean found = false;

                for (FileTreeNode child : current.children) {
                    if (child.name.equals(part)) {
                        current = child;
                        found = true;
                        break;
                    }
                }

                if (!found) {
                    FileTreeNode newNode = new FileTreeNode(part, isFile ? "file" : "folder");
                    current.children.add(newNode);
                    current = newNode;
                }
            }
        }

        return root;
    }


    private String findCommonPrefix(List<String> filenames) {
        if (filenames == null || filenames.isEmpty()) {
            return null;
        }

        String commonPrefix = filenames.get(0);
        for (int i = 1; i < filenames.size(); i++) {
            while (filenames.get(i).indexOf(commonPrefix) != 0) {
                commonPrefix = commonPrefix.substring(0, commonPrefix.length() - 1);
            }
        }
        return commonPrefix;
    }
}