package com.example.MQTT2SSE_BackEnd;

import com.example.MQTT2SSE_BackEnd.service.SseService;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.core.MessageProducer;
import org.springframework.integration.mqtt.core.DefaultMqttPahoClientFactory;
import org.springframework.integration.mqtt.core.MqttPahoClientFactory;
import org.springframework.integration.mqtt.inbound.MqttPahoMessageDrivenChannelAdapter;
import org.springframework.integration.mqtt.outbound.MqttPahoMessageHandler;
import org.springframework.integration.mqtt.support.DefaultPahoMessageConverter;
import org.springframework.integration.mqtt.support.MqttHeaders;
import org.springframework.messaging.*;

@Configuration
public class MqttBeans {

    public MqttPahoClientFactory mqttClientFactory() {
        DefaultMqttPahoClientFactory factory = new DefaultMqttPahoClientFactory();
        MqttConnectOptions options = new MqttConnectOptions();

        //options.setServerURIs(new String[] {"tcp://192.168.68.111:1883"}); // mosquitto local
        //options.setUserName("Gabriele05");
        //String password = "a31453";
        options.setServerURIs(new String[] {"tcp://159.69.51.171:1883"}); //TODO add credentials
        options.setUserName("terso");
        String password = "2OU5GZSB04ocdsjNDTxsK";
        options.setPassword(password.toCharArray());
        //Sets whether the client will automatically attempt to reconnect to the server if the connection is lost.
        options.setAutomaticReconnect(true);
        options.setCleanSession(true);
        options.setConnectionTimeout(10);
        String willMsg = "Client disconnected ungracefully";
        options.setWill("failTopic", willMsg.getBytes(), 2, false);

        factory.setConnectionOptions(options); //Mqtt connection options injected into factory

        return factory;
    }

    //Channel for subscribing
    @Bean
    public MessageChannel mqttInputChannel() {
        return new DirectChannel(); //the connection is point 2 point
    }

    @Bean
    public MessageProducer inBound() {
        //Topics for which the ChannelAdapter keeps listening on are only the ones in the context
        MqttPahoMessageDrivenChannelAdapter adapter = new MqttPahoMessageDrivenChannelAdapter("GabrieleBEServer",
                mqttClientFactory(), "/floud/autocounter/diag/#");
        adapter.setCompletionTimeout(5000);
        adapter.setConverter(new DefaultPahoMessageConverter());
        adapter.setQos(0);
        adapter.setOutputChannel(mqttInputChannel());

        return adapter;
    }

    @Bean
    @ServiceActivator(inputChannel = "mqttInputChannel")
    public MessageHandler handler() {
        return new MessageHandler() {
            @Override
            public void handleMessage(Message message) throws MessagingException {
                String topic = message.getHeaders().get(MqttHeaders.RECEIVED_TOPIC).toString();
                System.out.println(message.getPayload());
                String payload = message.getPayload().toString();
                try {
                    SseService.dispatchEventsToClients(topic, payload);
                    SseService.addStatusMessage(topic, new StatusMessage(payload)); //saves the message
                } catch (MessageHandlingException mhe) {
                    System.out.println("Message Handling Error");
                    mhe.printStackTrace();
                }

            }
        };
    }


    //Channel for publishing
    @Bean
    public MessageChannel mqttOutboundChannel() {
        return new DirectChannel();
    }

    @Bean
    @ServiceActivator(inputChannel = "mqttOutboundChannel")
    public MessageHandler mqttOutbound() {
        MqttPahoMessageHandler messageHandler = new MqttPahoMessageHandler("serverOut", mqttClientFactory());

        messageHandler.setAsync(true); //to let always listen the channel
        messageHandler.setAsyncEvents(true);
        messageHandler.setDefaultTopic("#");
        return messageHandler;
    }

}
