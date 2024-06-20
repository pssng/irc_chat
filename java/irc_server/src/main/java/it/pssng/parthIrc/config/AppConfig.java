package it.pssng.parthIrc.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import lombok.extern.log4j.Log4j2;

@Log4j2
public class AppConfig {
    private Properties properties = new Properties();

    public AppConfig() {
        log.info("APPCONFIG instance initialized");
        try (InputStream input = getClass().getClassLoader().getResourceAsStream("application.properties")) {
            if (input == null) {
                log.error("Unable to find application.properties in classpath");
                return;
            }
            properties.load(input);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    public String getProperty(String key) {
        log.info("Retrieving key {}", key);
        return properties.getProperty(key);
    }
}
