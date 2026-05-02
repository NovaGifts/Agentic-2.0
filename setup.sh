#!/bin/bash

# GiftNova Setup Script
# Checks all prerequisites, sets up the database, and runs the app.

set -e

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

ok()   { echo -e "${GREEN}[OK]${NC} $1"; }
warn() { echo -e "${YELLOW}[WARN]${NC} $1"; }
fail() { echo -e "${RED}[FAIL]${NC} $1"; exit 1; }

echo ""
echo "================================================"
echo "         GiftNova — Environment Setup"
echo "================================================"
echo ""

# ── 1. Java 17+ ──────────────────────────────────────
if ! command -v java &>/dev/null; then
    fail "Java not found. Install Java 17+: brew install openjdk@17"
fi

JAVA_VER=$(java -version 2>&1 | awk -F '"' '/version/ {print $2}' | cut -d'.' -f1)
if [ "$JAVA_VER" -lt 17 ] 2>/dev/null; then
    fail "Java 17+ required. Found version $JAVA_VER. Run: brew install openjdk@17"
fi
ok "Java $JAVA_VER found"

# ── 2. Maven ─────────────────────────────────────────
if ! command -v mvn &>/dev/null; then
    warn "Maven not found. Installing via Homebrew..."
    brew install maven || fail "Failed to install Maven"
fi
ok "Maven $(mvn -version 2>&1 | head -1 | awk '{print $3}') found"

# ── 3. PostgreSQL ─────────────────────────────────────
if ! command -v psql &>/dev/null; then
    warn "PostgreSQL not found. Installing via Homebrew..."
    brew install postgresql@16
    echo 'export PATH="/opt/homebrew/opt/postgresql@16/bin:$PATH"' >> ~/.zshrc
    export PATH="/opt/homebrew/opt/postgresql@16/bin:$PATH"
fi
ok "PostgreSQL $(psql --version | awk '{print $3}') found"

# ── 4. Start PostgreSQL if not running ───────────────
if ! pg_isready -q 2>/dev/null; then
    warn "PostgreSQL is not running. Starting..."
    brew services start postgresql@16
    sleep 2
fi
ok "PostgreSQL is running"

# ── 5. Create database if missing ────────────────────
DB_EXISTS=$(psql -U "$(whoami)" -tAc "SELECT 1 FROM pg_database WHERE datname='giftnova'" postgres 2>/dev/null || echo "")
if [ "$DB_EXISTS" != "1" ]; then
    warn "Database 'giftnova' not found. Creating..."
    createdb giftnova && ok "Database 'giftnova' created"
else
    ok "Database 'giftnova' exists"
fi

# ── 6. Update application.properties with current user ──
CURRENT_USER=$(whoami)
PROPS="src/main/resources/application.properties"

sed -i '' "s/^spring.datasource.username=.*/spring.datasource.username=$CURRENT_USER/" "$PROPS"
sed -i '' "s/^spring.datasource.password=.*/spring.datasource.password=/" "$PROPS"
ok "Database credentials set (user: $CURRENT_USER)"

# ── 7. Run the app ────────────────────────────────────
echo ""
echo "================================================"
echo "  All checks passed. Starting GiftNova..."
echo "  Open http://localhost:8080 in your browser"
echo "  Press Ctrl+C to stop"
echo "================================================"
echo ""

mvn spring-boot:run
