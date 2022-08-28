package com.example.MQTT2SSE_BackEnd.controller;


import com.example.MQTT2SSE_BackEnd.service.SseService;
import com.example.MQTT2SSE_BackEnd.UserEntity;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;

//This class contains all methods that manages the Rest requests from client web
@RestController
public class SseRestController {

    @Autowired
    private SseService ssePushService;


    @CrossOrigin
    @RequestMapping(value = "/subscribe", consumes = MediaType.ALL_VALUE) //define the endpoint for the REST request
    public ResponseEntity<SseEmitter> subscribe(@RequestParam String userID, @RequestParam String topic) throws IOException {

        if (topic.contains("*")) //Because client for request a sub with wildcard send * instead # due to encoding problem
            topic = topic.replace("*","#");

        if (ssePushService.findByUserNameAndTopic(userID,topic).size() == 0) {
            SseEmitter sseEmitter = new SseEmitter(Long.MAX_VALUE); //the time in millis before the emitter get timed-out
            ssePushService.sendInitEvent(sseEmitter,topic);

            UserEntity userEntity = new UserEntity(userID, topic);
            ssePushService.saveUser(userEntity);

            ssePushService.addEmitter(userID,sseEmitter,topic);
            ssePushService.sendStatus(sseEmitter, topic);

            String finalTopic = topic;
            sseEmitter.onCompletion(() -> {
                ssePushService.removeEmitter(userID,sseEmitter,finalTopic);
                System.out.println("The emitter: " + sseEmitter + " of Client: " + userID + " is completed");
                //Remove from the db all the rows with this user
                ssePushService.deleteUsers(userID);
            });
            sseEmitter.onTimeout(() -> {
                ssePushService.removeEmitter(userID,sseEmitter,finalTopic);
                System.out.println("The emitter: " + sseEmitter + " of Client: " + userID + " timed-out");
                ssePushService.deleteUsers(userID);
            });

            return new ResponseEntity<SseEmitter>(sseEmitter, HttpStatus.OK);
        }
        else {
            System.out.println("Già iscritto al topic");
            return new ResponseEntity<>(null, HttpStatus.BAD_REQUEST);
        }
    }


    @CrossOrigin
    @PostMapping("/unsubscribe")
    public ResponseEntity<?> unsubscribeTopic(@RequestBody String mqttMessage) {
        try {
            System.out.println(mqttMessage);
            String topicAdjusted = adjustJSONstringFormat(mqttMessage);
            JSONObject convertObject = new JSONObject(topicAdjusted);
            System.out.println(convertObject);
            String topic = convertObject.get("topic").toString();
            String userID = convertObject.get("user").toString();
            topicAdjusted = adjustJSONstringFormat(topic);
            System.out.println("topic to unsubscribe: " + topicAdjusted);
            SseEmitter emitterToRemove = ssePushService.findEmitter(userID, topicAdjusted);
            try {
                if (emitterToRemove != null) {
                    System.out.println("Unsubscribed");
                    ssePushService.removeEmitter(userID,emitterToRemove,topicAdjusted);
                    // delete the row of the subscription from the db
                    ssePushService.deleteUser(userID,topicAdjusted);
                }
                else
                    System.out.println("Can't unsubscribe to topic if not subscribed");
            } catch (Exception e) {
                e.printStackTrace();
            }
            return ResponseEntity.ok("Success");
        } catch (Exception e){
            e.printStackTrace();
            return ResponseEntity.ok("Fail");
        }
    }

    //function that replace characters forbidden in JSON format
    public static String adjustJSONstringFormat(String toAdjust) {
        String adjusted = toAdjust;
        if (toAdjust.contains("/") || toAdjust.contains("#")) {
            if (toAdjust.contains("/"))
                adjusted = toAdjust.replace("/", "*");
            if (toAdjust.contains("#"))
                adjusted = adjusted.replace("#", "§");
        }
        else { //else part is to transform back topic to original value
            if (toAdjust.contains("*"))
                adjusted = toAdjust.replace("*", "/");
            if (toAdjust.contains("§"))
                adjusted = adjusted.replace("§", "#");
        }
        return adjusted;
    }
}