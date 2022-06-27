package se.applyn.restapi.database;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseConnector {
    private Path databaseURL;

    public DatabaseConnector(String databaseURL) {
        this.databaseURL = Paths.get(databaseURL).toAbsolutePath().normalize();
    }

    public Connection connect() throws SQLException {
        String url = String.format("jdbc:sqlite:" + databaseURL);
        return connect(url);
    }

    public Connection connect(String url) throws SQLException {
        return DriverManager.getConnection(url);
    }
}
