package common.I2P.NetworkDB;

import common.Logger;
import org.bouncycastle.util.encoders.Base64;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;

/**
 * Network database to store information about peers using modified Kademlia algorithm
 */
public class NetDB {
    /**
     * Router Info for this router maintaining the netDB
     */
    private RouterInfo routerInfo;
    private byte[] rootRoutingKey;
    /**
     * RoutingTable has 256 buckets(32 byte sha256 hash) each bucket has an entry keyed under a hash of that entry with
     * a corresponding record to that hash(RouterInfo or LeaseSet)
     */
    private HashMap<Integer, HashMap<String, Record>> routingTable;
    /**
     * Logger for this branch
     */
    private Logger log = Logger.getInstance();
    private String lastHour;

    /**
     * Create a new NetDB for this router {@code routerInfo}
     * @param routerInfo routerInfo
     */
    public NetDB(RouterInfo routerInfo) {
        routingTable = new HashMap<>(); //create new routingTable we will use lazy initialization for each bucket

        this.routerInfo = routerInfo;
        //set hour off last key rotation
        //chatGPT generated date formatting
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyyMMddHH");

        lastHour = Instant.now()                // current moment
                .truncatedTo(ChronoUnit.HOURS)  // drop mins/sec/nanos
                .atZone(ZoneOffset.UTC)         // view in UTC
                .format(fmt);                   // format as yyyyMMddHH

        this.rootRoutingKey = computeRoutingKey(routerInfo.getHash());
    }

    /**
     * Store a record in this netDB
     * @param record Record to store {@code RouterInfo or LeaseSet}
     */
    public synchronized void store(Record record) {
        if (!record.verifySignature()) {
            log.warn("NetDB: Record has Invalid signature disregarding");
            return;
        }
        byte[] routingKey = computeRoutingKey(record.getHash());

        //avoid storing ourselfs so we do not get our own hash when getting k closest peers
        if (Arrays.equals(routingKey, rootRoutingKey))
            return;

        //calculate distance between hash and record hash
        int distance = calculateXORMetric(rootRoutingKey, routingKey);

        //get bucket at distance
        HashMap<String, Record> bucket = routingTable.get(distance);

        //no bucket in routingTable so lets add it
        if (bucket == null) {
            bucket = new HashMap<>();
            routingTable.put(distance, bucket);
        }

        //put record under routing key
        bucket.put(Base64.toBase64String(routingKey), record);
        log.debug("Put record into bucket " + distance + " hash " + Base64.toBase64String(routingKey));
        log.trace(logNetDB());
    }

    /**
     * Lookup a Record in the Network Database
     * @param key 32 byte SHA256 hash of record entry to find
     * @return Record {@code RouterInfo or LeaseSet} or null if record is not found
     */
    public synchronized Record lookup(byte[] key) {
        //get routing key
        byte[] routingKey = computeRoutingKey(key);

        if (Arrays.equals(key, routerInfo.getHash())) //we do not store ourselves so we handle that case here
            return routerInfo;


        //calculate distance between hash and record we want to find(under key)
        int distance = calculateXORMetric(rootRoutingKey, routingKey);
        log.trace("NetDB: distance is " + distance);

        //get bucket key would be stored in
        HashMap<String, Record> bucket = routingTable.get(distance);
        //if no bucket exists then return null we do not have Record
        if (bucket == null)
            return null;
        //Attempt to find record in bucket(will return null if not found)
        log.trace(logNetDB());
        return bucket.get(Base64.toBase64String(routingKey));
    }

