package com.resumescreener.resumescreener.config;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JacksonConfig {

    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();

        // Fail on unknown properties to catch errors early
        mapper.configure(
                DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES,
                true
        );

        // Prevent deserialization of unknown types
        mapper.configure(
                DeserializationFeature.FAIL_ON_INVALID_SUBTYPE,
                true
        );

        // Disable default typing to prevent gadget chain attacks
        mapper.deactivateDefaultTyping();

        // Only allow safe polymorphic type handling
        mapper.activateDefaultTyping(
                BasicPolymorphicTypeValidator.builder()
                        .allowIfBaseType(Object.class)
                        .build(),
                ObjectMapper.DefaultTyping.NON_FINAL
        );

        // Disable reading from source using CodecConfigurationException
        mapper.configure(
                com.fasterxml.jackson.core.JsonParser.Feature.ALLOW_UNQUOTED_CONTROL_CHARS,
                false
        );

        // Enable strict duplicate detection
        mapper.configure(
                com.fasterxml.jackson.core.JsonParser.Feature.STRICT_DUPLICATE_DETECTION,
                true
        );

        return mapper;
    }
}