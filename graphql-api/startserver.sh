#! /bin/sh


cd $(dirname $0)
argument_count=$#

if [ $argument_count -ne 3 ]
then
  echo "This script needs three arguments."
  echo "The first specifying a server type."
  echo "The second giving a URL to a SQLite3 database file."
  echo "The third giving a URL to a ruleset file."
  exit 1
fi

./gradlew clean -q
./gradlew build -q
java -jar -Dspring.main.banner-mode=off -Dgraphql.server-type=$1 -Dgraphql.database-url=$2 -Dgraphql.ruleset-url=$3 build/libs/graphql-api-0.0.1.jar
