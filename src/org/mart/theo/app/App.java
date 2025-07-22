package org.mart.theo.app;

import java.awt.Image;
import java.awt.SystemTray;
import java.awt.Toolkit;
import java.awt.TrayIcon.MessageType;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JTextField;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;

public class App {
	public static TrayApp tray = null;
	public static Logger rootLogger = null;
	private static JSONObject config = null;
	private static JSONObject data = null;
	public static MainFrame frame = null;
	private static File workingDirectory;
	private static Date lastUpdate = null;
	public final static SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/YYYY 'à' HH:mm:ss", Locale.FRENCH);
	private static List<JSONObject> currentEvents = new ArrayList<>();

	public static void main(String[] args) throws IOException {
		rootLogger = (Logger) LogManager.getRootLogger();
		rootLogger.info("Started application");

		String workingDirectoryPath;
		String OS = (System.getProperty("os.name")).toUpperCase();
		if (OS.contains("WIN")) {
			workingDirectoryPath = System.getenv("AppData") + File.separator + "GuildWarsStockAlert" + File.separator;
		} else if (OS.contains("MAC")) {
			workingDirectoryPath = System.getProperty("user.home") + File.separator + "Documents" + File.separator
					+ "GuildWarsStockAlert" + File.separator;
		} else {
			workingDirectoryPath = System.getProperty("user.home") + File.separator + "GuildWarsStockAlert"
					+ File.separator;
		}
		workingDirectory = new File(workingDirectoryPath);
		workingDirectory.mkdir();

		loadConfig();
		initTray();
		checkEvent();

		frame = new MainFrame();

		ServiceHandler serviceHandler = new ServiceHandler();
		serviceHandler.start();
	}

	public static JSONObject getConfig() {
		return App.config;
	}

	public static void setConfig(JSONObject config) {
		App.config = config;
	}

	public static JSONObject getData() {
		return App.data;
	}

	public static void setData(JSONObject data) {
		App.data = data;
	}

	public static Date getLastUpdate() {
		return lastUpdate;
	}

	public static void setLastUpdate(Date lastUpdate) {
		App.lastUpdate = lastUpdate;
		frame.changeUpdateDateLabel(sdf.format(lastUpdate));
	}

	private static void initTray() {
		if (SystemTray.isSupported()) {
			Image image = Toolkit.getDefaultToolkit().createImage(App.class.getResource("/img/margrid_64x64.png"));
			tray = new TrayApp(image, "Guild Wars Stock Alert");
		} else {
			rootLogger.error("Operating system does not support trays - exiting app...");
			showErrorWindowAndExitApp("Operating system does not support trays - The app will close.");
		}
	}

	private static void loadConfig() {
		try {
			File configFile = new File(workingDirectory, "config.json");
			if (configFile.createNewFile()) {
				InputStream is = App.class.getResourceAsStream("/defaultConfig.json");
				String content = new String(is.readAllBytes(), StandardCharsets.UTF_8);
				FileWriter writer = new FileWriter(configFile, false);
				writer.write(content);
				writer.close();
			}
			byte[] encoded = Files.readAllBytes(configFile.toPath());
			String content = new String(encoded, StandardCharsets.UTF_8);
			JSONObject jsonObject = new JSONObject(content);
			setConfig(jsonObject);
			setData(jsonObject);
		} catch (Exception e) {
			e.printStackTrace();
			showErrorWindowAndExitApp("Cannot load settings file");
		}
	}

	public static void saveConfig() throws IOException {
		JSONObject newConfig = new JSONObject(getConfig().toString());
		JSONArray refs = newConfig.getJSONArray("refs");
		JSONArray dataRefs = getData().getJSONArray("refs");
		for (int i = 0; i < refs.length(); i++) {
			JSONObject ref = refs.getJSONObject(i);
			String key = ref.getString("key");
			JSONObject dataRef = Helper.getJSONObjectFromKeyOrNull(dataRefs, key);
			Integer sellingPoint = !JSONObject.NULL.equals(dataRef.get("sellingPoint")) ? dataRef.getInt("sellingPoint")
					: null;
			ref.put("sellingPoint", sellingPoint != null ? sellingPoint : JSONObject.NULL);
			ref.put("isAlert", dataRef.getBoolean("isAlert"));

			Integer buyingPrice = dataRef.has("buy") && !JSONObject.NULL.equals(dataRef.get("buy"))
					? dataRef.getInt("buy")
					: null;
			if (sellingPoint != null && buyingPrice != null && sellingPoint.intValue() <= buyingPrice.intValue())
				App.frame.changeTextColor(key, MainFrame.GREEN_TEXT);
			else
				App.frame.changeTextColor(key, MainFrame.REGULAR_TEXT);
		}
		try (OutputStreamWriter writer = new OutputStreamWriter(
				new FileOutputStream(new File(workingDirectory, "config.json"), false), StandardCharsets.UTF_8)) {
			writer.write(newConfig.toString());
			writer.close();
		}
		frame.setFormChanged(false);

		JOptionPane.showMessageDialog(frame, "Succès de la sauvegarde", "Configuration sauvegardée",
				JOptionPane.INFORMATION_MESSAGE);
	}

