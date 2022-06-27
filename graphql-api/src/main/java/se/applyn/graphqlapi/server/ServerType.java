package se.applyn.graphqlapi.server;

import com.google.common.io.Resources;
import graphql.com.google.common.base.Charsets;
import graphql.schema.idl.RuntimeWiring;
import se.applyn.graphqlapi.models.Employee;

import java.io.IOException;
import java.net.URL;

public enum ServerType {
    NO_AC,
    ABAC_REDACTION,
    NO_AC_TEST,
    ABAC_REDACTION_TEST;

    private String getSchemaPath() {
        return "schema.graphqls";
    }

    public String getSchema() throws IOException {
        String schemaPath = this.getSchemaPath();
        URL url = Resources.getResource(schemaPath);
        return Resources.toString(url, Charsets.UTF_8);
    }

    public RuntimeWiring.Builder buildWiring() {
        return switch (this) {
            case ABAC_REDACTION, ABAC_REDACTION_TEST -> RuntimeWiring.newRuntimeWiring()
                    .directiveWiring(new DirectiveDataFetcherWrapper<Employee>());

            default -> RuntimeWiring.newRuntimeWiring();
        };
    }

    public String getDatabase() {
        return switch (this) {
            case ABAC_REDACTION, NO_AC -> "data.db";
            case ABAC_REDACTION_TEST, NO_AC_TEST -> "test.db";
        };
    }
}