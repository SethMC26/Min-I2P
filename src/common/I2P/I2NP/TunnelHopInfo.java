package common.I2P.I2NP;

import merrimackutil.json.JSONSerializable;
import merrimackutil.json.types.JSONObject;
import merrimackutil.json.types.JSONType;
import org.bouncycastle.util.encoders.Base64;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.io.InvalidObjectException;

public class TunnelHopInfo implements JSONSerializable {
    byte[] routerHash; // Router identity hash 16 byte for toPeer
    // PublicKey publicKey; // ElGamal key (used during build) do we need this? <-yes for later encryption? along hops? -Seth
    SecretKey layerKey; // AES key for that hop
    SecretKey ivKey; // AES IV key
    int sendTunnelId; // Tunnel ID for the next hop

    /**
     * Constructor for TunnelHopInfo
     * 
     * @param routerHash
     * @param layerKey
     * @param ivKey
     * @param sendTunnelId
     */
    public TunnelHopInfo(byte[] routerHash, SecretKey layerKey, SecretKey ivKey, int sendTunnelId) {
        this.routerHash = routerHash;
        // this.publicKey = publicKey;
        this.layerKey = layerKey;
        this.ivKey = ivKey;
        this.sendTunnelId = sendTunnelId;
    }

    public TunnelHopInfo(JSONObject object) {
        try {
            deserialize(object);
        } catch (InvalidObjectException e) {
            e.printStackTrace();
        }
    }

    public byte[] getRouterHash() {
        return routerHash;
    }

    // public PublicKey getPublicKey() {
    // return publicKey;
    // }

    public SecretKey getLayerKey() {
        return layerKey;
    }

    public SecretKey getIvKey() {
        return ivKey;
    }

    public int getSendTunnelId() {
        return sendTunnelId;
    }

    @Override
    public void deserialize(JSONType arg0) throws InvalidObjectException {
        if (!(arg0 instanceof JSONObject))
            throw new InvalidObjectException("Expected JSONObject");

        JSONObject json = (JSONObject) arg0;
        json.checkValidity(new String[] {"routerHash", "layerKey", "ivKey", "sendTunnelId"});
        this.routerHash = Base64.decode(json.getString("routerHash"));
        //im not sure new SecretKeySpec will work here? but maybe if you see an error try using KeyFactor instead with SecretKeySpec
        this.layerKey = new SecretKeySpec(Base64.decode(json.getString("layerKey")), "AES");
        this.ivKey = new SecretKeySpec(Base64.decode(json.getString("ivKey")), "AES");
        this.sendTunnelId = json.getInt("sendTunnelId");
    }

    @Override
    public JSONType toJSONType() {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("routerHash", Base64.toBase64String(routerHash));
        jsonObject.put("layerKey", Base64.toBase64String(layerKey.getEncoded()));
        jsonObject.put("ivKey", Base64.toBase64String(ivKey.getEncoded()));
        jsonObject.put("sendTunnelId", sendTunnelId);
        return jsonObject;
    }

}
