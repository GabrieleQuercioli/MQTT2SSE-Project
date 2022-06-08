package com.example.MQTT2SSE_BackEnd;

import javafx.util.Pair;
import org.json.JSONObject;
import org.springframework.http.MediaType;
import org.springframework.messaging.MessageHandlingException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;
import static java.time.temporal.ChronoUnit.SECONDS;

@RestController
public class SseController {

    //K: <UserID,Emitter> V: Topic
    public static Multimap<Pair<String,SseEmitter>,String> emitters = ArrayListMultimap.create();

    //This map contains the last message sent by the broker for each topic
    public static Map<String,StatusMessage> statusMessages = new HashMap<String, StatusMessage>();

    private static int msgID = 2;

    // method for client subscription, permits the connection releasing a SSE-emitter
    // that allows clients to listen the specified channel for receiving the events from the server-side
    // HTTP/1.1 permits max 6 connections with the same host
    @CrossOrigin
    @RequestMapping(value = "/subscribe", consumes = MediaType.ALL_VALUE) //define the endpoint for the REST request
    public SseEmitter subscribe(@RequestParam String userID, @RequestParam String topic) throws IOException {
        if (topic.contains("*")) //Because client for request a sub with wildcard send * instead # due to encoding problem
            topic = topic.replace("*","#");

        //If the client is not subscribed already to topic, creates a new tupla with associated emitter
        if (searchEmitter(userID, topic) == null) {
            //SseEmitter is the object that holds the connection between Client (WEB) and Server (THIS)
            SseEmitter sseEmitter = new SseEmitter(Long.MAX_VALUE); //the time in millis before the emitter get timed-out
            sendInitEvent(sseEmitter, topic);
            emitters.put(new Pair<String, SseEmitter>(userID, sseEmitter), topic);

            //When subscribe with wildcard, get the status msg for each topic, otherwise only one for the subscribed topic
            try {
                String statusPayload;
                if (topic.contains("#"))
                    for (String key : statusMessages.keySet()) {
                        statusPayload = statusMessages.get(key).getStatusPayload();
                        System.out.println("Lo status message è: " + statusPayload);
                        if (statusPayload != null) {
                            String eventFormatted = getEventFormatted(key,statusPayload);
                            sseEmitter.send(SseEmitter.event().id("msg ID: " + 1).name("diagnosys").data(eventFormatted));
                        }
                    }
                else {
                    statusPayload = statusMessages.get(topic).getStatusPayload();
                    System.out.println("Lo status message è: " + statusPayload);
                    System.out.println("Status Messages Queue: " + statusMessages);
                    if (statusPayload != null) {
                        String eventFormatted = getEventFormatted(topic, statusPayload);
                        sseEmitter.send(SseEmitter.event().id("msg ID: " + 1).name("diagnosys").data(eventFormatted));
                    }
                }
            } catch (NullPointerException e) {
                System.out.println("Non ci sono messaggi di status");
            }

            //When an emitter is completed (connection was closed after a message was sent completely), removes it from multimap
            String finalTopic = topic;
            sseEmitter.onCompletion(() -> {
                emitters.remove(new Pair<String, SseEmitter>(userID, sseEmitter), finalTopic);
                System.out.println("The emitter: " + sseEmitter + " of Client: " + userID + " is completed");
            });

            return sseEmitter;
        }
        else {
            System.out.println("Già iscritto al topic");
            return null;
        }
    }

    private void sendInitEvent(SseEmitter sseEmitter, String topic) {
        try {
            //Send the topic subscribed to the client to be visualized on his page
            String topicJson = new JSONObject().put("topic", topic).toString();
            sseEmitter.send(SseEmitter.event().id("msg ID: " + 0).name("INIT").data(topicJson));
            //sseEmitter.complete(); //complete the transmission and closes the connection with clients
        } catch (IOException e){
            sseEmitter.completeWithError(e);
        }
    }

    //method to dispatch events for specific clients
    //get the HTTP request in POST from the server and dispatch the events at clients
    //@PostMapping(value = "/dispatchEvent") //Post method for not letting public the parameters in Rest-Request
    public static void dispatchEventsToClients(@RequestParam String topic, @RequestParam String payload) throws MessageHandlingException {

        for (Map.Entry<Pair<String, SseEmitter>, String> it : emitters.entries())
            System.out.println("User client attivo: " + it.getKey() + "  listening topic:  " + it.getValue());

        System.out.println("parametri: " + topic + " " + payload);

        String eventFormatted = getEventFormatted(topic,payload);
        //System.out.println("Event Formatted: " + eventFormatted);
        ArrayList<Pair<String,SseEmitter>> emittersByTopic = searchByTopic(topic);
        System.out.println("Emitter: " + emittersByTopic);

        for (Pair<String,SseEmitter> pair : emittersByTopic) { //for each emitter on the list (so each client connected with server)
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

    private static SseEmitter searchEmitter(String userID, String topic) {
        SseEmitter sseEmitter = null;
        for (Map.Entry<Pair<String, SseEmitter>, String> it : emitters.entries()){
            if (it.getValue().equals(topic) && it.getKey().getKey().equals(userID)){
                // System.out.println(it.getKey().getKey());
                sseEmitter = it.getKey().getValue();
            }
        }
        return sseEmitter;
    }

    public static void addStatusMessage(String topic, StatusMessage sm) {
        statusMessages.put(topic,sm);
    }

    public static void unsubscribeTopic(String user, String topic) {
        String topicAdjusted = MqttController.adjustJSONstringFormat(topic);
        System.out.println("topic da disiscrivere: " + topicAdjusted);
        SseEmitter sseEmitter = searchEmitter(user,topicAdjusted);
        try {
            if (sseEmitter != null) {
                System.out.println("Unsubscribing...");
                emitters.remove(new Pair<String,SseEmitter>(user, sseEmitter),topicAdjusted);
            }
            else
                System.out.println("Non si può disiscriversi da un topic di cui non si è iscritti");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static String getEventFormatted(String topic, String payload) {
        JSONObject jsonPayload = new JSONObject(payload);
        String dev = jsonPayload.getString("device");
        String ip = decryptIP(dev);
        //System.out.println("Device: " + device);
        String op = jsonPayload.getString("operation");
        if (op.equals("NONE"))
            op = " ";
        //System.out.println("Operation: " + operation);
        int fps = jsonPayload.getInt("fps");
        //System.out.println("FPS: " + fps);
        String res = jsonPayload.getString("res");
        //System.out.println("Resolution: " + resolution);
        //System.out.println("Status Messages: " + statusMessages);
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