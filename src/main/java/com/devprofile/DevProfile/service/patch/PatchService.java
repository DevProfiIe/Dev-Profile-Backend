package com.devprofile.DevProfile.service.patch;

import com.devprofile.DevProfile.component.JwtProvider;
import com.devprofile.DevProfile.entity.PatchEntity;
import com.devprofile.DevProfile.entity.UserEntity;
import com.devprofile.DevProfile.repository.PatchRepository;
import com.devprofile.DevProfile.repository.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.difflib.DiffUtils;
import com.github.difflib.patch.AbstractDelta;
import com.github.difflib.patch.Patch;
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

    public Flux<PatchEntity> getPatchesByCommitOid(String commitOid) {
        return Flux.fromIterable(patchRepository.findByCommitOid(commitOid));
    }

    public Map<String, Object> compareCodeAndDiff(String code, List<String> diff) {
        List<String> codeLines = Stream.of(code.split("\n")).collect(Collectors.toList());
        Map<String, Object> result = new HashMap<>();

        for (String line : diff) {
            if (codeLines.contains(line)) {
                result.put(line, "original");
            } else {
                result.put(line, "modified");
            }
        }
        return result;
    }

    public List<String> analyzeDiff(String patch, String originalText) {
        List<String> originalLines = Stream.of(originalText.split("\n")).collect(Collectors.toList());
        List<String> patchLines = Stream.of(patch.split("\n")).collect(Collectors.toList());

        Patch<String> diffs = DiffUtils.diff(originalLines, patchLines);

        List<String> result = new ArrayList<>();
        for (AbstractDelta<String> delta : diffs.getDeltas()) {
            result.addAll(delta.getSource().getLines());
        }

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
        UserEntity user = userRepository.findById(Integer.parseInt(primaryId)).orElseThrow();

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