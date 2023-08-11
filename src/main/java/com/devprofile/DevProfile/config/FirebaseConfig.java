package com.devprofile.DevProfile.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

import java.io.IOException;

@Configuration
public class FirebaseConfig {

    @Bean
    public FirebaseApp firebaseApp() throws IOException {
        Resource resource = new ClassPathResource("firebase/dev-profile-251ff-firebase-adminsdk-gqky2-024feff5ba.json");

        FirebaseOptions options = new FirebaseOptions.Builder()
                .setCredentials(GoogleCredentials.fromStream(resource.getInputStream()))
                .setDatabaseUrl("https://dev-profile-251ff.firebaseio.com")
                .build();

        return FirebaseApp.initializeApp(options);
    }
}
