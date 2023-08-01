package com.devprofile.DevProfile.dto.response.analyze;

import java.util.Set;

public class CommitKeywordsDTO {
    private Set<String> featured;
    private Set<String> langFramework;

    public Set<String> getFeatured() {
        return featured;
    }

    public void setFeatured(Set<String> featured) {
        this.featured = featured;
    }

    public Set<String> getLangFramework() {
        return langFramework;
    }

    public void setLangFramework(Set<String> langFramework) {
        this.langFramework = langFramework;
    }
}