    /**
     * Retrieve up to {@code k} closest RouterInfo records to the given key from the routing table.
     * Only RouterInfos will be returned, LeaseSets are ignored.
     *
     * @param key the 32-byte SHA256 hash key to find close routers for
     * @param k the maximum number of RouterInfos to return
     * @return a list of up to {@code k} closest RouterInfo Records
     *
     * @author ChatGPT (OpenAI Assistant) - collaborator
     * @deprecated Since 5/1/2025
     */
    public synchronized ArrayList<RouterInfo> getKClosestRouterInfos(byte[] key, int k) {
        // Calculate XOR distance from our own router ID to the target key
        byte[] routingKey = computeRoutingKey(key);
        int startDistance = calculateXORMetric(rootRoutingKey, routingKey);
        //below is chatgpt generated but it did what i wanted for this kind of annoying bit of code

        ArrayList<RouterInfo> result = new ArrayList<>(k); // Prepare result list with initial capacity

        int lower = startDistance;    // Start searching downward
        int higher = startDistance + 1; // Start searching upward

        // Expand search outward until we find enough RouterInfo records or search bounds exhausted
        while (result.size() < k && (lower >= 0 || higher <= 255)) {

            // Check lower bucket if in bounds
            if (lower >= 0) {
                HashMap<String, Record> bucket = routingTable.get(lower);
                if (bucket != null) {
                    for (Record record : bucket.values()) {
                        // Only add if the record is of type ROUTERINFO
                        if (record.getRecordType() == Record.RecordType.ROUTERINFO) {
                            result.add((RouterInfo) record);
                            if (result.size() >= k) {
                                break; // Stop if we have enough
                            }
                        }
                    }
                }
                lower--; // Move to next lower bucket
            }

            // Check higher bucket if in bounds
            if (higher <= 255) {
                HashMap<String, Record> bucket = routingTable.get(higher);
                if (bucket != null) {
                    for (Record record : bucket.values()) {
                        // Only add if the record is of type ROUTERINFO
                        if (record.getRecordType() == Record.RecordType.ROUTERINFO) {
                            result.add((RouterInfo) record);
                            if (result.size() >= k) {
                                break; // Stop if we have enough
                            }
                        }
                    }
                }
                higher++; // Move to next higher bucket
            }
        }

        // Return the list of RouterInfo records found
        return result;
    }
    /**
     * Retrieve up to {@code k} closest records (either {@link RouterInfo} or
     * {@link LeaseSet}) to the supplied key, using the same XOR-metric walk that
     * Kademlia employs.
     *
     * @param key 32-byte SHA-256 hash whose neighbours we want
     * @param k   maximum number of records to return
     * @return a list (≤ k) of the closest {@code Record}s in distance order
     *
     * @author ChatGPT (OpenAI Assistant)
     */
    public synchronized ArrayList<Record> getKClosestPeers(byte[] key, int k) {
        ArrayList<Record> result = new ArrayList<>(k);
        byte[] routingKey = computeRoutingKey(key);

        int startDistance = calculateXORMetric(rootRoutingKey, routingKey);
        int lower  = startDistance;      // search downward
        int higher = startDistance + 1;  // and upward

        while (result.size() < k && (lower >= 0 || higher <= 255)) {

            /* --- lower bucket ------------------------------------------------ */
            if (lower >= 0) {
                HashMap<String, Record> bucket = routingTable.get(lower);
                if (bucket != null) {
                    for (Record record : bucket.values()) {
                        result.add(record);               // RouterInfo or LeaseSet
                        if (result.size() >= k) break;
                    }
                }
                lower--;
            }

            /* --- higher bucket ----------------------------------------------- */
            if (higher <= 255) {
                HashMap<String, Record> bucket = routingTable.get(higher);
                if (bucket != null) {
                    for (Record record : bucket.values()) {
                        result.add(record);
                        if (result.size() >= k) break;
                    }
                }
                higher++;
            }
        }
        return result;
    }

