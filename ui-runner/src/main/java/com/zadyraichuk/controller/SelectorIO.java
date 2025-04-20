package com.zadyraichuk.controller;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import com.fasterxml.jackson.databind.jsontype.PolymorphicTypeValidator;
import com.zadyraichuk.selector.AbstractRandomSelector;
import com.zadyraichuk.selector.RandomSelector;
import com.zadyraichuk.selector.RationalRandomSelector;
import com.zadyraichuk.selector.Selector;
import com.zadyraichuk.variant.*;
import java.io.*;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class SelectorIO {

    private static final ObjectMapper objectMapper = createObjectMapper();

    private static ObjectMapper createObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();

        // Enable polymorphic type handling to support multiple selector types
        PolymorphicTypeValidator ptv = BasicPolymorphicTypeValidator.builder()
            .allowIfSubType(Selector.class)
            .allowIfSubType(AbstractRandomSelector.class)
            .allowIfSubType(RandomSelector.class)
            .allowIfSubType(RationalRandomSelector.class)
            .allowIfSubType(VariantsCollection.class)
            .allowIfSubType(AbstractVariantsList.class)
            .allowIfSubType(VariantsList.class)
            .allowIfSubType(RationalVariantsList.class)
            .allowIfSubType(List.class)
            .allowIfSubType(ArrayList.class)
            .allowIfSubType(VariantColor[].class)
            .build();

        mapper.activateDefaultTyping(ptv, ObjectMapper.DefaultTyping.NON_FINAL);
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        return mapper;
    }

    public static AbstractRandomSelector<String, ? extends Variant<String>> read(Path path)
        throws IOException {
        File file = path.toFile();

        if (!file.exists() || file.length() == 0) {
            throw new IOException("No object inside selector file or file does not exist: " + path);
        }

        return objectMapper.readValue(file, AbstractRandomSelector.class);
    }

    public static void write(AbstractRandomSelector<String, ? extends Variant<String>> selector, Path path)
        throws IOException {
        File file = path.toFile();

        // Ensure the selector is properly initialized before saving
        selector.getVariantsList().initVariantPercents();

        objectMapper.writeValue(file, selector);
    }

    public static void delete(Path path) {
        File file = path.toFile();
        if (file.exists()) {
            file.delete();
        }
    }

// TODO create some templates + constructor

}
