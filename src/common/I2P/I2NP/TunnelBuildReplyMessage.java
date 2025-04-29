package common.I2P.I2NP;

import java.io.InvalidObjectException;
import java.util.Base64;
import java.util.List;

import merrimackutil.json.types.JSONArray;
import merrimackutil.json.types.JSONObject;
import merrimackutil.json.types.JSONType;

public class TunnelBuildReplyMessage extends I2NPMessage {
    private List<TunnelBuild.Record> records;

    public TunnelBuildReplyMessage(List<TunnelBuild.Record> records) {
        this.records = records;
    }

    @Override
    public void deserialize(JSONType jsonType) throws InvalidObjectException {
        if (!(jsonType instanceof JSONObject)) {
            throw new InvalidObjectException("Expected a JSONObject for TunnelBuildReplyMessage");
        }

        JSONObject jsonObject = (JSONObject) jsonType;

        // Ensure the JSON object contains the records key
        if (!jsonObject.containsKey("records")) {
            throw new InvalidObjectException("Missing 'records' key in TunnelBuildReplyMessage JSON");
        }

        // Extract the records array
        JSONArray recordsArray = jsonObject.getArray("records");

        // Clear the current records list
        records.clear();

        // Deserialize each record in the array
        for (int i = 0; i < recordsArray.size(); i++) {
            JSONObject recordJSON = recordsArray.getObject(i);
            TunnelBuild.Record record = new TunnelBuild.Record(recordJSON);
            records.add(record);
        }
    }

    // yeah this is probably valid
    @Override
    public JSONObject toJSONType() {
        JSONArray jsonArray = new JSONArray();
        for (TunnelBuild.Record record : records) {
            jsonArray.add(record.toJSONType());
        }
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("records", jsonArray);
        return jsonObject;
    }

}
