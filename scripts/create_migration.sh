#!/bin/bash

SCRIPT_DIR=$(dirname ${BASH_SOURCE})
MIGRATION_DIR=${SCRIPT_DIR}/../resources/migrations
TIMESTAMP=$(date "+%Y%m%d%H%M%S")
MIGRATION=${MIGRATION_DIR}/${TIMESTAMP}_$1

touch ${MIGRATION}.up.sql
touch ${MIGRATION}.down.sql
