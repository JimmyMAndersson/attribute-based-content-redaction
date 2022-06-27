#! /bin/sh


cd $(dirname $0)
argument_count=$#

if [ $argument_count -ne 2 ]
then
  echo "This script needs two arguments."
  echo "The first specifying a database path."
  echo "The second specifying a log file path."
  exit 1
fi

./gradlew clean -q
./gradlew build -q
java -jar -Dspring.main.banner-mode=off -Drest-api.log-path=$2 -Drest-api.database-path=$1 build/libs/rest-api-0.0.1.jar
