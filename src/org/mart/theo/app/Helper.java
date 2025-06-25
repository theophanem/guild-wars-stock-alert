package org.mart.theo.app;

import org.json.JSONArray;
import org.json.JSONObject;

public class Helper {
	public static JSONObject getMaterialFromKeyOrNull(JSONArray materials, String key) {
		for (int i = 0; i < materials.length(); i++) {
			JSONObject material = materials.getJSONObject(i);
			if (key.equals(material.getString("key")))
				return material;
		}
		return null;
	}
}
