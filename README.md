# Peer-to-peer streaming using I2P

## Description

This project will be a peer-to-peer streaming service that implements I2P. (I will add more later.)

---

## Install

1. First, you will need to clone the repo.

```bash
gh repo clone https://github.com/SOuellette22/CSC3055_Final_Cook_Holtzman_Ouellette
```

2. Second, you will need to add a lib folder to the project directory with the following library jar files:

   - [Merrimack Util GitHub page](https://github.com/kisselz/merrimackutil)
   - [Bouncycastle Java Download page](https://www.bouncycastle.org/download/bouncy-castle-java/)

3. Finally, you will need Apache Ant to run this project.

   - Which will build the project

```bash
ant dist
```

---

## Usage

1. First, you will want to set up the relay peers for the network to be viable. To do this, run the following;

      WARNING: The bashPostCleaner removes anything running on the ports used by the relay ports, which are ports 8080, 10001-10005, and 20001-20005 by default and can be changed in the bashPortCleaner.sh and in the relayConfig files.

      ```
      bash bashPortCleaner.sh
      bash relay.sh
      ```

2. Second, you must start the server after properly installing everything above. This can be done 1 of 2 ways:

   1. Default Config:
   - This will start the server with the default config file already in the repo.
   ```
   java -jar dist/server.jar
   ```
   2. Custom Config:
   - First, you should make sure that your config.json file is correctly formatted. This looks as follows;
   ```json
   {
      "public": "MCowBQYDK2VwAyEAKANZ5XaEOE6Y4GJnFLbsDJj6udKrBwTBOPUkH6MQrt8=",
      "private": "MFECAQEwBQYDK2VwBCIEIPv2eaPx8rupwPqtyp1OBGe55UOsSCzmvuh0EHeNsHSPgSEAKANZ5XaEOE6Y4GJnFLbsDJj6udKrBwTBOPUkH6MQrt8=",
      "debug": "true",
      "database-file": "test-data/database/database.json",
      "users-file": "test-data/database/users.json",
      "audio-file": "test-data/database/audio/",

      "host_BS": "127.0.0.1",
      "port_BS": 8080,
      "host_router": "127.0.0.1",
      "RSTPort": 10006,
      "CSTPort": 20006
   }
   ```
       I. public - This is the public Ed25519 key Base64 hash for the server

       II. private - This is the private Ed25519 key Base64 hash for the server

       III. debug - This is an optional field that takes a true or false value for debug mode

       IV. database-file - This is the file path where the audio database JSON file will be saved

       V. users-file - This is the file path where the user's database JSON file will be saved

       VI. audio-file - This is where the user will save the audio files that are created

       VII. host_BS - This is the address of the bootstrap peer

       VIII. port_BS - This is the port of the bootstrap peer

       IX. host_router - This is the address of the router

       X. RSTPort - This is the port number for the router

       XI. CSTPort - This is the port number of the client service

   - Then you will run this command;
   ```
   java -jar dist/server.jar --config "<file_path>"
   ```

3. Third, you will then want to run the client by doing the following;

   1. This will run the client with the default config:
   ```
   java -jar dist/client.jar
   ```
   2. You can also run it with a custom config file:
   ```
   java -jar dist/client.jar --config test-data/config/clientConfig.json
   ```
      - To make sure that the JSON file is properly formatted, it should look as follows

      ```json
      {
         "serverhash": "cDc+7zJGIrX9pttItlaNOwKTz9/cWCgtbRYLD0vwz7g=",

         "host_BS": "127.0.0.1",
         "port_BS": 8080,
         "host_router": "127.0.0.1",
         "RSTPort": 10007,
         "CSTPort": 20007
      }
      ```

       I. serverhash - This is the hash destination of the server

       II. host_BS - This is the address of the bootstrap peer

       III. port_BS - This is the port of the bootstrap peer

       IV. host_router - This is the address of the router

       V. RSTPort - This is the port number for the router

       VI. CSTPort - This is the port number of the client service

4. Once you have run everything properly, all the peers will help form tunnels with the client and the server. Then the client will get put into a console input prompt loop where they can either create a user (1), add a song to the database (2), play a Song from the database (3), or list all the songs in the database (4). 
