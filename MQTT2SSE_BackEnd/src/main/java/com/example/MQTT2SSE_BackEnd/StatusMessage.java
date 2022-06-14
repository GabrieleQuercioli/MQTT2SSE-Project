package com.example.MQTT2SSE_BackEnd;

import java.time.LocalDateTime;

//Objects from this class contains the payload of an MQTT message with the time when it arrived on channel
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
