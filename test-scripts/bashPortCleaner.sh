#!/usr/bin/env bash
set -euo pipefail

# Port ranges
udp_ports=(8080 {10001..10005})
tcp_ports=({20000..20005})

# Function: kill any PIDs in the given list
kill_pids() {
  local proto=$1 port=$2 pids=("${!3}")
  if [ ${#pids[@]} -eq 0 ]; then
    echo "[$proto] Port $port is free."
  else
    for pid in "${pids[@]}"; do
      if kill -0 "$pid" &>/dev/null; then
        echo "[$proto] Killing PID $pid on port $port"
        kill "$pid"
        # if you really need to force:
        # kill -9 "$pid"
      fi
    done
  fi
}

# Check and kill UDP listeners
for port in "${udp_ports[@]}"; do
  # lsof: -t → just PIDs, -i UDP@host:port
  mapfile -t pids < <(lsof -t -i UDP@127.0.0.1:"$port" 2>/dev/null || true)
  kill_pids "UDP" "$port" pids[@]
done

# Check and kill TCP listeners
for port in "${tcp_ports[@]}"; do
  # -sTCP:LISTEN restricts to LISTEN state
  mapfile -t pids < <(lsof -t -sTCP:LISTEN -i TCP@127.0.0.1:"$port" 2>/dev/null || true)
  kill_pids "TCP" "$port" pids[@]
done
