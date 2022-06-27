package se.applyn.graphqlapi.server;

import graphql.ExecutionInput;
import graphql.ExecutionResult;
import graphql.GraphQL;
import graphql.spring.web.servlet.GraphQLInvocation;
import graphql.spring.web.servlet.GraphQLInvocationData;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.WebRequest;
import se.applyn.graphqlapi.database.DatabaseConnector;
import se.applyn.graphqlapi.models.Employee;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Component
@Primary
class AccessControlInvocation implements GraphQLInvocation {
    @Value(value = "${graphql.server-type}")
    private ServerType serverType;
    @Value(value = "${graphql.database-url}")
    private String databaseURL;
    @Value(value = "${graphql.ruleset-url}")
    private String rulesetURL;
    private final GraphQL graphQL;

    AccessControlInvocation(GraphQL graphQL) { this.graphQL = graphQL; }

    @Override
    public CompletableFuture<ExecutionResult> invoke(GraphQLInvocationData invocationData, WebRequest webRequest) {
        try {
            String authParam = webRequest.getHeader("authid");
            Employee authUser;
            DatabaseConnector connector = new DatabaseConnector(databaseURL);
            if (authParam == null) {
                authUser = Employee.unauthorized();
            } else {
                Integer authid = Integer.parseInt(authParam);

                Connection conn = connector.connect();
                PreparedStatement userQuery = conn.prepareStatement("SELECT * FROM users WHERE id = ?");
                userQuery.setInt(1, authid);
                ResultSet userResult = userQuery.executeQuery();

                authUser = userResult.next() ? new Employee(userResult, true) : Employee.unauthorized();

                userResult.close();
                conn.close();
            }

            Map<?, Object> context = Map.of(
                    AccessRuleEvaluator.class, new AccessRuleEvaluator(rulesetURL),
                    DatabaseConnector.class, connector,
                    DirectiveDataFetcherWrapper.DECISION_CONTEXT_KEY, authUser
            );

            ExecutionInput input = ExecutionInput.newExecutionInput()
                    .query(invocationData.getQuery())
                    .operationName(invocationData.getOperationName())
                    .variables(invocationData.getVariables())
                    .graphQLContext(context)
                    .build();

            return graphQL.executeAsync(input);

        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }

        return null;
    }
}