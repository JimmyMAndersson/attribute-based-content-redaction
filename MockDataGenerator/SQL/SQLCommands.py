drop_users_table_command = """
DROP TABLE IF EXISTS users;
"""

drop_branches_table_command = """
DROP TABLE IF EXISTS branches;
"""

create_branches_table_command = """
CREATE TABLE IF NOT EXISTS branches
(
    id      INTEGER PRIMARY KEY AUTOINCREMENT,
    country TEXT NOT NULL,
    state   TEXT NOT NULL,
    city    TEXT NOT NULL
);
"""

create_users_table_command = """
CREATE TABLE IF NOT EXISTS users
(
    id                 INTEGER PRIMARY KEY AUTOINCREMENT,
    first_name         TEXT NOT NULL,
    last_name          TEXT NOT NULL,
    title              TEXT NOT NULL,
    reports_to         INTEGER,
    security_clearance INTEGER  NOT NULL,
    branch             INTEGER  NOT NULL,
    salary             INTEGER  NOT NULL,
    FOREIGN KEY (reports_to) REFERENCES users (id),
    FOREIGN KEY (branch) REFERENCES branches (id),
    CHECK (security_clearance BETWEEN 1 AND 4)
);
"""

insert_branches_command = """
INSERT INTO branches (country, state, city) VALUES (?, ?, ?);
"""

insert_employees_command = """
INSERT INTO users (first_name, last_name, title, reports_to, security_clearance, branch, salary) VALUES (?, ?, ?, ?, ?, ?, ?);
"""
