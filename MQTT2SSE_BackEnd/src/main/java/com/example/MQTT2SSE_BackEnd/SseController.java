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
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@RestController
public class SseController {

    public static Multimap<Pair<String,SseEmitter>,String> emitters = ArrayListMultimap.create(); //K: <UserID,Emitter> V: Topic

    //This map contains the last message sent by the broker for each topic
    public static Map<String,String> statusMessages = new HashMap<String, String>();
    private static int msgID = 2;

    // method for client subscription, permits the connection releasing a SSE-emitter
    // that allows clients to listen the specified channel for receiving the events from the server-side
    //FIXME HTTP/1.1 permits max 6 connections with the same host
    @CrossOrigin
    @RequestMapping(value = "/subscribe", consumes = MediaType.ALL_VALUE) //define the endpoint for the REST request
    public SseEmitter subscribe(@RequestParam String userID, @RequestParam String topic) throws IOException {
        //If the client is not subscribed already to topic, creates a new tupla with associated emitter
        if (searchEmitter(userID, topic) == null) {
        //if (searchEmitter(userID) == null) {
            //SseEmitter is the object that holds the connection between Client and Server
            SseEmitter sseEmitter = new SseEmitter(Long.MAX_VALUE); //the time in millis before the emitter get timed-out
            sendInitEvent(sseEmitter, topic);
            emitters.put(new Pair<String, SseEmitter>(userID, sseEmitter), topic);

            //FIXME stesso problema: il Topic della sub (/floud/../#) non corrisponde a quello che arriva dal broker
            // (ex. /floud/../a45ed2d1)
            String statusPayload = statusMessages.get(topic);
            System.out.println("Lo status message è: " + statusPayload);
            System.out.println("Status Messages Queue: " + statusMessages);

            if (statusPayload != null) {
                String eventFormatted = getEventFormatted(topic,statusPayload);
                sseEmitter.send(SseEmitter.event().id("msg ID: " + 1).name("latestNews").data(eventFormatted));
            }

            //emitters.put(userID, sseEmitter);
            //emitters.add(sseEmitter);
            //lambda func to remove emitters already completed from the list
            sseEmitter.onCompletion(() -> { //FIXME non funziona più (dovuto alla singola conn. TCP di HTTP/2?)
                emitters.remove(new Pair<String, SseEmitter>(userID, sseEmitter), topic);
                System.out.println("The emitter: " + sseEmitter + " of Client: " + userID + " is completed");
            });
            //FIXME funziona bene in tutti i casi, tranne qualche volta quando disconnetto più client contemporaneamente
            //sseEmitter.onCompletion(() -> emitters.remove(sseEmitter));
            /*sseEmitter.onError((e) -> {
            //emitters.remove(new Pair<String, SseEmitter>(userID, sseEmitter), topic);
            System.out.println("The emitter: " + sseEmitter + " of Client: " + userID + " had an error");
            });*/
            /*sseEmitter.onTimeout(() -> {
                //emitters.remove(new Pair<String, SseEmitter>(userID, sseEmitter), topic);
                System.out.println("The emitter: " + sseEmitter + " of Client: " + userID + " went in timeout");
            });*/
            //FIXME funziona bene se disconnetto un client, ma se poi lo riconnetto e disconnetto un altro contemp esplode
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
                emitter.send(SseEmitter.event().id("msg ID: " + msgID++).name("latestNews").data(eventFormatted));
            } catch (IOException e) {
                System.out.println("Dispatcher Error");
            }
        }
    }

    private static ArrayList<Pair<String,SseEmitter>> searchByTopic(String topic) {
        ArrayList<Pair<String,SseEmitter>> emitter = new ArrayList<Pair<String, SseEmitter>>();
        for (Map.Entry<Pair<String, SseEmitter>, String> it : emitters.entries()){
            if (it.getValue().equals(topic)){
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

    /*private static SseEmitter searchEmitter(String userID) {
        SseEmitter sseEmitter = null;
        for (Map.Entry<Pair<String, SseEmitter>, String> it : emitters.entries()){
            if (it.getKey().getKey().equals(userID)){
                // System.out.println(it.getKey().getKey());
                sseEmitter = it.getKey().getValue();
            }
        }
        return sseEmitter;
    }*/

    public static void addStatusMessage(String topic, String payload) {
        statusMessages.put(topic,payload);
    }

    public static void unsubscribeTopic(String user, String topic) {
        //SseEmitter sseEmitter = searchEmitter(user,topic);
        String topicWithSlashes = topic.replace("-","/"); //FIXME soluzione tampone
        System.out.println("topic da disiscrivere: " + topicWithSlashes);
        SseEmitter sseEmitter = searchEmitter(user,topicWithSlashes);
        try {
            if (sseEmitter != null) {
                System.out.println("Unsubscribing...");
                emitters.remove(new Pair<String,SseEmitter>(user, sseEmitter),topicWithSlashes);
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

        //stringify the JSON msg
        return new JSONObject().put("title", topic).put("device", ip)
                .put("operation", op).put("fps", fps).put("resolution", res).toString();
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