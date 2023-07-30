package com.devprofile.DevProfile.repository;

import com.devprofile.DevProfile.entity.ChatRoom;
import com.devprofile.DevProfile.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long> {
    Optional<ChatRoom> findByUser1AndUser2(UserEntity user1, UserEntity user2);
}
