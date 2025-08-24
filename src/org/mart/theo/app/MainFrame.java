package org.mart.theo.app;

import java.awt.Color;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import javax.imageio.ImageIO;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;
import javax.swing.text.AbstractDocument;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;

import org.json.JSONArray;
import org.json.JSONObject;

public class MainFrame extends JFrame {
	private static final long serialVersionUID = 75604763757012643L;
	public static final String LABEL_KEY = "label";
	public static final String PRICES_KEY = "prices";
	public static final String THRESHOLD_KEY = "threshold";
	public static final String ALERT_KEY = "alert";
	public static final int MAX_INPUT_CHARACTERS = 4;
	public static final Color GREEN_TEXT = new Color(0, 175, 0);
	public static final Color REGULAR_TEXT = new Color(51, 51, 51);
	public static final Font HEADER_FONT = new Font("Arial", Font.BOLD, 14);
	public static final Font CELL_FONT = new Font("Arial", Font.PLAIN, 14);
	private boolean formChanged = false;

	public boolean isFormChanged() {
		return formChanged;
	}

	public void setFormChanged(boolean formChanged) {
		this.formChanged = formChanged;
		saveButton.setEnabled(formChanged);
	}

	private JPanel container;
	private JButton saveButton;
	private JButton resetButton;
	private JLabel lastUpdate;
	private EventsPopup eventsPopup;

	public Map<String, Map<String, JComponent>> key2type2component = new HashMap<>();

	public MainFrame() throws IOException {
		super("Guild Wars Stock Alert");
		initialize();
	}

	public EventsPopup getEventsPopup() {
		return eventsPopup;
	}

