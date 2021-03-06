# Introduction 
This is the 2022 Master thesis project of Jimmy Andersson and Claudio Aguilar Aguilar, which investigates attribute-based hypergranular access control in large-scale data settings.

## Getting Started
The project consists of two main parts.

1. The data generator that builds an SQLite3 database consisting of a made up company hierarchy.

2. The GraphQL server that incorporates an access control mechanism that redacts response values according to an arbitrary set of rules.

### Generating Mock Data

A new mock database can be generated by running the `DatabaseGenerator.py` script found in the `MockDataGenerator` directory.

```bash
$ python DatabaseGenerator.py
```

The script takes two optional parameters:

* `--database <path>` specifies at which path the script should place the new database.
* `--workers <number>` specifies the approximate number of workers to insert into the database.

### Starting a GraphQL Server

Starting a graph-based server can be done by moving into the `graphql-api` folder and running the `startserver.sh` script, like so:

```bash
$ ./startserver.sh <servertype> <database-path> <ruleset-path>
```

The above script takes three different arguments.

1. `<servertype>` can be `noac` or `abacredaction`. This specifies whether to run the server using no access control (_noac_) or attribute-based redaction access control (_abacredaction_).

2. `<database-path>` is the relative or absolute path to the mocked database. In the PoC, the path is either `databases/test.db` or `databases/data.db`.

3. `<ruleset-path>` is the relative or absolute path to the mocked database. Get started by using the ruleset with path `rulesets/ruleset`.

GraphQL requests can now be made to `http://localhost:8080/graphql` according to the schema specified in `src/main/resources/schema.graphqls`.

### Starting a REST Server

> **IMPORTANT**  
> 
> The REST server only serves the purpose of a comparison for the attribute-based redaction component in the GraphQL server. It has little to no use in any other situation.

Starting a REST server can be done by moving into the `rest-api` folder and running the `startserver.sh` script, like so:

```bash
$ ./startserver.sh <database-path> <logfile-path>
```

The above script takes three different arguments.

1. `<database-path>` is the relative or absolute path to the mocked database. In the PoC, the path is either `../graphql-api/databases/test.db` or `./graphql-api/databases/data.db`.

2. `<logfile-path>` is the relative or absolute path to a log file in which the server records information about execution times. To get started, place the log file in the `rest-api` directory by passing `logfile` as the parameter.

REST requests can now be made to `http://localhost:8080` according to the endpoints specified in the source files.


## Running Measurement Collections

Measurements are collected using an automated script found in the root directory. Starting a collection run is as simple as executing:

```bash
$ python MeasurementCollection.py
```

The script takes four optional parameters:

* `--host <url>` specifies the URL where the servers will be located. The default value is `http://localhost:8080`.
* `--warmups <number>` specifies the number of warm up requests the server is sent before data collection begins. The default value is 30.
* `--requests <number>` specifies the number of requests sent during data collection. The default value is 500.
* `--measurementfolder <path>` specifies where the script should place the measurement results. The default value is `measurements`.

## Results Analysis

The `Likert Plots.R` and `Performance Analysis.ipynb` files perform analysis of the collected results and generates plots that are saved to the `result_plots` directory.

## Qualitative Results

Responses to the online survey can be found in CSV form in the `measurements/survey.csv` and `measurements/qualitative_results.csv` files.

* `qualitative_results.csv` Contains the original data, as exported from Google Forms.

* `survey.csv` contains a cleaned version of the data which makes it easier to work with in the analysis scripts.

# Building with Gradle

The project uses Gradle to build and package the application. Therefore, producing a `jar` file is as easy as moving into the `graphql-api` directory and executing the following command:

```bash
$ ./gradlew clean build
```

A `jar` file can now be found in the `build/libs` directory.

# Executing a Jar File

Executing the `jar` file produced in the precious section requires passing a few parameters.

