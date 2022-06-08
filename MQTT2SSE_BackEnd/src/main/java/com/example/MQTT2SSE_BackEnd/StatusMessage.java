package com.example.MQTT2SSE_BackEnd;

import java.time.LocalDateTime;

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
