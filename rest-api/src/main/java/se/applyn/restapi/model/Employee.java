package se.applyn.restapi.model;

import com.fasterxml.jackson.annotation.JsonAutoDetect;

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class Employee {
    public final Integer id;
    public final String firstName;
    public final String lastName;
    public final Branch branch;

    public Employee(Integer id, String firstName, String lastName, Branch branch) {
        this.id = id;
        this.firstName = firstName;
        this.lastName = lastName;
        this.branch = branch;
    }
}
