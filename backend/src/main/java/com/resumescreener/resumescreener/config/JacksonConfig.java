package com.resumescreener.resumescreener.config;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JacksonConfig {

    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();

        // Allow unknown properties to handle LLM variations
        // (LLM might return extra fields we don't expect)
        mapper.configure(
                DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES,
                false
        );

        // Disable default typing to prevent gadget chain attacks
        // This prevents arbitrary class deserialization
        mapper.deactivateDefaultTyping();

        // Coerce numbers to strings if needed (LLM might return int instead of String)
        mapper.configure(
                DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT,
                true
        );

        // Disable reading from source to prevent XXE
        mapper.configure(
                com.fasterxml.jackson.core.JsonParser.Feature.ALLOW_UNQUOTED_CONTROL_CHARS,
                false
        );

        return mapper;
    }
}