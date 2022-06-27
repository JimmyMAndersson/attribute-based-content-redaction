package se.applyn.graphqlapi.instrumentation;

import graphql.ExecutionResult;
import graphql.ExecutionResultImpl;
import graphql.execution.instrumentation.InstrumentationContext;
import graphql.execution.instrumentation.InstrumentationState;
import graphql.execution.instrumentation.SimpleInstrumentation;
import graphql.execution.instrumentation.SimpleInstrumentationContext;
import graphql.execution.instrumentation.parameters.InstrumentationExecutionParameters;
import graphql.execution.instrumentation.parameters.InstrumentationFieldFetchParameters;
import graphql.schema.DataFetcher;
import se.applyn.graphqlapi.server.ServerType;

import java.util.*;
import java.util.concurrent.CompletableFuture;

class CustomInstrumentationState implements InstrumentationState {
    List<Double> entries = new ArrayList<>();

    void recordTiming(Long time) {
        entries.add(time.doubleValue());
    }
}

public class TimingInstrumentation extends SimpleInstrumentation {
    private final ServerType serverType;

    public TimingInstrumentation(ServerType serverType) {
        super();
        this.serverType = serverType;
    }

    @Override
    public InstrumentationState createState() {
        return new CustomInstrumentationState();
    }

    @Override
    public InstrumentationContext<ExecutionResult> beginExecution(InstrumentationExecutionParameters parameters) {
        Long startNanos = System.nanoTime();
        return new SimpleInstrumentationContext<>() {
            @Override
            public void onCompleted(ExecutionResult result, Throwable t) {
                CustomInstrumentationState state = parameters.getInstrumentationState();
                state.recordTiming(System.nanoTime() - startNanos);
            }
        };
    }

    @Override
    public DataFetcher<?> instrumentDataFetcher(DataFetcher<?> dataFetcher, InstrumentationFieldFetchParameters parameters) {
        //
        // this allows you to intercept the data fetcher used to fetch a field and provide another one, perhaps
        // that enforces certain behaviours or has certain side effects on the data
        //
        return dataFetcher;
    }

    @Override
    public CompletableFuture<ExecutionResult> instrumentExecutionResult(ExecutionResult executionResult, InstrumentationExecutionParameters parameters) {
        //
        // this allows you to instrument the execution result some how.  For example the Tracing support uses this to put
        // the `extensions` map of data in place
        //
        CustomInstrumentationState state = parameters.getInstrumentationState();
        // Sum up execution times and convert them to millisecond resolution.
        Double executionTime = state.entries.stream()
                .reduce((x, y) -> x + y)
                .map(x -> x * 1e-9)
                .get();

        Map<Object, Object> oldExtensions = executionResult.getExtensions();
        LinkedHashMap<Object, Object> extensions = new LinkedHashMap<>();
        extensions.putAll(oldExtensions == null ? Collections.emptyMap() : oldExtensions);
        extensions.put("executionTime", executionTime);
        extensions.put("serverType", this.serverType);

        return CompletableFuture.completedFuture(
                new ExecutionResultImpl(
                        executionResult.getData(),
                        executionResult.getErrors(),
                        extensions
                )
        );
    }
}