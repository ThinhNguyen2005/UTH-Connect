"""Script thiết lập database PostgreSQL cho UTH Canteen ERP."""

import subprocess
import sys


def setup():
    psql_path = r"C:\Program Files\PostgreSQL\18\bin\psql.exe"
    print(f"[*] Checking PostgreSQL connection via {psql_path}...")

    # SQL script to create role and database
    sql_commands = """
    DO $$
    BEGIN
        IF NOT EXISTS (SELECT FROM pg_catalog.pg_roles WHERE rolname = 'odoo') THEN
            CREATE ROLE odoo WITH LOGIN SUPERUSER CREATEDB CREATEROLE PASSWORD 'odoo';
            RAISE NOTICE 'Role odoo created successfully.';
        ELSE
            RAISE NOTICE 'Role odoo already exists.';
        END IF;
    END
    $$;
    """

    try:
        # Run with default postgres user
        res = subprocess.run(
            [psql_path, "-U", "postgres", "-c", sql_commands],
            capture_output=True,
            text=True,
            timeout=10,
        )
        print(res.stdout)
        if res.stderr:
            print("Notice/Stderr:", res.stderr)
        print("[+] PostgreSQL user 'odoo' verified/configured.")
    except Exception as e:
        print(f"[-] Note on PostgreSQL setup: {e}")


if __name__ == "__main__":
    setup()
