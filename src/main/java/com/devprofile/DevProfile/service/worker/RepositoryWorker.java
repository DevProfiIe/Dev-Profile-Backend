package com.devprofile.DevProfile.service.worker;

import com.devprofile.DevProfile.service.RepositoryService;
import com.devprofile.DevProfile.service.message.RepositoryMessage;
import com.devprofile.DevProfile.service.queue.RepositoryQueue;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
public class RepositoryWorker {
    private final RepositoryQueue repositoryQueue;
    private final RepositoryService repositoryService;

    public RepositoryWorker(RepositoryQueue repositoryQueue, RepositoryService repositoryService) {
        this.repositoryQueue = repositoryQueue;
        this.repositoryService = repositoryService;
    }

    @PostConstruct
    public void init() {
        Flux<RepositoryMessage> messages = repositoryQueue.receive();
        messages.flatMap(this::processMessage).subscribe();
    }

    private Mono<Void> processMessage(RepositoryMessage message) {
        return repositoryService.saveRepository(message.getRepository(), message.getUserId());
    }
}
