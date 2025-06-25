package org.mart.theo.app;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import org.json.JSONObject;

public class DataFetcher {

	public DataFetcher() {

	}

	public JSONObject fetch() throws Exception {
		HttpClient client = HttpClient.newHttpClient();
		HttpRequest request = HttpRequest.newBuilder().uri(URI.create("https://kamadan.gwtoolbox.com/")).build();
		HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
		String rawResponse = response.body();
		String rawJson = null;
		if (rawResponse != null) {
			String[] lines = rawResponse.split("\\n");
			for (String line : lines) {
				if (line.trim().startsWith("window.current_trader_quotes = {\"")) {
					rawJson = line.trim().replace("window.current_trader_quotes = ", "");
					rawJson = rawJson.substring(0, rawJson.length() - 1);
				}
			}
		}
		if (rawJson == null)
			throw new Exception("Unable to get and isolate data");

		JSONObject object = new JSONObject(rawJson);
		return object;
	}
}
