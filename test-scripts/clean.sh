rm -r test-data/database/audio/*
rm -r test-data/database/database.json

ant clean
ant dist

java -jar dist/server.jar