	public static void resetConfig() throws IOException {
		JSONObject newConfig = new JSONObject(getConfig().toString());
		JSONArray refs = newConfig.getJSONArray("refs");
		JSONArray dataRefs = getData().getJSONArray("refs");

		InputStream is = App.class.getResourceAsStream("/defaultConfig.json");
		String content = new String(is.readAllBytes(), StandardCharsets.UTF_8);
		JSONObject defaultConfig = new JSONObject(content);
		for (int i = 0; i < refs.length(); i++) {
			JSONObject ref = refs.getJSONObject(i);
			String key = ref.getString("key");
			JSONObject defaultRef = Helper.getJSONObjectFromKeyOrNull(defaultConfig.getJSONArray("refs"), key);
			JSONObject dataRef = Helper.getJSONObjectFromKeyOrNull(dataRefs, key);
			Integer sellingPoint = !JSONObject.NULL.equals(defaultRef.get("sellingPoint"))
					? defaultRef.getInt("sellingPoint")
					: null;
			boolean isAlert = defaultRef.getBoolean("isAlert");

			JTextField thresholdField = (JTextField) frame.key2type2component.get(key).get(MainFrame.THRESHOLD_KEY);
			JCheckBox alertCheckBox = (JCheckBox) frame.key2type2component.get(key).get(MainFrame.ALERT_KEY);
			ref.put("sellingPoint", sellingPoint != null ? sellingPoint : JSONObject.NULL);
			dataRef.put("sellingPoint", sellingPoint != null ? sellingPoint : JSONObject.NULL);
			thresholdField.setText(sellingPoint != null ? sellingPoint.toString() : "");
			ref.put("isAlert", isAlert);
			dataRef.put("isAlert", isAlert);
			alertCheckBox.setSelected(isAlert);

			Integer buyingPrice = dataRef.has("buy") && !JSONObject.NULL.equals(dataRef.get("buy"))
					? dataRef.getInt("buy")
					: null;
			if (sellingPoint != null && buyingPrice != null && sellingPoint.intValue() <= buyingPrice.intValue())
				frame.changeTextColor(key, MainFrame.GREEN_TEXT);
			else
				frame.changeTextColor(key, MainFrame.REGULAR_TEXT);
		}
		try (OutputStreamWriter writer = new OutputStreamWriter(
				new FileOutputStream(new File(workingDirectory, "config.json"), false), StandardCharsets.UTF_8)) {
			writer.write(newConfig.toString());
			writer.close();
		}
		frame.setFormChanged(false);

		JOptionPane.showMessageDialog(frame, "Succès de la réinitialisation", "Configuration réinitialisée",
				JOptionPane.INFORMATION_MESSAGE);
	}

	public static void showErrorWindowAndExitApp(String message) {
		JOptionPane.showMessageDialog(new JFrame(), message, "Dialog", JOptionPane.ERROR_MESSAGE);
		System.exit(0);
	}

	private static void checkEvent() throws IOException {
		InputStream is = App.class.getResourceAsStream("/events.json");
		String content = new String(is.readAllBytes(), StandardCharsets.UTF_8);
		JSONArray events = new JSONArray(content);
		Calendar today = Calendar.getInstance();
		int currentYear = today.get(Calendar.YEAR);
		for (int i = 0; i < events.length(); i++) {
			JSONObject event = events.getJSONObject(i);
			Calendar from = Calendar.getInstance();
			Calendar to = Calendar.getInstance();
			String[] fromDayAndMonth = event.getString("from").split("/");
			String[] toDayAndMonth = event.getString("to").split("/");

			if ("wintersday".equals(event.getString("key"))) {
				int currentMonth = today.get(Calendar.MONTH);
				from.set(currentMonth == 0 ? currentYear - 1 : currentYear, Integer.parseInt(fromDayAndMonth[1]) - 1,
						Integer.parseInt(fromDayAndMonth[0]), 0, 0, 0);
				to.set(currentMonth == 0 ? currentYear : currentYear + 1, Integer.parseInt(toDayAndMonth[1]) - 1,
						Integer.parseInt(toDayAndMonth[0]), 23, 59, 59);
			} else {
				from.set(currentYear, Integer.parseInt(fromDayAndMonth[1]) - 1, Integer.parseInt(fromDayAndMonth[0]), 0,
						0, 0);
				to.set(currentYear, Integer.parseInt(toDayAndMonth[1]) - 1, Integer.parseInt(toDayAndMonth[0]), 23, 59,
						59);
			}

			if (from.before(today) && today.before(to))
				currentEvents.add(event);
		}

		if (currentEvents.size() > 0) {
			if (currentEvents.size() == 1)
				tray.displayMessage("Guild Wars Event Alert",
						"Évènement en cours : " + currentEvents.get(0).getString("label"), MessageType.INFO);
			else {
				String message = "Évènements en cours :";
				for (JSONObject e : currentEvents)
					message += "\n - " + e.getString("label");
				tray.displayMessage("Guild Wars Event Alert", message, MessageType.INFO);
			}
		}
	}

}
