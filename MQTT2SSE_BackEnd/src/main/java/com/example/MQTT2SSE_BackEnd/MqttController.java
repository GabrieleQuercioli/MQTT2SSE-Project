package com.example.MQTT2SSE_BackEnd;

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
            JSONObject convertObject = new JSONObject(mqttMessage);
            System.out.println(convertObject);
            mqttGateway.sendToMqtt(convertObject.get("message").toString(), convertObject.get("topic").toString());
            return ResponseEntity.ok("Success");
        } catch (Exception e){
            e.printStackTrace();
            return ResponseEntity.ok("Fail");
        }
    }

    @CrossOrigin
    @PostMapping("/unsubscribe")
    public ResponseEntity<?> unsubscribeTopic(@RequestBody String mqttMessage) {
        try {
            System.out.println(mqttMessage);
            //String topicAdjusted = mqttMessage.replace("/","-");
            //FIXME soluzione tampone
            String topicAdjusted = adjustJSONstringFormat(mqttMessage);
            System.out.println(topicAdjusted);
            JSONObject convertObject = new JSONObject(topicAdjusted);
            System.out.println(convertObject);
            String topic = convertObject.get("topic").toString();
            //System.out.println(topic);
            String userID = convertObject.get("user").toString();
            //System.out.println(userID);
            //mqttGateway.sendToMqtt(topic, userID);
            SseController.unsubscribeTopic(userID,topic);
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
        else {
            if (toAdjust.contains("*"))
                adjusted = toAdjust.replace("*", "/");
            if (toAdjust.contains("§"))
                adjusted = adjusted.replace("§", "#");
        }
        return adjusted;
    }


}
