package com.example.MQTT2SSE_BackEnd.repository;

import com.example.MQTT2SSE_BackEnd.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/*  THIS INTERFACE ALLOWS TO USE JPA GENERAL METHODS FOR MANIPULATE THE DB TABLE */
@Repository
public interface UserRepository extends JpaRepository<UserEntity, Long> {
    // custom queries
    List<UserEntity> findByUserName(String userName);
    List<UserEntity> findByUserNameAndTopic(String userName, String topic);
}
