#!/bin/bash
set -e

# Creates all per-service PostgreSQL databases on first container start.
# Mounted at /docker-entrypoint-initdb.d/ — runs automatically as postgres superuser.

DATABASES=("user_db" "product_db" "order_db" "payment_db" "notification_db")

for DB in "${DATABASES[@]}"; do
    echo "Creating database: $DB"
    psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" <<-EOSQL
        CREATE DATABASE $DB;
        GRANT ALL PRIVILEGES ON DATABASE $DB TO $POSTGRES_USER;
EOSQL
done

echo "All databases created: ${DATABASES[*]}"
