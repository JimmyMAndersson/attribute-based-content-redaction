package se.applyn.graphqlapi.server;

import graphql.schema.DataFetcher;
import org.springframework.stereotype.Component;
import se.applyn.graphqlapi.database.DatabaseConnector;
import se.applyn.graphqlapi.models.Branch;
import se.applyn.graphqlapi.models.Employee;

import java.sql.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;

@Component
public class GraphQLDataFetchers {
    private GraphQLDataFetchers() {}

    public static DataFetcher<CompletableFuture<List<Branch>>> getBranches() {
        return environment -> CompletableFuture.supplyAsync(() -> {
            try {
                DatabaseConnector db = environment.getGraphQlContext().get(DatabaseConnector.class);
                Connection conn = db.connect();
                PreparedStatement s = conn.prepareStatement("SELECT * FROM branches");
                ResultSet rs = s.executeQuery();
                ArrayList<Branch> branches = new ArrayList<>();

                while (rs.next()) {
                    int id = rs.getInt("id");
                    String country = rs.getString("country");
                    String state = rs.getString("state");
                    String city = rs.getString("city");

                    branches.add(new Branch(id, country, state, city));
                }

                s.close();
                rs.close();
                conn.close();
                return branches;
            } catch (SQLException e) {
                e.printStackTrace();
                return null;
            }
        });
    }

    public static DataFetcher<CompletableFuture<Branch>> getBranch() {
        return environment -> CompletableFuture.supplyAsync(() -> {
            try {
                DatabaseConnector db = environment.getGraphQlContext().get(DatabaseConnector.class);
                Connection conn = db.connect();
                Integer branchID = environment.getArgument("id");
                PreparedStatement s = conn.prepareStatement("SELECT * FROM branches WHERE id = ?");
                s.setInt(1, branchID);
                ResultSet rs = s.executeQuery();
                Branch branch = null;

                if (rs.next()) {
                    int id = rs.getInt("id");
                    String country = rs.getString("country");
                    String state = rs.getString("state");
                    String city = rs.getString("city");

                    branch = new Branch(id, country, state, city);
                }

                s.close();
                rs.close();
                conn.close();
                return branch;
            } catch (SQLException e) {
                e.printStackTrace();
                return null;
            }
        });
    }

    public static DataFetcher<CompletableFuture<List<Employee>>> getBranchEmployees() {
        return environment -> CompletableFuture.supplyAsync(() -> {
            try {
                DatabaseConnector db = environment.getGraphQlContext().get(DatabaseConnector.class);
                Connection conn = db.connect();

                Integer branchID = ((Branch) environment.getSource()).id;
                PreparedStatement s = conn.prepareStatement("SELECT * FROM users where branch = ?");
                s.setInt(1, branchID);
                ResultSet rs = s.executeQuery();

                List<Employee> employees = new ArrayList<>();

                while (rs.next()) {
                    employees.add(new Employee(rs, false));
                }

                s.close();
                rs.close();
                conn.close();
                return employees;
            } catch (SQLException e) {
                e.printStackTrace();
                return null;
            }
        });
    }

    public static DataFetcher<CompletableFuture<Employee[]>> getEmployees() {
        return environment -> CompletableFuture.supplyAsync(() -> {
            try {
                DatabaseConnector db = environment.getGraphQlContext().get(DatabaseConnector.class);
                Connection conn = db.connect();
                PreparedStatement countStatement = conn.prepareStatement("SELECT COUNT(*) FROM users");
                ResultSet countSet = countStatement.executeQuery();
                countSet.next();
                int count = countSet.getInt(1);
                countStatement.close();

                PreparedStatement s = conn.prepareStatement("SELECT * FROM users");
                ResultSet rs = s.executeQuery();

                Employee[] employees = new Employee[count];
                int index = 0;
                while (rs.next()) {
                    employees[index] = new Employee(rs, false);
                    index += 1;
                }

                countSet.close();
                s.close();
                rs.close();
                conn.close();
                return employees;
            } catch (SQLException e) {
                e.printStackTrace();
                return null;
            }
        });
    }

    public static DataFetcher<CompletableFuture<Employee>> getNestedEmployee() {
        return environment -> CompletableFuture.supplyAsync(() -> {
            try {
                DatabaseConnector db = environment.getGraphQlContext().get(DatabaseConnector.class);
                Connection conn = db.connect();
                Integer userid = ((Employee) environment.getSource()).reportsTo;

                PreparedStatement s = conn.prepareStatement("SELECT * FROM users WHERE id = ?");
                s.setInt(1, userid);
                ResultSet rs = s.executeQuery();

                Employee user = rs.next() ? new Employee(rs, false) : null;

                s.close();
                rs.close();
                conn.close();
                return user;
            } catch (SQLException e) {
                e.printStackTrace();
                return null;
            }
        });
    }

    public static DataFetcher<CompletableFuture<Employee>> getEmployee() {
        return environment -> CompletableFuture.supplyAsync(() -> {
            try {
                DatabaseConnector db = environment.getGraphQlContext().get(DatabaseConnector.class);
                Connection conn = db.connect();
                Integer userid = environment.getArgument("id");

                PreparedStatement s = conn.prepareStatement("SELECT * FROM users WHERE id = ?");
                s.setInt(1, userid);
                ResultSet rs = s.executeQuery();

                Employee user = rs.next() ? new Employee(rs, false) : null;

                s.close();
                rs.close();
                conn.close();
                return user;
            } catch (SQLException e) {
                e.printStackTrace();
                return null;
            }
        });
    }

    public static DataFetcher<CompletableFuture<Branch>> getNestedBranch() {
        return environment -> CompletableFuture.supplyAsync(() -> {
            try {
                DatabaseConnector db = environment.getGraphQlContext().get(DatabaseConnector.class);
                Connection conn = db.connect();
                Integer branchid = ((Employee) environment.getSource()).branch;

                PreparedStatement s = conn.prepareStatement("SELECT * FROM branches WHERE id = ?");
                s.setInt(1, branchid);
                ResultSet rs = s.executeQuery();

                Branch branch = rs.next() ? new Branch(rs) : null;

                s.close();
                rs.close();
                conn.close();
                return branch;
            } catch (SQLException e) {
                e.printStackTrace();
                return null;
            }
        });
    }
}
