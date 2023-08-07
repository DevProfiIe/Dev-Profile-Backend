package com.devprofile.DevProfile.service.rabbitmq;

import com.devprofile.DevProfile.entity.CommitEntity;
import com.devprofile.DevProfile.entity.PatchEntity;
import com.devprofile.DevProfile.entity.RepositoryEntity;
import com.devprofile.DevProfile.entity.UserEntity;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class MessageOrgSenderService {
    @Autowired
    private AmqpTemplate amqpTemplate;
    @Autowired
    private ObjectMapper objectMapper;

    @Value("${rabbitmq.exchange2}")
    private String exchange;

    @Value("${rabbitmq.routingkey2}")
    private String routingkey;

    public Mono<String> orgMainSendMessage(UserEntity userEntity) {
        return Mono.fromCallable(() -> {
            String message = objectMapper.writeValueAsString(userEntity);
            amqpTemplate.convertAndSend(exchange, routingkey, message);
            return "Message sent for User: " + userEntity.getId();
        });
    }

    public Mono<String> orgRepoSendMessage(RepositoryEntity repositoryEntity) {
        return Mono.fromCallable(() -> {
            String message = objectMapper.writeValueAsString(repositoryEntity);
            amqpTemplate.convertAndSend(exchange, routingkey, message);
            return "Message sent for Repository: " + repositoryEntity.getUserId();
        });
    }

    public Mono<String> orgCommitSendMessage(CommitEntity commitEntity) {
        return Mono.fromCallable(() -> {
            String message = objectMapper.writeValueAsString(commitEntity);
            amqpTemplate.convertAndSend(exchange, routingkey, message);
            return "Message sent for Commit: " + commitEntity.getUserId();
        });
    }
    public Mono<String> orgPatchSendMessage(PatchEntity patchEntity) {
        return Mono.fromCallable(() -> {
            String message = objectMapper.writeValueAsString(patchEntity);
            amqpTemplate.convertAndSend(exchange, routingkey, message);
            return "Message sent for Patch: " + patchEntity.getCommitOid();
        });
    }

    public Mono<String> orgSendMessage(String message) {
        return Mono.fromCallable(() -> {
            amqpTemplate.convertAndSend(exchange, routingkey, message);
            return "Message sent: " + message;
        });
    }
}
