package com.example.MQTT2SSE_BackEnd.service;

import com.example.MQTT2SSE_BackEnd.UserEntity;

import java.util.List;
import java.util.Optional;

/*  THIS INTERFACE EXPOSES CRUD METHODS FOR OBJECTS IN THE DB TABLE */
public interface UserService {
    List<UserEntity> findAllUsers();
    Optional<UserEntity> findByID(Long id);
    List<UserEntity> findByUserName(String userName);
    List<UserEntity> findByUserNameAndTopic(String userName, String topic);
    UserEntity saveUser(UserEntity user);
    void deleteUser(Long id);
    void deleteUser(String userName, String topic);
    void deleteUsers(String userName);
}
