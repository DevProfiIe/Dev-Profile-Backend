package com.devprofile.DevProfile.service.patch;

import com.devprofile.DevProfile.component.JwtProvider;
import com.devprofile.DevProfile.entity.PatchEntity;
import com.devprofile.DevProfile.entity.UserEntity;
import com.devprofile.DevProfile.repository.PatchRepository;
import com.devprofile.DevProfile.repository.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.net.URI;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class PatchService {
    private final PatchRepository patchRepository;
    @Autowired
    private final WebClient webClient; // 변경된 부분
    private final JwtProvider jwtProvider;
    private final UserRepository userRepository;

    @Autowired
    public PatchService(PatchRepository patchRepository, @Qualifier("patchWebClient") WebClient webClient, JwtProvider jwtProvider, UserRepository userRepository) { // 변경된 부분
        this.patchRepository = patchRepository;
        this.webClient = webClient;
        this.jwtProvider = jwtProvider;
        this.userRepository = userRepository;
    }
    public Map<String, Object> analyzeDiffWithContent(String patch, String originalText) {
        List<String> contentLines = Stream.of(originalText.split("\n")).collect(Collectors.toList());
        List<String> patchLines = Stream.of(patch.split("\n")).collect(Collectors.toList());

        List<Integer> original = new ArrayList<>();
        List<Integer> inserted = new ArrayList<>();
        List<Integer> deleted = new ArrayList<>();

        List<String> modifiedContent = new ArrayList<>(contentLines);

        int contentIndex = 0;
        int diffIndex = 0;

        while (diffIndex < patchLines.size()) {
            String line = patchLines.get(diffIndex);

            if (line.startsWith("@@")) {
                String[] numbers = line.split(" ")[1].substring(1).split(",");
                contentIndex = Integer.parseInt(numbers[0]) - 1;
                if (contentIndex < 0) contentIndex = 0;
            } else if (line.startsWith("+")) {
                inserted.add(contentIndex + 1);
                modifiedContent.add(contentIndex, line.substring(1));
                contentIndex++;
            } else if (line.startsWith("-")) {
                deleted.add(contentIndex + 1);
                modifiedContent.add(contentIndex, line.substring(1));
                contentIndex++;
            } else {
                original.add(contentIndex + 1);
                contentIndex++;
            }
            diffIndex++;
        }

        Map<String, Object> result = new HashMap<>();
        result.put("content", String.join("\n", modifiedContent));
        result.put("status", Map.of(
                "original", original,
                "inserted", inserted,
                "deleted", deleted
        ));

        return result;
    }



    public Flux<PatchEntity> getPatchesByCommitOid(String commitOid) {
        return Flux.fromIterable(patchRepository.findByCommitOid(commitOid));
    }

    public Map<String, Object> analyzeDiff(String patch, String originalText) {
        List<String> patchLines = Stream.of(patch.split("\n")).collect(Collectors.toList());

        StringBuilder contentBuilder = new StringBuilder();
        List<Integer> original = new ArrayList<>();
        List<Integer> inserted = new ArrayList<>();
        List<Integer> deleted = new ArrayList<>();

        int lineNumber = 1;
        for (String line : patchLines) {
            if (line.startsWith("@@")) {
                continue;
            }

            if (line.startsWith("+")) {
                inserted.add(lineNumber);
                contentBuilder.append(line.substring(1)).append("\n");
            } else if (line.startsWith("-")) {
                deleted.add(lineNumber);
                contentBuilder.append(line.substring(1)).append("\n");
            } else {
                original.add(lineNumber);
                contentBuilder.append(line).append("\n");
            }

            lineNumber++;
        }

        Map<String, Object> result = new HashMap<>();
        result.put("content", contentBuilder.toString());
        result.put("status", Map.of("original", original, "inserted", inserted, "deleted", deleted));

        return result;
    }




    public String decodeBase64(String encoded) {
        byte[] decodedBytes = Base64.getDecoder().decode(encoded);
        String decodedString = new String(decodedBytes);

        return decodedString;
    }

    public Mono<String> fetchCode(String contentsUrl, String Authorization) {
        jwtProvider.validateToken(Authorization);
        String primaryId = jwtProvider.getIdFromJWT(Authorization);
        UserEntity user = userRepository.findById(Integer.parseInt(primaryId))
                .orElseThrow(() -> new IllegalArgumentException("No user found with id: " + primaryId));

        return webClient.get()
                .uri(URI.create(contentsUrl)) // 변경된 부분
                .headers(headers -> headers.setBearerAuth(user.getGitHubToken()))
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, response -> {
                    System.out.println("The requested resource was not found: " + contentsUrl);
                    return Mono.error(new RuntimeException("Not found: " + contentsUrl));
                })
                .bodyToMono(String.class)
                .flatMap(body -> {
                    ObjectMapper objectMapper = new ObjectMapper();
                    JsonNode root;
                    try {
                        root = objectMapper.readTree(body);

                        JsonNode contentNode = root.get("content");
                        if (contentNode != null) {
                            String content = contentNode.asText();
                            if(content != null && !content.isEmpty()) {
                                content = content.replaceAll("\\r\\n|\\r|\\n", "");
                                return Mono.just(decodeBase64(content));
                            }
                        }

                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                    return Mono.empty();
                });
    }
}