package com.incident.alerthound.incident.config;

import com.incident.alerthound.incident.model.AgentTaskEvent;
import java.util.Map;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.listener.CommonErrorHandler;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.JsonSerializer;
import org.springframework.util.backoff.ExponentialBackOff;

@Configuration
public class KafkaIncidentConfig {

    @Bean
    public ConsumerFactory<String, Object> incidentConsumerFactory(KafkaProperties kafkaProperties) {
        Map<String, Object> properties = kafkaProperties.buildConsumerProperties();
        properties.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        properties.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        properties.put(JsonDeserializer.TRUSTED_PACKAGES, "com.incident.alerthound.*");
        properties.put(JsonDeserializer.USE_TYPE_INFO_HEADERS, false);
        properties.put(JsonDeserializer.VALUE_DEFAULT_TYPE, "com.incident.alerthound.detection.model.IncidentCreatedEvent");
        return new DefaultKafkaConsumerFactory<>(properties);
    }

    @Bean
    public ProducerFactory<String, AgentTaskEvent> agentTaskProducerFactory(KafkaProperties kafkaProperties) {
        Map<String, Object> properties = kafkaProperties.buildProducerProperties();
        properties.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        properties.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        properties.put(JsonSerializer.ADD_TYPE_INFO_HEADERS, false);
        return new DefaultKafkaProducerFactory<>(properties);
    }

    @Bean
    public KafkaTemplate<String, AgentTaskEvent> agentTaskKafkaTemplate(
            ProducerFactory<String, AgentTaskEvent> agentTaskProducerFactory
    ) {
        return new KafkaTemplate<>(agentTaskProducerFactory);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, Object> incidentKafkaListenerContainerFactory(
            ConsumerFactory<String, Object> incidentConsumerFactory,
            CommonErrorHandler incidentErrorHandler,
            @Value("${spring.kafka.listener.auto-startup:true}") boolean autoStartup
    ) {
        ConcurrentKafkaListenerContainerFactory<String, Object> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(incidentConsumerFactory);
        factory.setCommonErrorHandler(incidentErrorHandler);
        factory.setAutoStartup(autoStartup);
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL_IMMEDIATE);
        return factory;
    }

    @Bean
    public CommonErrorHandler incidentErrorHandler() {
        ExponentialBackOff backOff = new ExponentialBackOff();
        backOff.setInitialInterval(1000L);
        backOff.setMultiplier(2.0);
        backOff.setMaxInterval(5000L);
        backOff.setMaxElapsedTime(15000L);
        return new DefaultErrorHandler(backOff);
    }
}
