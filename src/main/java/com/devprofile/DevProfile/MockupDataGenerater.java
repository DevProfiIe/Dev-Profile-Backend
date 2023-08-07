package com.devprofile.DevProfile;

import com.devprofile.DevProfile.entity.FilterEntity;
import com.devprofile.DevProfile.entity.FrameworkEntity;
import com.devprofile.DevProfile.repository.FilterRepository;
import com.devprofile.DevProfile.repository.FrameworkRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;


@AllArgsConstructor
@Service
public class MockupDataGenerater {


    private static final Random random = new Random();
    private final FrameworkRepository frameworkRepository;
    private final FilterRepository filterRepository;

    private static <T> T getRandomElement(List<T> list) {
        int index = random.nextInt(list.size());
        return list.get(index);
    }

    public static String getRandomField(List<String> fieldList) {
        int index = new Random().nextInt(fieldList.size());
        return fieldList.get(index);
    }

    private static Set<String> getRandomStyles(List<String> styles) {
            int size = random.nextInt(styles.size()) + 1;
            return random.ints(size, 0, styles.size())
                    .mapToObj(styles::get)
                    .collect(Collectors.toSet());
        }

    public String generateUniqueUserName() {
        return UUID.randomUUID().toString().replace("-", "").substring(0 , new Random().nextInt(5,10));
    }

    private static Map<String, Integer> getRandomLanguages(List<String> languages) {
        int size = random.nextInt(languages.size()) + 1;
        return random.ints(size, 0, languages.size())
                .mapToObj(languages::get)
                .collect(Collectors.toMap(lang -> lang, lang -> random.nextInt(180), (oldValue, newValue) -> oldValue));
    }

    private static Map<String, Integer> getRandomFrameworks(List<String> frameworks) {
        int size = random.nextInt(frameworks.size()) + 1;
        return random.ints(size, 0, frameworks.size())
                .mapToObj(frameworks::get)
                .collect(Collectors.toMap(framework -> framework, framework -> random.nextInt(180), (oldValue, newValue) -> oldValue));
    }

    public FilterEntity createMockFilterEntity() {
        List<FrameworkEntity> frameworkEntities = frameworkRepository.findAll();
        List<String> frameworkList = new ArrayList<>();
        for(FrameworkEntity framework: frameworkEntities){
            Map<String, String> frameworkMap = new HashMap<>();
            frameworkList.add(framework.getFrameworkName());
        }
        List<String> languageList = new ArrayList<>();
        languageList.add("Java");
        languageList.add("C");
        languageList.add("Ruby");
        languageList.add("C++");
        languageList.add("Python");
        languageList.add("Go");
        languageList.add("C#");
        languageList.add("JavaScript");
        languageList.add("TypeScript");
        languageList.add("PHP");
        languageList.add("Kotlin");
        languageList.add("Rust");
        languageList.add("R");
        languageList.add("Swift");
        languageList.add("Perl");

        List<String> styleList = Arrays.asList(
                "지속적인 개발자",
                "싱글 플레이어",
                "멀티 플레이어",
                "1일 1커밋",
                "인기 개발자",
                "영향력 있는 개발자",
                "새벽형 개발자",
                "아침형 개발자",
                "설명충",
                "주말 커밋 전문가",
                "주말 커밋자",
                "실속없는 커밋자"
        );

        List<String> userNameList = Arrays.asList(
                "JihoonKim",
                "YeonSooLee",
                "MinseokPark",
                "EunjiChoi",
                "HyunwooKang",
                "Sohee Lim",
                "JungsooHan",
                "Seojin Yoon",
                "Yongha Jang",
                "MinjiKo",
                "SungminShin",
                "Haeun Song",
                "Junhee Moon",
                "YoonhoNa",
                "SaebyeokOh",
                "Haerin Kwon",
                "Jihoon Jung",
                "MyName",
                "SpecialName",
                "Sindle",
                "Seungyeon Hwang",
                "Yejin Kim",
                "Seongsoo Park"
        );

        List<String> fieldList = Arrays.asList(
                "ai",
                "database",
                "webBackend",
                "webFrontend",
                "game",
                "systemProgramming"
        );


        FilterEntity entity = new FilterEntity();
        entity.setId(UUID.randomUUID().toString());
        entity.setUserLogin(null);
        String uniqueUser =generateUniqueUserName();
        entity.setUserName(uniqueUser);
        entity.setUserLogin(uniqueUser);
        entity.setAvatarUrl(null);
        entity.setField(getRandomField(fieldList));
        entity.setStyles(getRandomStyles(styleList));
        entity.setLanguages(getRandomLanguages(languageList));
        entity.setFrameworks(getRandomFrameworks(frameworkList));
        entity.setRepoCount(new Random().nextInt(50) + 1);
        entity.setCommitCount(new Random().nextInt(500) + 1);
        entity.setCommitDays(new Random().nextInt(900) + 1);

        return entity;
    }

    // use this method to generate a list of mock FilterEntity
    public void generateMockFilterEntities(int num) {
        filterRepository.saveAll(
        Stream.generate(() -> createMockFilterEntity())
                .limit(num)
                .collect(Collectors.toList()));
    }
}

