package se.applyn.graphqlapi.server;

import graphql.com.google.common.base.Charsets;
import org.json.*;
import org.projectnessie.cel.checker.Decls;
import org.projectnessie.cel.tools.Script;
import org.projectnessie.cel.tools.ScriptException;
import org.projectnessie.cel.tools.ScriptHost;
import org.projectnessie.cel.types.jackson.JacksonRegistry;
import org.sqlite.util.StringUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;

public class AccessRuleEvaluator {
    private final JSONObject ruleSet;
    private final Map<String, Map<String, Map<Policy, Script>>> scriptMap = new ConcurrentHashMap<>();
    private final ScriptHost scriptHost = ScriptHost.newBuilder()
            .registry(JacksonRegistry.newRegistry())
            .build();

    AccessRuleEvaluator(String ruleSetPath) throws IOException {
        String rules = readRuleFile(ruleSetPath);
        this.ruleSet = new JSONObject(rules);
    }

    private String readRuleFile(String path) throws IOException {
        Path filePath = Paths.get(path);
        return StringUtils.join(Files.readAllLines(filePath, Charsets.UTF_8), "");
    }

    public <U, T, V> List<T> evaluateList(U user, V parent, List<T> value, String typeName, String propertyName) {
        try {
            List<T> list = evaluateProperty(user, parent, value, typeName, propertyName);

            if (list == null)
                return null;

            CollectionPolicy policy = getCollectionPolicy(typeName, propertyName);

            if (policy == CollectionPolicy.FULL || list.size() == 0) {
                return list;
            }


            Script script = getPreparedScript(typeName, propertyName, Policy.ELEMENT_FILTER, host -> {
                try {
                    String filter = getPolicy(Policy.ELEMENT_FILTER, typeName, propertyName);
                    if (parent == null) {
                        return host.buildScript(filter)
                                .withDeclarations(
                                        Decls.newVar("user", Decls.newObjectType(user.getClass().getName())),
                                        Decls.newVar("element", Decls.newObjectType(list.get(0).getClass().getName()))
                                )
                                .withTypes(user.getClass(), list.get(0).getClass())
                                .build();
                    } else {
                        return host.buildScript(filter)
                                .withDeclarations(
                                        Decls.newVar("user", Decls.newObjectType(user.getClass().getName())),
                                        Decls.newVar("object", Decls.newObjectType(parent.getClass().getName())),
                                        Decls.newVar("element", Decls.newObjectType(list.get(0).getClass().getName()))
                                )
                                .withTypes(user.getClass(), parent.getClass(), list.get(0).getClass())
                                .build();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    System.exit(1);
                    return null;
                }
            });

            return switch (policy) {
                case REDACTED -> list.parallelStream()
                        .map(x -> {
                            try {
                                if (parent == null) {
                                    return script.execute(Boolean.class, Map.of("user", user, "element", x)) ? x : null;
                                } else {
                                    return script.execute(Boolean.class, Map.of("user", user, "object", parent, "element", x)) ? x : null;
                                }
                            } catch (ScriptException e) {
                                return null;
                            }
                        })
                        .collect(Collectors.toList());

                case PARTIAL -> list.parallelStream()
                        .filter(x -> {
                            try {
                                if (parent == null) {
                                    return script.execute(Boolean.class, Map.of("user", user, "element", x));
                                } else {
                                    return script.execute(Boolean.class, Map.of("user", user, "object", parent, "element", x));
                                }
                            } catch (ScriptException e) {
                                return false;
                            }
                        })
                        .collect(Collectors.toList());

                default -> list;
            };
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
            return null;
        }
    }

    public <U, T, V> T evaluateProperty(U user, V parent, T property, String typeName, String propertyName) {
        String filter = getPolicy(Policy.READ, typeName, propertyName);
        Script script = getPreparedScript(typeName, propertyName, Policy.READ, host -> {
            try {
                if (parent == null) {
                    return host.buildScript(filter)
                            .withDeclarations(
                                    Decls.newVar("user", Decls.newObjectType(user.getClass().getName())),
                                    Decls.newVar("property", Decls.newObjectType(property.getClass().getName()))
                            )
                            .withTypes(user.getClass(), property.getClass())
                            .build();
                } else {
                    return host.buildScript(filter)
                            .withDeclarations(
                                    Decls.newVar("user", Decls.newObjectType(user.getClass().getName())),
                                    Decls.newVar("object", Decls.newObjectType(parent.getClass().getName())),
                                    Decls.newVar("property", Decls.newObjectType(property.getClass().getName()))
                            )
                            .withTypes(user.getClass(), parent.getClass(), property.getClass())
                            .build();
                }

            } catch (Exception e) {
                e.printStackTrace();
                System.exit(1);
                return null;
            }
        });

        Map<String, Object> arguments;

        if (parent == null) {
            arguments = Map.of("user", user, "property", property);
        } else {
            arguments = Map.of("user", user, "object", parent, "property", property);
        }


        try {
            return script.execute(Boolean.class, arguments) ? property : null;
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
            return null;
        }
    }

    synchronized private Script getPreparedScript(String typeName, String propertyName, Policy policy, Function<ScriptHost, Script> missingScriptBuilder) {
        if (scriptMap.containsKey(typeName) && scriptMap.get(typeName).containsKey(propertyName) && scriptMap.get(typeName).get(propertyName).containsKey(policy)) {
            return scriptMap.get(typeName).get(propertyName).get(policy);
        }

        Script missingScript = missingScriptBuilder.apply(scriptHost);
        Map<String, Map<Policy, Script>> propertyMap = scriptMap.getOrDefault(typeName, new HashMap<>());
        Map<Policy, Script> policyMap = propertyMap.getOrDefault(propertyName, new HashMap<>());
        policyMap.putIfAbsent(policy, missingScript);
        propertyMap.putIfAbsent(propertyName, policyMap);
        scriptMap.putIfAbsent(typeName, propertyMap);
        return missingScript;
    }

    public CollectionPolicy getCollectionPolicy(String type, String property) {
        String policy = getPolicy(Policy.COLLECTION_POLICY, type, property);
        return CollectionPolicy.valueOf(policy.toUpperCase());
    }

    private String getPolicy(Policy policy, String type, String property) {
        if (!ruleSet.has(type))
            return policy.defaultValue();

        JSONObject typeObject = ruleSet.getJSONObject(type);

        if (!typeObject.has(property))
            return policy.defaultValue();

        JSONObject propertyObject = typeObject.getJSONObject(property);

        if (!propertyObject.has(policy.policyKey()))
            return policy.defaultValue();

        return propertyObject.get(policy.policyKey()).toString();
    }

    enum CollectionPolicy {
        FULL,
        REDACTED,
        PARTIAL
    }

    enum Policy {
        READ,
        COLLECTION_POLICY,
        ELEMENT_FILTER;

        String defaultValue() {
            return switch (this) {
                case COLLECTION_POLICY -> "FULL";
                case ELEMENT_FILTER, READ -> "true";
            };
        }

        String policyKey() {
            return switch (this) {
                case READ -> "read";
                case COLLECTION_POLICY -> "collection_policy";
                case ELEMENT_FILTER -> "element_filter";
            };
        }
    }
}