package se.applyn.restapi.model;

import com.fasterxml.jackson.annotation.JsonAutoDetect;

import java.util.List;

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class Branch {
    public final Integer id;
    public final String country;
    public final String state;
    public final String city;

    public Branch(Integer id, String country, String state, String city) {
        this.id = id;
        this.country = country;
        this.state = state;
        this.city = city;
    }
}
