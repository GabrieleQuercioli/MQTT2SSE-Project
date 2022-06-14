package com.example.MQTT2SSE_BackEnd;

import org.springframework.integration.annotation.MessagingGateway;
import org.springframework.integration.mqtt.support.MqttHeaders;
import org.springframework.messaging.handler.annotation.Header;

// Interface that exposes a method that allows to send messages to a MQTT Broker
@MessagingGateway(defaultRequestChannel = "mqttOutboundChannel")
public interface MqttGateway {

    void sendToMqtt(String data, @Header(MqttHeaders.TOPIC) String topic);
}
