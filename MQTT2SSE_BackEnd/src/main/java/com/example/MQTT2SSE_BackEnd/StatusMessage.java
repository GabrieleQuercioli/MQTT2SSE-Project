package com.example.MQTT2SSE_BackEnd;

import java.time.LocalDateTime;

//Is right to create a new object every time a message is sent?
public class StatusMessage {

    private String statusPayload;
    private LocalDateTime dateWhenIsSent;


    public StatusMessage(String statusPayload) {
        this.statusPayload = statusPayload;
        dateWhenIsSent = LocalDateTime.now();
    }

    public String getStatusPayload() {
        return statusPayload;
    }

    public LocalDateTime getDateWhenIsSent() {
        return dateWhenIsSent;
    }
}
