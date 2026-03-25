FROM postgres:17

COPY sql/init.sql /docker-entrypoint-initdb.d/01-init.sql