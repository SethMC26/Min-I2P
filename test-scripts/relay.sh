for cfg in b1 p1 p2 p3 p4 p5; do
    java -jar ../dist/peer.jar -c relayConfigs/${cfg}Config.json &
    sleep 1500 &
done
wait
