package com.example.MQTT2SSE_BackEnd;

import javafx.util.Pair;
import org.json.JSONObject;

import org.springframework.messaging.MessageHandlingException;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;
import static java.time.temporal.ChronoUnit.SECONDS;

//This class contains all methods and structures that permits the push notifications to all web clients listening
@Service
public class SseController {

    //K: <UserID,Emitter> V: Topic - keeps a many 2 many relation between clients and topics subscribed
    public static Multimap<Pair<String,SseEmitter>,String> emitters = ArrayListMultimap.create();

    //This map contains the last message sent by the broker for each topic
    public static Map<String,StatusMessage> statusMessages = new HashMap<String, StatusMessage>();

    private static int msgID = 2;


    public void addEmitter(String userID, SseEmitter emitter, String topic) {
        emitters.put(new Pair<String, SseEmitter>(userID, emitter), topic);
    }

    public void removeEmitter(String userID, SseEmitter emitter, String topic) {
        emitters.remove(new Pair<String, SseEmitter>(userID, emitter), topic);
    }

    public void sendInitEvent(SseEmitter sseEmitter, String topic) {
        try {
            //Send the topic subscribed to the client to be visualized on his page
            String topicJson = new JSONObject().put("topic", topic).toString();
            sseEmitter.send(SseEmitter.event().id("msg ID: " + 0).name("INIT").data(topicJson));
        } catch (IOException e){
            sseEmitter.completeWithError(e);
        }
    }

    public static void addStatusMessage(String topic, StatusMessage sm) {
        statusMessages.put(topic,sm);
    }

    public void sendStatus(SseEmitter emitter, String topic) throws IOException {
        //When subscribe with wildcard, get the status msg for each topic, otherwise only one for the subscribed topic
        try {
            String statusPayload;
            if (topic.contains("#"))
                for (String key : statusMessages.keySet()) {
                    statusPayload = statusMessages.get(key).getStatusPayload();
                    System.out.println("Lo status message è: " + statusPayload);
                    if (statusPayload != null) {
                        String eventFormatted = getEventFormatted(key,statusPayload);
                        emitter.send(SseEmitter.event().id("msg ID: " + 1).name("diagnosys").data(eventFormatted));
                    }
                }
            else {
                statusPayload = statusMessages.get(topic).getStatusPayload();
                System.out.println("Status message: " + statusPayload);
                System.out.println("Status Messages Queue: " + statusMessages);
                if (statusPayload != null) {
                    String eventFormatted = getEventFormatted(topic, statusPayload);
                    emitter.send(SseEmitter.event().id("msg ID: " + 1).name("diagnosys").data(eventFormatted));
                }
            }
        } catch (NullPointerException e) {
            System.out.println("There isn't a status message");
        }
    }

    //method to dispatch events for specific web clients
   public static void dispatchEventsToClients(@RequestParam String topic, @RequestParam String payload) throws MessageHandlingException {

        for (Map.Entry<Pair<String, SseEmitter>, String> it : emitters.entries())
            System.out.println("User client active: " + it.getKey() + "  listening topic:  " + it.getValue());

        System.out.println("parameters: " + topic + " " + payload);

        String eventFormatted = getEventFormatted(topic,payload);
        //retrieves all clients subscribed on the topic
        ArrayList<Pair<String,SseEmitter>> emittersByTopic = searchByTopic(topic);
        System.out.println("Emitter: " + emittersByTopic);

        for (Pair<String,SseEmitter> pair : emittersByTopic) {
            System.out.println("New msg in topic: " + topic + " for User: " + pair.getKey()
                    + " with Emitter: " + pair.getValue());
            SseEmitter emitter = pair.getValue();
            try {
                //send the event for each emitter
                emitter.send(SseEmitter.event().id("msg ID: " + msgID++).name("diagnosys").data(eventFormatted));
            } catch (IOException e) {
                System.out.println("Dispatcher Error");
            }
        }
    }

    //return an array list with all clients subscribed to the topic with the respective emitter
    private static ArrayList<Pair<String,SseEmitter>> searchByTopic(String topic) {
        ArrayList<Pair<String,SseEmitter>> emitter = new ArrayList<Pair<String, SseEmitter>>();
        for (Map.Entry<Pair<String, SseEmitter>, String> it : emitters.entries()){
            if (it.getValue().equals(topic) || it.getValue().contains("#")){
                System.out.println(it.getKey());
                emitter.add(it.getKey());
            }
            /*else {
                System.out.println("fail");
            }*/
        }
        return emitter;
    }

    //returns the emitter of the topic that a specific client has subscribed to
    public SseEmitter findEmitter(String userID, String topic) {
        SseEmitter sseEmitter = null;
        for (Map.Entry<Pair<String, SseEmitter>, String> it : emitters.entries()){
            if (it.getValue().equals(topic) && it.getKey().getKey().equals(userID)){
                sseEmitter = it.getKey().getValue();
            }
        }
        return sseEmitter;
    }

    //method for getting all fields in the payload of mqtt msg and re formatting the message for the client part
    private static String getEventFormatted(String topic, String payload) {
        JSONObject jsonPayload = new JSONObject(payload);
        String dev = jsonPayload.getString("device");
        String ip = decryptIP(dev);
        String op = jsonPayload.getString("operation");
        if (op.equals("NONE"))
            op = " ";
        int fps = jsonPayload.getInt("fps");
        String res = jsonPayload.getString("res");
        long secSinceLast = 0L;
        if (statusMessages.get(topic) != null) { //calculate in sec the time since last message for this topic was sent
            secSinceLast = SECONDS.between(statusMessages.get(topic).getDateWhenIsSent(), LocalDateTime.now());
            System.out.println("Seconds since last for this topic: " + secSinceLast);
        }
        else {
            System.out.println("First message in the topic");
        }

        //stringify the JSON msg
        return new JSONObject().put("title", topic).put("device", ip)
                .put("operation", op).put("fps", fps).put("resolution", res).put("tslm", secSinceLast).toString();
    }

    private static String decryptIP(String device) {
        String decIP;
        if (device.contains("b827eb7ea205"))
            decIP = "172.20.3.15";
        else if (device.contains("b827eb118ad5"))
            decIP = "172.20.3.16";
        else if (device.contains("b827ebdb3577"))
            decIP = "172.20.3.18";
        else if (device.contains("b827ebcd8271"))
            decIP = "172.20.3.19";
        else if (device.contains("b827eb524d0c"))
            decIP = "172.20.3.21";
        else if (device.contains("b827ebe7e6b6"))
            decIP = "172.20.3.22";
        else if (device.contains("b827eb7c9ec8"))
            decIP = "172.20.3.23";
        else if (device.contains("b827ebfd6fa0"))
            decIP = "172.20.3.25";
        else
            decIP = "IP Not Found";
        return decIP;
    }
}