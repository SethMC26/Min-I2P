package testPeers;

import org.bouncycastle.util.encoders.Base64;

import common.I2P.I2NP.EndpointPayload;
import merrimackutil.json.types.JSONObject;

public class testSerDe {
    public static void main(String[] args) {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("Message", "You will find moonlit nights strangely empty...");
        jsonObject.put("From:", "QRzMbzIJLv3zeE9aZ7RE37eWbNCZQcsa41QoWnaR/3I=");
        jsonObject.put("To:", "PLo5CdKA/xDZLMYqmeU/+fe1IeIBokn4eXmTSdM3hcg=");

        EndpointPayload payload = new EndpointPayload(2037985876,
                Base64.decode("Nw06R4xtN4mwu3N0G/D3mHEcxfPAy8lt30bLD4z8ef0="), jsonObject);
        JSONObject serialized = payload.toJSONType();
        System.out.println("Serialized EndpointPayload: " + serialized.getFormattedJSON());

        try {
            EndpointPayload deserializedPayload = new EndpointPayload(serialized);
            System.out.println("Deserialized EndpointPayload: " + deserializedPayload.toJSONType().getFormattedJSON());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
