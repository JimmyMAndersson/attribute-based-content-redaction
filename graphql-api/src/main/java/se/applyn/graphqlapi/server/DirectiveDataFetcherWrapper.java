package se.applyn.graphqlapi.server;

import graphql.execution.DataFetcherResult;
import graphql.schema.*;
import graphql.schema.idl.SchemaDirectiveWiring;
import graphql.schema.idl.SchemaDirectiveWiringEnvironment;

import java.util.*;

class DirectiveDataFetcherWrapper<DecisionContextType> implements SchemaDirectiveWiring {
    public static final String DECISION_CONTEXT_KEY = "decisionContext";

    @Override
    public GraphQLFieldDefinition onField(SchemaDirectiveWiringEnvironment<GraphQLFieldDefinition> environment) {
        GraphQLFieldDefinition field = environment.getElement();
        GraphQLFieldsContainer parentType = environment.getFieldsContainer();
        FieldCoordinates coordinates = FieldCoordinates.coordinates(parentType, field);

        DataFetcher<?> originalFetcher = environment.getCodeRegistry().getDataFetcher(parentType, field);
        DataFetcher<?> dataFetcher = DataFetcherFactories.wrapDataFetcher(originalFetcher,
                (dataFetchingEnvironment, value) -> {
                    if (value == null) {
                        return null;
                    }

                    if (!environment.getElementParentTree().getParentInfo().isPresent()) {
                        return null;
                    }

                    if (value instanceof List<?>) {
                        return handleListProperty((List<?>) value, dataFetchingEnvironment);
                    }

                    if (value != null && value.getClass().isArray()) {
                        Object[] castValue = (Object[]) value;
                        List<Object> newValue = Arrays.asList(castValue);
                        return handleListProperty(newValue, dataFetchingEnvironment);
                    }

                    return handleProperty(value, dataFetchingEnvironment);
                }
        );

        environment.getCodeRegistry().dataFetcher(coordinates, dataFetcher);

        return field;
    }

    private <T> Object handleListProperty(List<T> list, DataFetchingEnvironment environment) {
        AccessRuleEvaluator accessRuleEvaluator = environment.getGraphQlContext().get(AccessRuleEvaluator.class);
        DecisionContextType context = environment.getGraphQlContext().get(DECISION_CONTEXT_KEY);

        GraphQLNamedType type = (GraphQLNamedType) environment.getParentType();
        String typeName = type.getName();
        String propertyName = environment.getFieldDefinition().getName();

        return DataFetcherResult.<List<T>>newResult()
                .data(accessRuleEvaluator.evaluateList(context, environment.getSource(), list, typeName, propertyName))
                .build();
    }

    private Object handleProperty(Object value, DataFetchingEnvironment environment) {
        AccessRuleEvaluator accessRuleEvaluator = environment.getGraphQlContext().get(AccessRuleEvaluator.class);
        DecisionContextType context = environment.getGraphQlContext().get(DECISION_CONTEXT_KEY);

        GraphQLNamedType type = (GraphQLNamedType) environment.getParentType();
        String typeName = type.getName();
        String propertyName = environment.getFieldDefinition().getName();

        return DataFetcherResult.newResult()
                .data(accessRuleEvaluator.evaluateProperty(context, environment.getSource(), value, typeName, propertyName))
                .build();
    }
}
