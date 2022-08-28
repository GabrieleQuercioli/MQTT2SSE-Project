package com.example.MQTT2SSE_BackEnd;

import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import javax.persistence.*;
import java.time.LocalDateTime;


/* HERE IS WHERE THE DATABASE COLUMN WERE DEFINED */
@Entity
@Table(name = "webclients")   // the name of the db table
public class UserEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "ID")
    private Long id;

    @Column(name = "userName")
    private String userName;

    @Column(name = "topic")
    private String topic;

    @Column(name = "timeOfSubscription")
    private LocalDateTime time;

    public UserEntity() {
    }

    public UserEntity(String userName, String topic) {
        this.userName = userName;
        this.topic = topic;
        this.time = LocalDateTime.now();
    }

    public Long getUserID() {
        return id;
    }

    public void setUserID(Long id) {
        this.id = id;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public LocalDateTime getTime() {
        return time;
    }

    public void setTime(LocalDateTime time) {
        this.time = time;
    }
}
