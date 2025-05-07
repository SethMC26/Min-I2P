for port in {10000..10010}; do
  if pid=$(lsof -t -i:"$port"); then
    echo "killing port $port"
    kill "$pid"
  fi
done

for port in {20000..20010}; do
  if pid=$(lsof -t -i:"$port"); then
    echo "killing port $port"
    kill "$pid"
  fi
done