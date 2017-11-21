package com.whiker.learn.mysql;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

public class MysqlProperties {
    private static final Logger LOGGER = LoggerFactory.getLogger(ConnectionManager.class);

    private static MysqlProperties Instance = new MysqlProperties();

    public static MysqlProperties getInstance() {
        return Instance;
    }

    Properties mProperties = new Properties();

    private MysqlProperties() {
        String path = this.getClass().getResource("/mysql.properties").getPath();

        try {
            mProperties.load(new FileInputStream(path));
        } catch (IOException e) {
            LOGGER.error("", e);
        }
    }

    public String getHost() {
        return mProperties.getProperty("host");
    }

    public String getDatabase() {
        return mProperties.getProperty("database");
    }

    public String getUser() {
        return mProperties.getProperty("user");
    }

    public String getPassword() {
        return mProperties.getProperty("password");
    }
}
