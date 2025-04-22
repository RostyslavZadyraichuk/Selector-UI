package com.zadyraichuk.general;

import java.io.*;
import java.util.Objects;
import java.util.Properties;

public class PropertiesFile {

    private volatile Properties properties;
    private File file;

    public PropertiesFile(File file) {
        Objects.requireNonNull(file);

        try {
            file.createNewFile();
            loadProperties(file);
        } catch (IOException e) {
            this.file = file;

            try {
                saveProperties();
            } catch (IOException ex) {
                throw new RuntimeException("Properties cannot be stored", ex);
            }
        }
    }

    public void loadProperties(File file) throws IOException {
        if (this.file != null && this.file.exists()) {
            saveProperties();
        }

        this.file = file;
        load();
    }

    public void saveProperties() throws IOException {
        try (OutputStream os = new FileOutputStream(file)) {
            properties.store(os, "");
        }
    }

    public String getProperty(String key) {
        return properties.getProperty(key);
    }

    public void setProperty(String key, String value) {
        properties.setProperty(key, value);
    }

    private void load() throws IOException {
        try (InputStream is = new FileInputStream(file)) {
            properties = new Properties();
            properties.load(is);
        }
    }
}
