package se.applyn.graphqlapi.models;

import com.fasterxml.jackson.annotation.JsonAutoDetect;

import java.sql.ResultSet;
import java.sql.SQLException;

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class Employee {
    public int branch;
    public int securityClearance;
    public int id;
    public String firstName;
    public String lastName;
    public String title;
    public int reportsTo;
    public int salary;
    public boolean isAuthenticated;

    public Employee(int branch, int securityClearance, int id, String firstName, String lastName, String title, int reportsTo, int salary, boolean isAuthenticated) {
        this.branch = branch;
        this.securityClearance = securityClearance;
        this.id = id;
        this.firstName = firstName;
        this.lastName = lastName;
        this.title = title;
        this.reportsTo = reportsTo;
        this.salary = salary;
        this.isAuthenticated = isAuthenticated;
    }

    public Employee(ResultSet result, boolean authenticated) throws SQLException {
        this.branch = result.getInt("branch");
        this.securityClearance = result.getInt("security_clearance");
        this.id = result.getInt("id");
        this.firstName = result.getString("first_name");
        this.lastName = result.getString("last_name");
        this.title = result.getString("title");
        this.reportsTo = result.getInt("reports_to");
        this.salary = result.getInt("salary");
        this.isAuthenticated = authenticated;
    }

    public static Employee unauthorized() {
        return new Employee(
                -1,
                -1,
                -1,
                "",
                "",
                "",
                -1,
                -1,
                false
        );
    }

    @Override
    public String toString() {
        return "User{" +
                "branch=" + branch +
                ", securityClearance=" + securityClearance +
                ", id=" + id +
                ", firstName='" + firstName + '\'' +
                ", lastName='" + lastName + '\'' +
                ", title='" + title + '\'' +
                ", reportsTo=" + reportsTo +
                ", salary=" + salary +
                ", isAuthenticated=" + isAuthenticated +
                '}';
    }
}
