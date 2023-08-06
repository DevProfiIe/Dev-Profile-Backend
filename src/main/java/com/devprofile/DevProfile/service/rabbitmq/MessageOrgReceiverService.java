package com.devprofile.DevProfile.service.rabbitmq;

import com.devprofile.DevProfile.service.commit.CommitUserService;
import com.devprofile.DevProfile.service.graphql.GraphUserService;
import com.devprofile.DevProfile.service.repository.UserRepoService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
public class MessageOrgReceiverService {
    @Autowired
    private GraphUserService graphUserService;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepoService userRepoService;

    @Autowired
    private CommitUserService commitUserService;

    @RabbitListener(queues = "${rabbitmq.queue2}")
    public void receivedMessage(String message) {
        try {
            JsonNode jsonNode = objectMapper.readTree(message);

            if (jsonNode.has("repoName")) {
                String repoName = jsonNode.get("repoName").asText();
                System.out.println("Received Message From RabbitMQ - Repository: " + repoName);
            } else if (jsonNode.has("commitMessage")) {
                String commitMessage = jsonNode.get("commitMessage").asText();
                System.out.println("Received Message From RabbitMQ - Commit: " + commitMessage);
            } else if (jsonNode.has("userName")) {
                String userName = jsonNode.get("userName").asText();
                System.out.println("Received Message From RabbitMQ - User: " + userName);
            } else if (jsonNode.has("patch")) {
                String patch = jsonNode.get("patch").asText();
            } else {
                System.out.println("Received Message From RabbitMQ: Unknown entity");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
