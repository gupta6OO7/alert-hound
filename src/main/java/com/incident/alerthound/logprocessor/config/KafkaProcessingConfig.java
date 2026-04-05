package com.incident.alerthound.logprocessor.config;

import com.incident.alerthound.config.AlertHoundProperties;
import com.incident.alerthound.logprocessor.model.InvalidLogEvent;
import com.incident.alerthound.logprocessor.model.StructuredLog;
import com.incident.alerthound.logprocessor.service.LogTransformationException;
import java.util.Map;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.listener.CommonErrorHandler;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.JsonSerializer;
import org.springframework.util.backoff.ExponentialBackOff;

@Configuration
public class KafkaProcessingConfig {

    @Bean
    public ConsumerFactory<String, Object> kafkaConsumerFactory(KafkaProperties kafkaProperties) {
        Map<String, Object> properties = kafkaProperties.buildConsumerProperties();
        properties.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        properties.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        properties.put(JsonDeserializer.TRUSTED_PACKAGES, "com.incident.alerthound.*");
        properties.put(JsonDeserializer.USE_TYPE_INFO_HEADERS, false);
        properties.put(JsonDeserializer.VALUE_DEFAULT_TYPE, "com.incident.alerthound.logingestion.model.LogEvent");

        return new DefaultKafkaConsumerFactory<>(properties);
    }

    @Bean
    public ProducerFactory<String, StructuredLog> processedLogProducerFactory(KafkaProperties kafkaProperties) {
        Map<String, Object> properties = kafkaProperties.buildProducerProperties();
        properties.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        properties.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        properties.put(JsonSerializer.ADD_TYPE_INFO_HEADERS, false);

        return new DefaultKafkaProducerFactory<>(properties);
    }

    @Bean
    public ProducerFactory<String, InvalidLogEvent> invalidLogProducerFactory(KafkaProperties kafkaProperties) {
        Map<String, Object> properties = kafkaProperties.buildProducerProperties();
        properties.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        properties.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        properties.put(JsonSerializer.ADD_TYPE_INFO_HEADERS, false);

        return new DefaultKafkaProducerFactory<>(properties);
    }

    @Bean
    public ProducerFactory<String, Object> deadLetterProducerFactory(KafkaProperties kafkaProperties) {
        Map<String, Object> properties = kafkaProperties.buildProducerProperties();
        properties.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        properties.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        properties.put(JsonSerializer.ADD_TYPE_INFO_HEADERS, false);

        return new DefaultKafkaProducerFactory<>(properties);
    }

    @Bean
    public KafkaTemplate<String, StructuredLog> processedLogKafkaTemplate(ProducerFactory<String, StructuredLog> processedLogProducerFactory) {
        return new KafkaTemplate<>(processedLogProducerFactory);
    }

    @Bean
    public KafkaTemplate<String, InvalidLogEvent> invalidLogKafkaTemplate(ProducerFactory<String, InvalidLogEvent> invalidLogProducerFactory) {
        return new KafkaTemplate<>(invalidLogProducerFactory);
    }

    @Bean
    public KafkaTemplate<String, Object> deadLetterKafkaTemplate(ProducerFactory<String, Object> deadLetterProducerFactory) {
        return new KafkaTemplate<>(deadLetterProducerFactory);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, Object> logProcessorKafkaListenerContainerFactory(
            ConsumerFactory<String, Object> kafkaConsumerFactory,
            CommonErrorHandler commonErrorHandler,
            @Value("${spring.kafka.listener.auto-startup:true}") boolean autoStartup
    ) {
        ConcurrentKafkaListenerContainerFactory<String, Object> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(kafkaConsumerFactory);
        factory.setCommonErrorHandler(commonErrorHandler);
        factory.setAutoStartup(autoStartup);
        return factory;
    }

    @Bean
    public CommonErrorHandler commonErrorHandler(
            KafkaTemplate<String, Object> deadLetterKafkaTemplate,
            AlertHoundProperties properties
    ) {
        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(
                deadLetterKafkaTemplate,
                (record, exception) -> new TopicPartition(resolveFailedTopic(properties), record.partition())
        );

        ExponentialBackOff backOff = new ExponentialBackOff();
        backOff.setInitialInterval(1000L);
        backOff.setMultiplier(2.0);
        backOff.setMaxInterval(5000L);
        backOff.setMaxElapsedTime(8000L);

        DefaultErrorHandler errorHandler = new DefaultErrorHandler(recoverer, backOff);
        errorHandler.addNotRetryableExceptions(LogTransformationException.class);
        return errorHandler;
    }

    private String resolveFailedTopic(AlertHoundProperties properties) {
        if (properties == null
                || properties.kafka() == null
                || properties.kafka().topics() == null
                || properties.kafka().topics().logsFailed() == null
                || properties.kafka().topics().logsFailed().name() == null
                || properties.kafka().topics().logsFailed().name().isBlank()) {
            return "logs.failed";
        }

        return properties.kafka().topics().logsFailed().name();
    }
}
