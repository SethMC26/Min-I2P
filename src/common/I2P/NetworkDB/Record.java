package common.I2P.NetworkDB;

import merrimackutil.json.JSONSerializable;

import java.security.PublicKey;

/**
 * Records are entires that can be stored in netDB
 */
public abstract class Record implements JSONSerializable {
    /**
     * Type of record {@code LEASESET or ROUTERINFO}
     * @apiNote made to help with casting
     *
     */
    public enum RecordType {
        LEASESET,
        ROUTERINFO
    }

    /**
     * Type of Record {@code LEASESET or ROUTERINFO}
     */
    private RecordType recordType;

    /**
     * Create new Record
     * @param recordType Type of Record {@code LEASESET or ROUTERINFO}
     */
    Record(RecordType recordType) {
        this.recordType = recordType;
    }

    /**
     * Get hash of this record
     * @return 32 byte SHA256 hash
     */
    public abstract byte[] getHash();

    /**
     * Get type of record
     * @return Type of Record {@code LEASESET or ROUTERINFO}
     */
    public RecordType getRecordType() {
        return recordType;
    }

    /**
     * Verify the signature of this record given a public key
     * @return true if signature is valid false otherwise
     */
    public abstract boolean verifySignature();
}
