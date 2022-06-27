import sqlite3
from Data.Branches import *
from EmployeeCreator import create_employees
from argparse import ArgumentParser
from SQL.SQLCommands import *


def create_tables(cur):
    cur.execute(drop_users_table_command)
    cur.execute(drop_branches_table_command)
    cur.execute(create_branches_table_command)
    cur.execute(create_users_table_command)


def insert_branches(cur):
    cur.executemany(insert_branches_command, map(lambda x: x[:-1], branches.itertuples(index=False)))


def insert_employees(cur, emps):
    cur.executemany(insert_employees_command, map(lambda x: x[1:], emps))


if __name__ == "__main__":
    parser = ArgumentParser(description="Generate user data and insert it into a SQLite3 database.")
    parser.add_argument("--database", default="mockdatabase.db", type=str)
    parser.add_argument("--workers", default=50000, type=int)

    args = parser.parse_args()

    with sqlite3.connect(args.database) as connection:
        cursor = connection.cursor()
        create_tables(cursor)
        insert_branches(cursor)
        employees = create_employees(args.workers)
        insert_employees(cursor, employees)
        connection.commit()
        cursor.close()
