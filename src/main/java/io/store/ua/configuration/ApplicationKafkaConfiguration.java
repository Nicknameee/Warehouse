package io.store.ua.configuration;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.core.*;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.JsonSerializer;

import java.util.Map;

@Configuration
@Profile("kafka")
public class ApplicationKafkaConfiguration {
    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Bean
    public KafkaTemplate<String, ?> kafkaTemplate() {
        return new KafkaTemplate<>(producerFactory());
    }

    @Bean
    public ProducerFactory<String, ?> producerFactory() {
        return new DefaultKafkaProducerFactory<>(
                Map.ofEntries(
                        Map.entry(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers),
                        Map.entry(ProducerConfig.ACKS_CONFIG, "all"),
                        Map.entry(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true),
                        Map.entry(ProducerConfig.COMPRESSION_TYPE_CONFIG, "lz4"),
                        Map.entry(ProducerConfig.RETRIES_CONFIG, 3),
                        Map.entry(ProducerConfig.RETRY_BACKOFF_MS_CONFIG, 500),
                        Map.entry(ProducerConfig.BUFFER_MEMORY_CONFIG, 33_554_432L),
                        Map.entry(ProducerConfig.LINGER_MS_CONFIG, 3_000),
                        Map.entry(ProducerConfig.DELIVERY_TIMEOUT_MS_CONFIG, 5_000),
                        Map.entry(ProducerConfig.REQUEST_TIMEOUT_MS_CONFIG, 1_000),
                        Map.entry(ProducerConfig.RECONNECT_BACKOFF_MAX_MS_CONFIG, 3_000),
                        Map.entry(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class),
                        Map.entry(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class),
                        Map.entry(JsonDeserializer.TRUSTED_PACKAGES, "*")));
    }

    @Bean
    public ConsumerFactory<String, ?> consumerFactory() {
        return new DefaultKafkaConsumerFactory<>(
                Map.ofEntries(
                        Map.entry(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers),
                        Map.entry(ConsumerConfig.GROUP_ID_CONFIG, "default"),
                        Map.entry(ConsumerConfig.ALLOW_AUTO_CREATE_TOPICS_CONFIG, true),
                        Map.entry(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false),
                        Map.entry(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest"),
                        Map.entry(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 500),
                        Map.entry(ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG, 10000),
                        Map.entry(ConsumerConfig.HEARTBEAT_INTERVAL_MS_CONFIG, 500),
                        Map.entry(ConsumerConfig.RETRY_BACKOFF_MS_CONFIG, 1000),
                        Map.entry(ConsumerConfig.RECONNECT_BACKOFF_MAX_MS_CONFIG, 3000),
                        Map.entry(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class),
                        Map.entry(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class),
                        Map.entry(JsonDeserializer.TRUSTED_PACKAGES, "*")));
    }
}
