from argparse import ArgumentParser
import os
import json
import shutil
import psutil
import requests
import subprocess
import time


def kill(proc_pid):
    process = psutil.Process(proc_pid)
    for proc in process.children(recursive=True):
        proc.kill()
    process.kill()


def remove_last_lines(file, line_count):
    with open(file, 'r') as readfile:
        lines = readfile.readlines()

    with open(file, 'w') as writefile:
        writefile.writelines(lines[:-line_count])


def split_graphql_query(line):
    contents = line.split(",")
    return contents[0].strip(), contents[1].strip()


def query_iterator(lines):
    return map(split_graphql_query, lines)


def run_rest_tests(args):
    script_path = os.path.abspath(os.path.join(os.path.curdir, 'rest-api/startserver.sh'))
    database_path = os.path.abspath(os.path.join(os.path.curdir, 'graphql-api/databases/data.db'))
    log_file_path = os.path.abspath(os.path.join(os.path.curdir, args.measurementsfolder, 'rest_data'))

    with open(log_file_path, 'w'):
        pass

    server = subprocess.Popen(['sh', script_path, database_path, log_file_path])

    # Wait a few seconds for server to start
    time.sleep(7)

    with open(args.measurementsfolder + 'rest_calls', 'r') as rest_calls:
        for endpoint in map(lambda x: x.strip(), rest_calls.readlines()):
            for _ in range(args.warmups):
                requests.get(args.host + endpoint)

            remove_last_lines(log_file_path, args.warmups)

            for _ in range(args.requests):
                requests.get(args.host + endpoint)

            print(f"Finished collecting data for REST endpoint: {endpoint}.")

    kill(server.pid)


def run_graphql_tests(args):
    script_path = os.path.abspath(os.path.join(os.path.curdir, 'graphql-api/startserver.sh'))
    database_path = os.path.abspath(os.path.join(os.path.curdir, 'graphql-api/databases/data.db'))
    ruleset_path = os.path.abspath(os.path.join(os.path.curdir, 'graphql_ruleset'))
    log_file_path = os.path.abspath(os.path.join(os.path.curdir, args.measurementsfolder, 'graphql_data'))

    server = subprocess.Popen(['sh', script_path, 'abacredaction', database_path, ruleset_path])

    # Wait a few seconds for server to start
    time.sleep(7)

    with open(log_file_path, 'w') as log:
        with open('graphql_calls', 'r') as graphql_calls:
            for rest_equal, query in query_iterator(graphql_calls.readlines()):
                for _ in range(args.warmups):
                    requests.post(args.host + "/graphql", json={"query": query}, headers={"authid": "1"})

                for _ in range(args.requests):
                    response = requests.post(args.host + "/graphql", json={"query": query}, headers={"authid": "1"})
                    log.write(f"{rest_equal},{response.json()['extensions']['executionTime']}\n")

                print(f"Finished collecting data for GraphQL query: {rest_equal}.")

    kill(server.pid)


if __name__ == '__main__':
    argument_parser = ArgumentParser()
    argument_parser.add_argument("--host", default="http://localhost:8080", type=str)
    argument_parser.add_argument("--warmups", '-w', default=30, type=int)
    argument_parser.add_argument("--requests", '-r', default=500, type=int)
    argument_parser.add_argument("--measurementfolder", '-m', default='measurements', type=str)
    args = argument_parser.parse_args()

    run_rest_tests(args)
    run_graphql_tests(args)
