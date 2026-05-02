#!/bin/bash
# Kill anything already on port 8080
lsof -ti:8080 | xargs kill -9 2>/dev/null

# Start the app in background
mvn spring-boot:run &
APP_PID=$!

# Wait until port 8080 is accepting connections, then open browser
echo "Starting GiftNova..."
until curl -s http://localhost:8080 > /dev/null 2>&1; do
  sleep 1
done

open http://localhost:8080
echo "GiftNova is running at http://localhost:8080 (PID $APP_PID)"
echo "Press Ctrl+C to stop."
wait $APP_PID
