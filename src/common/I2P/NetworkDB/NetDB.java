package common.I2P.NetworkDB;

import common.Logger;
import org.bouncycastle.util.encoders.Base64;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;

/**
 * Network database to store information about peers using modified Kademlia algorithm
 */
public class NetDB {
    /**
     * Router Info for this router maintaining the netDB
     */
    private RouterInfo routerInfo;
    /**
     * RoutingTable has 256 buckets(32 byte sha256 hash) each bucket has an entry keyed under a hash of that entry with
     * a corresponding record to that hash(RouterInfo or LeaseSet)
     */
    private HashMap<Integer, HashMap<String, Record>> routingTable;
    /**
     *
     */
    private Logger log = Logger.getInstance();
    /**
     * Create a new NetDB for this router {@code routerInfo}
     * @param routerInfo routerInfo
     */
    public NetDB(RouterInfo routerInfo) {
        this.routerInfo = routerInfo;
        routingTable = new HashMap<>(); //create new routingTable we will use lazy initialization for each bucket

        //we will store ourselfs in netDB in case someone wants to lookup us
        store(routerInfo);
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

        //if (Arrays.equals(record.getHash(),routerInfo.getHash()))
            //return;

        //calculate distance between hash and record hash
        int distance = calculateXORMetric(routerInfo.getHash(), record.getHash());

        //sanity check remove for prod
        assert distance <= 256 : "distance greater than 256! Distance" + distance;

        //get bucket at distance
        HashMap<String, Record> bucket = routingTable.get(distance);

        //no bucket in routingTable so lets add it
        if (bucket == null) {
            bucket = new HashMap<>();
            routingTable.put(distance, bucket);
        }

        //add record to bucket under its key(hash of record)
        // could actually just get key of record isntead of record.getHash() but no diff
        bucket.put(Base64.toBase64String(record.getHash()), record);
        log.debug("Put record into bucket " + distance + " hash " + Base64.toBase64String(record.getHash()));
        log.trace(logNetDB());
    }

    /**
     * Lookup a Record in the Network Database
     * @param key 32 byte SHA256 hash of record entry to find
     * @return Record {@code RouterInfo or LeaseSet} or null if record is not found
     */
    public synchronized Record lookup(byte[] key) {
        //calculate distance between hash and record we want to find(under key)
        int distance = calculateXORMetric(routerInfo.getHash(), key);
        log.trace("NetDB: distance is " + distance);
        //sanity check remove for prod
        assert distance <= 256 : "distance greater than 256! Distance" + distance;

        //get bucket key would be stored in
        HashMap<String, Record> bucket = routingTable.get(distance);
        //if no bucket exists then return null we do not have Record
        if (bucket == null)
            return null;
        //Attempt to find record in bucket(will return null if not found)
        log.trace(logNetDB());
        return bucket.get(Base64.toBase64String(key));
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
        int startDistance = calculateXORMetric(routerInfo.getHash(), key);
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

        int startDistance = calculateXORMetric(routerInfo.getHash(), key);
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
     *
     * @return a formatted text snapshot (ready for logging or console output)
     */
    public String logNetDB() {
        StringBuilder out = new StringBuilder();

        // Buckets are numbered 0-256 (inclusive).  Highest first for readability.
        for (int idx = 256; idx >= 0; idx--) {
            HashMap<String, Record> bucket = routingTable.get(idx);
            if (bucket == null || bucket.isEmpty()) {
                continue;                 // skip empty buckets
            }

            out.append(String.format("%n--- Bucket %-3d (size %d) %s%n",
                    idx,
                    bucket.size(),
                    "─".repeat(40)));

            // Stable order helps diff successive dumps
            bucket.keySet()
                    .stream()
                    .sorted()               // alphabetical
                    .forEach(key -> out.append("   ").append(key).append('\n'));
        }
        return out.toString();
    }


}
