package com.whiker.learn.mysql;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.DriverManager;

public class ConnectionManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(ConnectionManager.class);

    private static final ThreadLocal<Connection> CONNECTION_HOLDER =
            new ThreadLocal<Connection>() {
                @Override
                protected Connection initialValue() {
                    try {
                        Class.forName("com.mysql.jdbc.Driver").newInstance();

                        MysqlProperties properties = MysqlProperties.getInstance();

                        return DriverManager.getConnection(
                                "jdbc:mysql://" + properties.getHost() + "/" + properties.getDatabase(),
                                properties.getUser(), properties.getPassword());
                    } catch (Exception e) {
                        LOGGER.error("", e);
                        return null;
                    }
                }
            };

    public static Connection getConnection() {
        return CONNECTION_HOLDER.get();
    }
}
