package se.applyn.graphqlapi.server;

import graphql.GraphQL;
import graphql.schema.GraphQLSchema;
import graphql.schema.idl.RuntimeWiring;
import graphql.schema.idl.SchemaGenerator;
import graphql.schema.idl.SchemaParser;
import graphql.schema.idl.TypeDefinitionRegistry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;
import se.applyn.graphqlapi.instrumentation.TimingInstrumentation;

import javax.annotation.PostConstruct;
import java.io.IOException;

@Component
public class GraphQLProvider {
    @Value(value = "${graphql.server-type}")
    private ServerType serverType;
    private GraphQL graphQL;

    @Bean
    public GraphQL graphQL() {
        return graphQL;
    }

    @PostConstruct
    public void init() throws IOException {
        GraphQLSchema graphQLSchema = buildSchema(serverType);
        this.graphQL = GraphQL
                .newGraphQL(graphQLSchema)
                .instrumentation(new TimingInstrumentation(serverType))
                .build();
    }

    private GraphQLSchema buildSchema(ServerType serverType) throws IOException {
        String sdl = serverType.getSchema();
        TypeDefinitionRegistry typeRegistry = new SchemaParser().parse(sdl);
        RuntimeWiring.Builder wiringBuilder = serverType.buildWiring();
        RuntimeWiring runtimeWiring = addWiring(wiringBuilder);
        SchemaGenerator schemaGenerator = new SchemaGenerator();
        return schemaGenerator.makeExecutableSchema(typeRegistry, runtimeWiring);
    }

    private RuntimeWiring addWiring(RuntimeWiring.Builder builder) {
        return builder.type("Query", typeWiring -> typeWiring
                        .dataFetcher("branches", GraphQLDataFetchers.getBranches())
                        .dataFetcher("employees", GraphQLDataFetchers.getEmployees())
                        .dataFetcher("branch", GraphQLDataFetchers.getBranch())
                        .dataFetcher("employee", GraphQLDataFetchers.getEmployee())
                )
                .type("Employee", typeWiring -> typeWiring
                        .dataFetcher("reportsTo", GraphQLDataFetchers.getNestedEmployee())
                        .dataFetcher("branch", GraphQLDataFetchers.getNestedBranch())
                )
                .type("Branch", typeWiring -> typeWiring
                        .dataFetcher("employees", GraphQLDataFetchers.getBranchEmployees())
                )
                .build();
    }
}

