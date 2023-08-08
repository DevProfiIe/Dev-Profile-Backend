package com.devprofile.DevProfile.service.rabbitmq;

import com.devprofile.DevProfile.entity.CommitEntity;
import com.devprofile.DevProfile.entity.PatchEntity;
import com.devprofile.DevProfile.entity.RepositoryEntity;
import com.devprofile.DevProfile.entity.UserEntity;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@Slf4j
public class MessageSenderService {

    @Autowired
    private AmqpTemplate amqpTemplate;
    @Autowired
    private ObjectMapper objectMapper;

    @Value("${rabbitmq.exchange}")
    private String exchange;

    @Value("${rabbitmq.routingkey}")
    private String routingkey;

    public Mono<String> MainSendMessage(UserEntity userEntity) {
        return Mono.fromCallable(() -> {
            String message = objectMapper.writeValueAsString(userEntity);
            amqpTemplate.convertAndSend(exchange, routingkey, message);
            return "Message sent for User: " + userEntity.getId();
        });
    }

    public Mono<String> RepoSendMessage(RepositoryEntity repositoryEntity) {
        return Mono.fromCallable(() -> {
            String message = objectMapper.writeValueAsString(repositoryEntity);
            amqpTemplate.convertAndSend(exchange, routingkey, message);
            return "Message sent for Repository: " + repositoryEntity.getUserId();
        });
    }

    public Mono<String> CommitSendMessage(CommitEntity commitEntity) {
        return Mono.fromCallable(() -> {
            String message = objectMapper.writeValueAsString(commitEntity);
            amqpTemplate.convertAndSend(exchange, routingkey, message);
            return "Message sent for Commit: " + commitEntity.getUserId();
        });
    }

    public Mono<Void> PatchSendMessage(PatchEntity patchEntity) {
        return Mono.fromRunnable(() -> {
            try {
                String message = objectMapper.writeValueAsString(patchEntity);
                amqpTemplate.convertAndSend(exchange, routingkey, message);
                log.info("Message sent for Patch: " + patchEntity.getCommitOid());
            } catch (JsonProcessingException e) {
                log.error("Error serializing PatchEntity: ", e);
            }
        });
    }

    public Mono<String> sendMessage(String message) {
        return Mono.fromCallable(() -> {
            amqpTemplate.convertAndSend(exchange, routingkey, message);
            return "Message sent: " + message;
        });
    }
}