* __graphql.server-type__ specifies the type of server to spin up. This takes any of the `<servertype>` arguments discussed in [Starting a GraphQL Server](/README.md#starting-a-graphql-server) (_noac_ or _abacredaction_).

* __graphql.database-url__ specifies the path to the database, just like the `<database-path>` described in [Starting a GraphQL Server](/README.md#starting-a-graphql-server).

* __graphql.ruleset-url__ specifies the path to the rule set, just like the `<ruleset-path>` described in [Starting a GraphQL Server](/README.md#starting-a-graphql-server).

As an example, one can start a server by executing the following command while standing in the `graphql-api` directory:

```bash
java -jar -Dgraphql.server-type=abacredaction -Dgraphql.database-url=databases/test.db -Dgraphql.ruleset-url=rulesets/ruleset build/libs/graphql-api-0.0.1.jar
```

# Configuring the Redaction Component

In this project, the `DirectiveDataFetcherWrapper` class is the entrypoint to the access control mechanism. It is injected as a global directive, where it wraps the data fetchers that produce real data for the application.

`DirectiveDataFetcherWrapper` is generic over the type one wants to use as a decision information context. The decision context in this PoC is the `Employee` type, and specifying that type within angle brackets gives the access control engine full access to evaluate its properties.

It is important to Jacksonize the chosen decision context type in order to open up all of its properties to the rule evaluator. To do so, simply mark your decision context with the following decorator:

```java
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
```

## Defining Access Rules

This project uses a JSON file to define access rules. However, any format that can express tree-style relations can be used; this includes JSON, YAML, XML, and relational database tables, to name a few approaches.

Rules are built using Type - Property pairs. For example, to specify a rule for the `salary` property of an `Employee`, one can construct a JSON that looks like the following:

```json
{
    "Employee": {
        "salary": {
            "read": "user.isAuthenticated && object.firstName.contains('Jane') && property < 5000"
        }
    }
}
```

The rule above uses three different constructs that are made available to the rule makers.

* __`user`__ is the decision context in this PoC. To make it more generic, the `AccessRuleEvaluator` could be re-programmed to name it `decisionContext` or something similar.

* __`object`__ is the object that encloses property one wants to control access to. In this case, it is the `Employee` which salary we are looking to place restrictions on.

* __`property`__ is the property that we are controlling access to at the moment. When the above rule executes, it will contain the salary value.

Sometimes, the property we want to restrict is a collection type, such as an array, a list, or a set. In such cases, the rule set supports two more rule types to control access.

* __`collection_policy`__ defines how a collection is shared. It takes any of the following values:
  * __`full`__: Shares the collection as-is, and says that the collection and its elements can be shared without any further evaluation. However, any nested properties inside each element will be evaluated on their own later.
  * __`redacted`__: Specifies that each element in the collection should be evaluated. If they fail, they are replaced by a `null` value.
  * __`partial`__: Specifies that each element in the collection should be evaluated. If they fail, they are removed from the collection.

* __`element_filter`__ defines a Boolean expression which the elements in a collection needs to fulfill in order to be included in the response. This expression is only evaluated for __`redacted`__ and __`partial`__ __`collection_policy`__ values, since the __`full`__ option short-circuits any further evaluation at this particular stage of the access control evaluation.

A rule set that restricts access to employee information of a corporate branch office could therefore look like:

```json
{
    "Branch": {
        "employees": {
            "read": "true",
            "collection_policy": "partial",
            "element_filter": "user.isAuthenticated && element.title.contains('Manager')"
        }
    }
}
```

The rule above ensures that everyone can read the `employees` collection, but employees that do not fulfill the __`element_filter`__ expression will not be included in the response.

The element filters give rule makers access to two different constructs:

* __`user`__ is the decision context in this PoC. To make it more generic, the `AccessRuleEvaluator` could be re-programmed to name it `decisionContext` or something similar.

* __`element`__ is the collection element that we are currently investigating. In this case, it is the `Employee` we may want to remove from the collection.