	public void initialize() throws IOException {
		JSONArray refs = App.getData().getJSONArray("refs");
		setIconImage(new ImageIcon(App.class.getResource("/img/margrid_64x64.png")).getImage());
		setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
//		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		final int rows = refs.length() + 1;
		final int columns = 5;

		container = new JPanel();
		container.setLayout(new BoxLayout(container, BoxLayout.Y_AXIS));

		// ----------- header -----------
		JPanel headerPanel = new JPanel();
		headerPanel.setBorder(new EmptyBorder(20, 20, 0, 20));// top,left,bottom,right
		headerPanel.setLayout(new BoxLayout(headerPanel, BoxLayout.X_AXIS));
		headerPanel.setVisible(true);
		JLabel titleLabel = new JLabel("Matériaux");
		titleLabel.setFont(new Font("Arial", Font.BOLD, 20));
		headerPanel.add(new JLabel(getScaledImage("/img/margrid.png", 0.2d)));
		headerPanel.add(titleLabel);
		headerPanel.add(Box.createHorizontalGlue());
		saveButton = new JButton("Sauvegarder");
		saveButton.setEnabled(false);
		saveButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				int result = JOptionPane.showConfirmDialog(container, "Voulez-vous vraiment écraser la configuration ?",
						"Confirmation", JOptionPane.CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE);
				if (result == JOptionPane.OK_OPTION) {
					try {
						App.saveConfig();
					} catch (IOException e1) {
						e1.printStackTrace();
					}
				}
			}
		});
		resetButton = new JButton("Réinitialiser");
		resetButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				int result = JOptionPane.showConfirmDialog(container,
						"Voulez-vous vraiment réinitialiser la configuration à son état par défaut ?", "Confirmation",
						JOptionPane.CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE);
				if (result == JOptionPane.OK_OPTION) {
					try {
						App.resetConfig();
					} catch (IOException e1) {
						e1.printStackTrace();
					}
				}
			}
		});
		headerPanel.add(resetButton);
		headerPanel.add(new JLabel("    "));
		headerPanel.add(saveButton);
		container.add(headerPanel);

		// ----------- body -----------
		JPanel bodyPanel = new JPanel();
		bodyPanel.setLayout(new GridLayout(rows, columns));
		bodyPanel.setVisible(true);
		bodyPanel.setBorder(new EmptyBorder(0, 0, 10, 0));

		// ----------- table header -----------
		bodyPanel.add(new JLabel());
		JLabel materialLabel = new JLabel("Type");
		materialLabel.setFont(HEADER_FONT);
		bodyPanel.add(materialLabel);
		JLabel pricesLabel = new JLabel("Prix");
		pricesLabel.setFont(HEADER_FONT);
		bodyPanel.add(pricesLabel);
		JLabel sellingPointLabel = new JLabel("Seuil de vente");
		sellingPointLabel.setFont(HEADER_FONT);
		bodyPanel.add(sellingPointLabel);
		JLabel alertLabel = new JLabel("Alerte");
		alertLabel.setFont(HEADER_FONT);
		bodyPanel.add(alertLabel);

		// ----------- table body -----------
		for (int i = 0; i < refs.length(); i++) {
			JSONObject ref = refs.getJSONObject(i);
			String key = ref.getString("key");
			ImageIcon icon = getScaledImage("/img/materials/" + key + ".png", 0.6d);
			bodyPanel.add(new JLabel(icon));

			JLabel name = new JLabel(ref.getString("label"));
			name.setName(key);
			name.setFont(CELL_FONT);
			bodyPanel.add(name);
			key2type2component.put(key, new HashMap<String, JComponent>());
			key2type2component.get(key).put(LABEL_KEY, name);

			JTextArea prices = new JTextArea("A : " + "\nV : ");
			prices.setEditable(false);
			prices.setOpaque(false);
			prices.setFont(CELL_FONT);
			bodyPanel.add(prices);
			key2type2component.get(key).put(PRICES_KEY, prices);

			String sellingPoint = "";
			if (!JSONObject.NULL.equals(ref.get("sellingPoint")))
				sellingPoint = Integer.toString(ref.getInt("sellingPoint"));
			JTextField priceThreshold = new JTextField(sellingPoint);
			AbstractDocument document = (AbstractDocument) priceThreshold.getDocument();
			document.setDocumentFilter(new DocumentFilter() {
				public void replace(FilterBypass fb, int offs, int length, String str, AttributeSet a)
						throws BadLocationException {
					String text = fb.getDocument().getText(0, fb.getDocument().getLength());
					text += str;
					if ((fb.getDocument().getLength() + str.length() - length) <= MAX_INPUT_CHARACTERS
							&& text.matches("[0-9]+")) {
						super.replace(fb, offs, length, str, a);
						setFormChanged(true);
						text = fb.getDocument().getText(0, fb.getDocument().getLength());
						ref.put("sellingPoint", Integer.parseInt(text));
					} else {
						Toolkit.getDefaultToolkit().beep();
					}
				}

				public void insertString(FilterBypass fb, int offs, String str, AttributeSet a)
						throws BadLocationException {
					String text = fb.getDocument().getText(0, fb.getDocument().getLength());
					text += str;
					if ((fb.getDocument().getLength() + str.length()) <= MAX_INPUT_CHARACTERS
							&& text.matches("[0-9]+")) {
						super.insertString(fb, offs, str, a);
						setFormChanged(true);
						text = fb.getDocument().getText(0, fb.getDocument().getLength());
						ref.put("sellingPoint", Integer.parseInt(text));
					} else {
						Toolkit.getDefaultToolkit().beep();
					}
				}

				public void remove(FilterBypass fb, int offset, int length) throws BadLocationException {
					super.remove(fb, offset, length);
					setFormChanged(true);
					String text = fb.getDocument().getText(0, fb.getDocument().getLength());
					ref.put("sellingPoint", text.isEmpty() ? JSONObject.NULL : Integer.parseInt(text));
				}
			});
			bodyPanel.add(priceThreshold);
			key2type2component.get(key).put(THRESHOLD_KEY, priceThreshold);

			JCheckBox alertCheck = new JCheckBox();
			alertCheck.setSelected(ref.getBoolean("isAlert"));
			alertCheck.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent event) {
					JCheckBox cb = (JCheckBox) event.getSource();
					ref.put("isAlert", cb.isSelected());
					setFormChanged(true);
				}
			});
			bodyPanel.add(alertCheck);
			key2type2component.get(key).put(ALERT_KEY, alertCheck);
		}
		container.add(bodyPanel);

		JPanel updatedAtPanel = new JPanel();
		updatedAtPanel.setVisible(true);
		updatedAtPanel.setBorder(new EmptyBorder(0, 0, 10, 0));
		updatedAtPanel.add(new JLabel("Dernière mise à jour :"));
		lastUpdate = new JLabel();
		updatedAtPanel.add(lastUpdate);
		container.add(updatedAtPanel);

		add(container);
		setJMenuBar(new MenuBar());

		pack();
		setLocationRelativeTo(null);

		eventsPopup = new EventsPopup(this);
//		setVisible(true);
	}

	public void changeTextColor(String key, Color color) {
		JLabel label = (JLabel) App.frame.key2type2component.get(key).get(MainFrame.LABEL_KEY);
		label.setForeground(color);
		JTextArea prices = (JTextArea) App.frame.key2type2component.get(key).get(MainFrame.PRICES_KEY);
		prices.setForeground(color);
	}

	public void changeUpdateDateLabel(String newDate) {
		lastUpdate.setText(newDate);
	}

	public ImageIcon getScaledImage(String resourcePath, double scale) throws IOException {
		InputStream stream = App.class.getResourceAsStream(resourcePath);
		BufferedImage bi = ImageIO.read(stream);
		ImageIcon icon = new ImageIcon(
				new ImageIcon(bi).getImage().getScaledInstance((int) Math.round(bi.getWidth() * scale),
						(int) Math.round(bi.getHeight() * scale), Image.SCALE_DEFAULT));
		return icon;
	}

//	private Component getComponentById(Container container, String componentId) {
//		if (container.getComponents().length > 0) {
//			for (Component c : container.getComponents()) {
//				if (componentId.equals(c.getName())) {
//					return c;
//				}
//				if (c instanceof Container) {
//					return getComponentById((Container) c, componentId);
//				}
//			}
//		}
//		return null;
//	}
}
