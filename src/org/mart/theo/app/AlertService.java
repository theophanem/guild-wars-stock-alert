package org.mart.theo.app;

import java.awt.TrayIcon.MessageType;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.swing.JTextArea;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;

public class AlertService extends Thread {
	private static AlertService instance;

	public static AlertService getInstance() {
		if (instance == null)
			instance = new AlertService();
		return instance;
	}

	private AlertService() {
		setName("Alert service");
	}

	@Override
	public void run() {
		while (isInterrupted() == false) {
			try {
				process();
			} catch (Exception e1) {
				Thread.currentThread().interrupt();
				e1.printStackTrace();
			}

			try {
				Thread.sleep(60_000l);
			} catch (Throwable t) {
				interrupt();
			}
		}
	}

	public synchronized void process() throws Exception {
		Logger logger = LogManager.getLogger(AlertService.class);
		logger.info("Fetching data...");
		DataFetcher fetcher = new DataFetcher();
		JSONObject fetchedData = fetcher.fetch();
		// COMPARE WITH PURCHASE PRICE BECAUSE MORE DATA THAN SELLING PRICE

		long updatedAtLong = fetchedData.getInt("updated_at");
		Date updatedAt = new Date(updatedAtLong * 1000);
		if (!updatedAt.equals(App.getLastUpdate()))
			App.setLastUpdate(updatedAt);
		logger.info("Updated at: " + updatedAtLong + " (" + updatedAt + ")");
		JSONObject data = App.getData();
		JSONArray refs = data.getJSONArray("refs");
		List<String> toSell = new ArrayList<>();
		for (int i = 0; i < refs.length(); i++) {
			JSONObject ref = refs.getJSONObject(i);
			String key = ref.getString("key");

			String id = Integer.toString(ref.getInt("id"));
			JSONObject fetchedDataBuy = fetchedData.getJSONObject("buy").has(id)
					? fetchedData.getJSONObject("buy").getJSONObject(id)
					: null;
			JSONObject fetchedDataSell = fetchedData.getJSONObject("sell").has(id)
					? fetchedData.getJSONObject("sell").getJSONObject(id)
					: null;

			ref.put("buy", fetchedDataBuy != null ? fetchedDataBuy.getInt("p") : null);
			ref.put("sell", fetchedDataSell != null ? fetchedDataSell.getInt("p") : null);

			App.frame.changeTextColor(key, MainFrame.REGULAR_TEXT);
			if (fetchedDataBuy != null) {
				if (!JSONObject.NULL.equals(ref.get("sellingPoint")) && !JSONObject.NULL.equals(fetchedDataBuy.get("p"))
						&& ref.getInt("sellingPoint") <= fetchedDataBuy.getInt("p")) {
					App.frame.changeTextColor(key, MainFrame.GREEN_TEXT);
					if (ref.getBoolean("isAlert")) {
						if (!ref.has("notified") || !ref.getBoolean("notified"))
							toSell.add(ref.getString("label"));
						ref.put("notified", true);
					} else
						ref.put("notified", false);
				} else {
					ref.put("notified", false);
				}
			}
			JTextArea prices = (JTextArea) App.frame.key2type2component.get(key).get(MainFrame.PRICES_KEY);
			prices.setText("A : " + (fetchedDataBuy != null ? fetchedDataBuy.getInt("p") : "n/a") + "\nV : "
					+ (fetchedDataSell != null ? fetchedDataSell.getInt("p") : "n/a"));
		}
		if (!toSell.isEmpty()) {
			String message = "C'est l'heure de vendre :";
			for (String label : toSell)
				message += "\n - " + label;

			App.tray.displayMessage("Guild Wars Stock Alert", message, MessageType.INFO);
		}

		logger.info("Done fetching data");
	}
}
