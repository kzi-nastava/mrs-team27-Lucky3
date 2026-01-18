package com.team27.lucky3.backend.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

public class DotenvConfig implements EnvironmentPostProcessor {

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        Path envPath = Paths.get(".env");

        if (Files.exists(envPath)) {
            try {
                Map<String, Object> envMap = new HashMap<>();

                Files.readAllLines(envPath).forEach(line -> {
                    // Skip empty lines and comments
                    String trimmedLine = line.trim();
                    if (!trimmedLine.isEmpty() && !trimmedLine.startsWith("#")) {
                        int separatorIndex = trimmedLine.indexOf('=');
                        if (separatorIndex > 0) {
                            String key = trimmedLine.substring(0, separatorIndex).trim();
                            String value = trimmedLine.substring(separatorIndex + 1).trim();

                            // Remove surrounding quotes if present
                            if ((value.startsWith("\"") && value.endsWith("\"")) ||
                                (value.startsWith("'") && value.endsWith("'"))) {
                                value = value.substring(1, value.length() - 1);
                            }

                            envMap.put(key, value);
                        }
                    }
                });

                environment.getPropertySources().addFirst(new MapPropertySource("dotenvProperties", envMap));
            } catch (IOException e) {
                // Silently ignore if file cannot be read
            }
        }
    }
}

