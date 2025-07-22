package org.mart.theo.app;

import org.json.JSONArray;
import org.json.JSONObject;

public class Helper {
	public static JSONObject getJSONObjectFromKeyOrNull(JSONArray array, String key) {
		for (int i = 0; i < array.length(); i++) {
			JSONObject object = array.getJSONObject(i);
			if (key.equals(object.getString("key")))
				return object;
		}
		return null;
	}
}
