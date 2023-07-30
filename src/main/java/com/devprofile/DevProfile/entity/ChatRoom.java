package com.devprofile.DevProfile.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@Table(name = "chat_room")
public class ChatRoom {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "send_id")
    private UserEntity send;

    @ManyToOne
    @JoinColumn(name = "receive_id")
    private UserEntity receive;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

}
