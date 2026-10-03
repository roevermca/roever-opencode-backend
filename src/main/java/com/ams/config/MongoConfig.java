package com.ams.config;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.mongo.MongoConnectionDetails;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.mongodb.MongoDatabaseFactory;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.SimpleMongoClientDatabaseFactory;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;

@Configuration
public class MongoConfig {

    private static final Logger log = LoggerFactory.getLogger(MongoConfig.class);

    @Value("${spring.data.mongodb.uri:${MONGODB_URI:mongodb+srv://admin:Password123@cluster0.dxhx0zh.mongodb.net/ams_db?appName=Cluster0}}")
    private String rawUri;

    @Value("${spring.data.mongodb.database:${MONGODB_DATABASE:ams_db}}")
    private String database;

    /**
     * Sanitizes MongoDB connection string:
     * 1. Strips accidental quotes or spaces.
     * 2. Strips accidental < > angle brackets around password.
     * 3. Automatically URL-encodes special characters in password (e.g. '@' -> '%40', ':' -> '%3A').
     */
    public static String sanitizeMongoUri(String uri) {
        if (uri == null || uri.isBlank()) {
            return "mongodb+srv://admin:Password123@cluster0.dxhx0zh.mongodb.net/ams_db?appName=Cluster0";
        }
        String clean = uri.trim();
        // Remove surrounding quotes if entered like "..." or '...'
        if ((clean.startsWith("\"") && clean.endsWith("\"")) || (clean.startsWith("'") && clean.endsWith("'"))) {
            clean = clean.substring(1, clean.length() - 1).trim();
        }

        try {
            // Match: mongodb(+srv)://<user>:<password>@<hostAndRest>
            int protocolEnd = clean.indexOf("://");
            if (protocolEnd != -1) {
                String protocol = clean.substring(0, protocolEnd + 3);
                String remainder = clean.substring(protocolEnd + 3);

                int lastAt = remainder.lastIndexOf('@');
                if (lastAt != -1) {
                    String userInfo = remainder.substring(0, lastAt);
                    String hostAndRest = remainder.substring(lastAt + 1);

                    int firstColon = userInfo.indexOf(':');
                    if (firstColon != -1) {
                        String username = userInfo.substring(0, firstColon);
                        String password = userInfo.substring(firstColon + 1);

                        // Strip accidental < > brackets around password
                        if (password.startsWith("<") && password.endsWith(">") && password.length() > 2) {
                            password = password.substring(1, password.length() - 1);
                        }

                        // If password contains raw '@', encode to '%40'
                        if (password.contains("@") && !password.contains("%40")) {
                            password = password.replace("@", "%40");
                        }
                        // If password contains raw ':', encode to '%3A'
                        if (password.contains(":") && !password.contains("%3A")) {
                            password = password.replace(":", "%3A");
                        }
                        // If password contains raw '#', encode to '%23'
                        if (password.contains("#") && !password.contains("%23")) {
                            password = password.replace("#", "%23");
                        }

                        clean = protocol + username + ":" + password + "@" + hostAndRest;
                    }
                }
            }
        } catch (Exception ex) {
            log.warn("Could not auto-sanitize Mongo URI: {}", ex.getMessage());
        }

        return clean;
    }

    @Bean
    @Primary
    public MongoConnectionDetails mongoConnectionDetails() {
        return new MongoConnectionDetails() {
            @Override
            public ConnectionString getConnectionString() {
                String sanitized = sanitizeMongoUri(rawUri);
                log.info("MongoConnectionDetails using sanitized URI");
                return new ConnectionString(sanitized);
            }
        };
    }

    @Bean
    @Primary
    public MongoClient mongoClient() {
        String sanitized = sanitizeMongoUri(rawUri);
        log.info("Connecting to MongoDB with sanitized connection string...");
        ConnectionString connString = new ConnectionString(sanitized);
        MongoClientSettings settings = MongoClientSettings.builder()
                .applyConnectionString(connString)
                .build();
        return MongoClients.create(settings);
    }

    @Bean
    @Primary
    public MongoDatabaseFactory mongoDatabaseFactory(MongoClient mongoClient) {
        String dbName = database;
        try {
            ConnectionString cs = new ConnectionString(sanitizeMongoUri(rawUri));
            if (cs.getDatabase() != null && !cs.getDatabase().isBlank()) {
                dbName = cs.getDatabase();
            }
        } catch (Exception ignored) {
        }
        return new SimpleMongoClientDatabaseFactory(mongoClient, dbName != null && !dbName.isBlank() ? dbName : "ams_db");
    }

    @Bean
    @Primary
    public MongoTemplate mongoTemplate(MongoDatabaseFactory mongoDatabaseFactory) {
        return new MongoTemplate(mongoDatabaseFactory);
    }
}
