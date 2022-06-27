from deepdiff import DeepDiff
import os
import json
import psutil
import requests
import shutil
import subprocess
import time


def kill(proc_pid):
    process = psutil.Process(proc_pid)
    for proc in process.children(recursive=True):
        proc.kill()
    process.kill()


def load_file(path) -> str:
    with open(path, 'r') as file:
        return "".join(file.readlines())


def send_request(query_file, header_path):
    host = 'http://localhost:8080/graphql'

    headers = json.loads(load_file(header_path))
    query = load_file(query_file)

    return requests.post(host, json={"query": query}, headers=headers)


if __name__ == '__main__':
    test_folders = [folder for folder in os.listdir(os.path.curdir) if os.path.isdir(folder)]

    ruleset_path = os.path.abspath(os.path.join(os.path.curdir, 'ruleset'))
    script_path = os.path.abspath(os.path.join(os.path.curdir, '../startserver.sh'))
    database_path = os.path.abspath(os.path.join(os.path.curdir, '../databases/test.db'))
    server = subprocess.Popen(['sh', script_path, 'abacredaction', database_path, ruleset_path])

    # Wait a few seconds for server to start
    time.sleep(7)
    successful_tests = 0
    failed_tests = 0

    for test_folder in test_folders:
        current_dir = os.path.join(os.path.curdir, test_folder)
        query_path = os.path.join(current_dir, 'query')
        header_path = os.path.abspath(os.path.join(current_dir, 'headers'))
        test_rules_path = os.path.join(current_dir, 'ruleset')

        shutil.copyfile(test_rules_path, ruleset_path)
        case_expectation = os.path.join(current_dir, 'expectation')

        response = send_request(query_path, header_path)
        data = response.json()['data']
        expectation = json.loads(load_file(case_expectation))

        diff = DeepDiff(data, expectation, ignore_order=True)

        if diff:
            failed_tests += 1
            print(f"{test_folder} failed!")
            print(f"Response: {data}")
        else:
            successful_tests += 1
            print(f"{test_folder} succeeded!")

    print(f"Testing done! {len(test_folders)} tests ran - {successful_tests} succeeded, {failed_tests} failed!")
    os.remove(ruleset_path)
    kill(server.pid)
    subprocess.call(['sh', 'gradle', '--stop', '-q'])
