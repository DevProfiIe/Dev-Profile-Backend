package com.devprofile.DevProfile.service.queue;


import com.devprofile.DevProfile.service.message.RepositoryMessage;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

@Service
public class RepositoryQueue {
    private final Queue<RepositoryMessage> queue = new ConcurrentLinkedQueue<>();

    public void send(RepositoryMessage message) {
        queue.add(message);
    }

    public Flux<RepositoryMessage> receive() {
        return Flux.fromIterable(queue);
    }
}