    /**
     * Computer routing key H(H(record) + time up to hour UTC) this is to make Sybil attacks more expensive
     * @param key of Record to compute routing key for
     * @return Bytes of routing key
     */
    private synchronized byte[] computeRoutingKey(byte[] key) {
        //add record to bucket under its RoutingKey which is the hash of the record with date up to hour appeneded
        //this is to prevent a sybil attack by doing a partial rotation of the keyspace every hour(every day in I2P)
        //chatGPT generated date formatting
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyyMMddHH");

        String currHour = Instant.now()                // current moment
                .truncatedTo(ChronoUnit.HOURS)  // drop mins/sec/nanos
                .atZone(ZoneOffset.UTC)         // view in UTC
                .format(fmt);                   // format as yyyyMMddHH

        /* --- parse as UTC ZonedDateTime ---------------------------------------- */
        ZonedDateTime prev = LocalDateTime.parse(lastHour, fmt).atZone(ZoneOffset.UTC);
        ZonedDateTime curr = LocalDateTime.parse(currHour, fmt).atZone(ZoneOffset.UTC);

        rootRoutingKey = computeHashOfKey(routerInfo.getHash(), currHour);
        //rotate routing keys if it is a new hours
        if (!curr.isBefore(prev.plusHours(1))) {
            log.debug("Last hour: " + lastHour + " currHour: " + currHour);
            log.info("Next hour, Rotating NetDB keys");

            HashMap<Integer, HashMap<String, Record>> newRoutingTable = new HashMap<>();
            for (HashMap<String, Record> bucket : routingTable.values()) {
                for (Record record : bucket.values()) {
                    byte[] routingKey = computeHashOfKey(key, currHour);
                    //calculate distance between hash and record hash
                    int distance = calculateXORMetric(rootRoutingKey, routingKey);

                    //get bucket at distance
                    HashMap<String, Record> newBucket = newRoutingTable.computeIfAbsent(distance, k -> new HashMap<>());
                    newBucket.put(Base64.toBase64String(routingKey), record);
                }
            }
            routingTable = newRoutingTable;
            lastHour = currHour;
            log.debug("new netDB " + logNetDB());
        }

        return computeHashOfKey(key, lastHour);
    }

    private byte[] computeHashOfKey(byte[] key, String time) {
        try {
            //hash payload of message
            MessageDigest md = MessageDigest.getInstance("SHA256");
            md.update(key);
            md.update(time.getBytes(StandardCharsets.UTF_8));
            return md.digest();
        }
        catch (NoSuchAlgorithmException ex) {throw new RuntimeException(ex);} //should not hit this case
    }

    /**
     * Calculate closeness as from XOR metric of keys we xor keys, they are close if result have many leading zeroes -
     * hashes are similar
     * @param key1 32 byte SHA256 hash to compare
     * @param key2 32 byte SHA256 hash to compare
     * @return 0-255 of closeness
     */
    private int calculateXORMetric(byte[] key1, byte[] key2) {
        BitSet bits1 = BitSet.valueOf(key1);
        BitSet bits2 = BitSet.valueOf(key2);

        bits1.xor(bits2);

        //not sure which we should use - in my previous project we used commented out version
        //return 256 - bits1.length();
        return 256 - bits1.length(); //take length to find leading zeros after xor
    }

    /**
     * Dump the entire routing table, bucket-by-bucket, showing all keys.
     * For RouterInfo records we also include "host:port".
     *
     * @author ChatGPT (OpenAI Assistant)
     *
     * @return a formatted text snapshot (ready for logging or console output)
     */
    public String logNetDB() {
        StringBuilder out = new StringBuilder();

        // Buckets are numbered 0-256 (inclusive).  Highest first for readability.
        for (int idx = 256; idx >= 0; idx--) {
            HashMap<String, Record> bucket = routingTable.get(idx);
            if (bucket == null || bucket.isEmpty()) {
                continue;                                 // skip empty buckets
            }

            out.append(String.format("%n--- Bucket %-3d (size %d) %s%n",
                    idx,
                    bucket.size(),
                    "─".repeat(40)));

            /* iterate over both key and value so we can inspect the Record */
            bucket.entrySet()
                    .stream()
                    .sorted(Map.Entry.comparingByKey())     // alphabetical, stable
                    .forEach(entry -> {
                        String key    = entry.getKey();     // Base64(hash)
                        Record record = entry.getValue();

                        if (record.getRecordType() == Record.RecordType.ROUTERINFO) {
                            RouterInfo ri = (RouterInfo) record;
                            out.append(String.format("   %s %s:%d%n",
                                    key,
                                    ri.getHost(),
                                    ri.getPort()));
                        } else {                            // LeaseSet (or future types)
                            out.append("   ").append(key).append('\n');
                        }
                    });
        }
        return out.toString();
    }
}
