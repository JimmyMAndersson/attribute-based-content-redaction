package se.applyn.restapi.server;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import se.applyn.restapi.database.DatabaseConnector;
import se.applyn.restapi.model.Branch;
import se.applyn.restapi.model.Employee;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

@RestController
public class BranchesController {
    @Value(value = "${rest-api.database-path}")
    private String databasePath;

    @GetMapping("/branches")
    public List<Branch> branches() throws SQLException {
        Connection conn = new DatabaseConnector(databasePath).connect();
        List<Branch> result = new ArrayList<>();

        PreparedStatement statement = conn
                .prepareStatement("SELECT * FROM branches");

        ResultSet resultSet = statement.executeQuery();

        while (resultSet.next()) {
            Integer id = resultSet.getInt("id");
            String country = resultSet.getString("country");
            String state = resultSet.getString("state");
            String city = resultSet.getString("city");
            result.add(new Branch(id, country, state, city));
        }

        statement.close();
        resultSet.close();
        conn.close();
        return result;
    }

    @GetMapping("/branches/{id}")
    public Branch branch(@PathVariable("id") Integer id) throws SQLException {
        Connection conn = new DatabaseConnector(databasePath).connect();
        Branch result = null;

        PreparedStatement statement = conn
                .prepareStatement("SELECT * from branches where id = ?");
        statement.setInt(1, id);
        ResultSet resultSet = statement.executeQuery();

        if (resultSet.next()) {
            Integer branchID = resultSet.getInt("id");
            String country = resultSet.getString("country");
            String state = resultSet.getString("state");
            String city = resultSet.getString("city");
            result = new Branch(branchID, country, state, city);
        }

        statement.close();
        resultSet.close();
        conn.close();
        return result;
    }

    @GetMapping("/branches/{id}/employees")
    public List<Employee> branchEmployees(@PathVariable("id") Integer id) throws SQLException {
        Connection conn = new DatabaseConnector(databasePath).connect();
        List<Employee> result = new ArrayList<>();

        PreparedStatement statement = conn
                .prepareStatement("SELECT users.id as userid, first_name, last_name, branches.id as branchid, country, state, city FROM users LEFT OUTER JOIN branches ON users.branch = branches.id WHERE branch = ?");
        statement.setInt(1, id);
        ResultSet resultSet = statement.executeQuery();

        while (resultSet.next()) {
            Integer employeeid = resultSet.getInt("userid");
            String firstName = resultSet.getString("first_name");
            String lastName = resultSet.getString("last_name");
            Integer branchID = resultSet.getInt("branchid");
            String country = resultSet.getString("country");
            String state = resultSet.getString("state");
            String city = resultSet.getString("city");
            result.add(new Employee(employeeid, firstName, lastName, new Branch(branchID, country, state, city)));
        }

        statement.close();
        resultSet.close();
        conn.close();
        return result;
    }
}
