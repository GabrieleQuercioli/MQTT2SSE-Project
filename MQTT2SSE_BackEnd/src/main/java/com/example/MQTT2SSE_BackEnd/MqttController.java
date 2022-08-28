package com.example.MQTT2SSE_BackEnd;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class MqttController {

    @Autowired
    public MqttGateway mqttGateway;

    @CrossOrigin
    @PostMapping("/sendMessage")
    public ResponseEntity<?> publish(@RequestBody String mqttMessage) {
        try {
            System.out.println(mqttMessage);
            //TODO controlla che sui caratteri / e # funzioni
            JSONObject convertObject = new JSONObject(mqttMessage);
            System.out.println(convertObject);
            mqttGateway.sendToMqtt(convertObject.get("message").toString(), convertObject.get("topic").toString());
            return ResponseEntity.ok("Success");
        } catch (Exception e){
            e.printStackTrace();
            return ResponseEntity.ok("Fail");
        }
    }

}