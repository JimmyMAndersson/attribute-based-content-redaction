package se.applyn.graphqlapi.models;

import com.fasterxml.jackson.annotation.JsonAutoDetect;

import java.sql.ResultSet;
import java.sql.SQLException;

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class Branch {
    public int id;
    public String country;
    public String state;
    public String city;

    public Branch(int id, String country, String state, String city) {
        this.id = id;
        this.country = country;
        this.state = state;
        this.city = city;
    }

    public Branch(ResultSet rs) throws SQLException {
        this.id = rs.getInt("id");
        this.country = rs.getString("country");
        this.state = rs.getString("state");
        this.city = rs.getString("city");
    }
}
