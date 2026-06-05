#!/usr/bin/env bash
# Dump the UUMS PostgreSQL database to a timestamped SQL file.
# Requires pg_dump on PATH.
#
# Usage:
#   ./scripts/dump-database.sh
#   ./scripts/dump-database.sh custom
#   PGPASSWORD=your-password ./scripts/dump-database.sh

set -euo pipefail

resolve_pg_dump() {
  if command -v pg_dump >/dev/null 2>&1; then
    command -v pg_dump
    return 0
  fi

  if [[ "$OSTYPE" == "msys" || "$OSTYPE" == "cygwin" || -n "${MINGW_PREFIX:-}" ]]; then
    local version candidate
    for version in 18 17 16 15 14 13; do
      candidate="/c/Program Files/PostgreSQL/${version}/bin/pg_dump.exe"
      if [[ -x "$candidate" ]]; then
        echo "$candidate"
        return 0
      fi
    done
  fi

  echo "pg_dump not found. Add PostgreSQL bin to PATH, e.g.:" >&2
  echo '  export PATH="/c/Program Files/PostgreSQL/18/bin:$PATH"' >&2
  return 1
}

PG_DUMP="$(resolve_pg_dump)"

DB_HOST="${DB_HOST:-localhost}"
DB_PORT="${DB_PORT:-5432}"
DATABASE="${DATABASE:-uums_db}"
DB_USER="${DB_USER:-postgres}"
OUTPUT_DIR="${OUTPUT_DIR:-backups}"
FORMAT="${1:-plain}"

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
OUTPUT_PATH="$PROJECT_ROOT/$OUTPUT_DIR"
TIMESTAMP="$(date +%Y%m%d_%H%M%S)"

mkdir -p "$OUTPUT_PATH"

if [[ "$FORMAT" == "custom" ]]; then
  OUTPUT_FILE="$OUTPUT_PATH/${DATABASE}_${TIMESTAMP}.dump"
  DUMP_FORMAT="c"
else
  OUTPUT_FILE="$OUTPUT_PATH/${DATABASE}_${TIMESTAMP}.sql"
  DUMP_FORMAT="p"
fi

if [[ -z "${PGPASSWORD:-}" ]]; then
  read -r -s -p "Enter PostgreSQL password for user '$DB_USER': " PGPASSWORD
  echo
  export PGPASSWORD
fi

echo "Dumping '$DATABASE' from ${DB_HOST}:${DB_PORT} ..."

"$PG_DUMP" \
  -h "$DB_HOST" \
  -p "$DB_PORT" \
  -U "$DB_USER" \
  -d "$DATABASE" \
  -F "$DUMP_FORMAT" \
  -f "$OUTPUT_FILE" \
  --no-owner \
  --no-acl

FILE_SIZE_KB="$(du -k "$OUTPUT_FILE" | cut -f1)"
echo "Done. Backup saved to:"
echo "  $OUTPUT_FILE"
echo "  Size: ${FILE_SIZE_KB} KB"
