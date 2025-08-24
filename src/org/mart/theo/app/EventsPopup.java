package org.mart.theo.app;

import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.font.TextAttribute;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumnModel;

import org.json.JSONArray;
import org.json.JSONObject;

public class EventsPopup extends JDialog {
	private static final long serialVersionUID = 1769123847007113831L;
	public static final Color GREEN_BACKGROUND = new Color(127, 201, 127);
	public static final Color LINK_TEXT = new Color(0, 0, 238);
	public static final Color LINK_CLICK_TEXT = new Color(255, 0, 0);
	private boolean formChanged = false;

	private List<String> rowsToHighlight = new ArrayList<>();

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
	private JSONArray events;

	public Map<String, Map<String, JComponent>> key2type2component = new HashMap<>();

	public EventsPopup(JFrame frame) throws IOException {
		super(frame, "Guild Wars Stock Alert - Évènements", true);
		initialize();
	}

	public void initialize() throws IOException {
		InputStream is = App.class.getResourceAsStream("/events.json");
		String content = new String(is.readAllBytes(), StandardCharsets.UTF_8);
		events = new JSONArray(content);
		setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		setResizable(false);

		final int rows = events.length() + 1;
		final int columns = 6;

		container = new JPanel();
		container.setLayout(new BoxLayout(container, BoxLayout.Y_AXIS));

		// ----------- header -----------
		JPanel headerPanel = new JPanel();
		headerPanel.setBorder(new EmptyBorder(0, 0, 20, 0));// top,left,bottom,right
		headerPanel.setLayout(new BoxLayout(headerPanel, BoxLayout.X_AXIS));
		headerPanel.setVisible(true);
		JLabel titleLabel = new JLabel("Évènements");
		titleLabel.setFont(new Font("Arial", Font.BOLD, 20));
		headerPanel.add(titleLabel);
		headerPanel.add(Box.createHorizontalGlue());
		saveButton = new JButton("Sauvegarder");
		saveButton.setEnabled(false);
		saveButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				String todo = "manage save action";
				// int result = JOptionPane.showConfirmDialog(container, "Voulez-vous vraiment
				// écraser la configuration ?",
				// "Confirmation", JOptionPane.CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE);
				// if (result == JOptionPane.OK_OPTION) {
				// try {
				// App.saveConfig();
				// } catch (IOException e1) {
				// e1.printStackTrace();
				// }
				// }
			}
		});
		resetButton = new JButton("Réinitialiser");
		resetButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				String todo = "manage reset action";
				// int result = JOptionPane.showConfirmDialog(container,
				// "Voulez-vous vraiment réinitialiser la configuration à son état par défaut
				// ?", "Confirmation",
				// JOptionPane.CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE);
				// if (result == JOptionPane.OK_OPTION) {
				// try {
				// App.resetConfig();
				// } catch (IOException e1) {
				// e1.printStackTrace();
				// }
				// }
			}
		});
		headerPanel.add(resetButton);
		headerPanel.add(new JLabel("    "));
		headerPanel.add(saveButton);
		container.add(headerPanel);
		add(container);

		for (JSONObject currentEvent : App.getCurrentEvents())
			rowsToHighlight.add(currentEvent.getString("key"));

		EventTableModel model = new EventTableModel();
		for (int i = 0; i < events.length(); i++) {
			JSONObject event = events.getJSONObject(i);
			Object[] row = new Object[columns];
			row[0] = event.getString("label");
			row[1] = event.getBoolean("isMajor");
			row[2] = event.getBoolean("hasInterestingItems");
			row[3] = event.getString("from");
			row[4] = event.getString("to");
			row[5] = event.getBoolean("hasInterestingItems");
			model.addRow(row);
		}
		JTable table = new JTable(model);
		table.setShowGrid(false);
		table.setCellSelectionEnabled(false);
		table.setRowHeight(30);
		table.setFont(MainFrame.CELL_FONT);
		table.setBackground(null);
		table.getTableHeader().setReorderingAllowed(false);
		TableColumnModel columnModel = table.getColumnModel();
		EventTableCellRenderer cellRenderer = new EventTableCellRenderer();
		columnModel.getColumn(0).setCellRenderer(cellRenderer);
		columnModel.getColumn(1).setCellRenderer(cellRenderer);
		columnModel.getColumn(2).setCellRenderer(cellRenderer);
		columnModel.getColumn(3).setCellRenderer(cellRenderer);
		columnModel.getColumn(4).setCellRenderer(cellRenderer);
		columnModel.getColumn(5).setCellRenderer(cellRenderer);
		table.addMouseListener(new MouseAdapter() {
			public void mouseClicked(MouseEvent e) {
				int row = table.getSelectedRow();
				int col = table.getSelectedColumn();
				if (col == 0)
					try {
						URI uri = new URI(events.getJSONObject(row).getString("link"));
						open(uri);
					} catch (URISyntaxException e1) {
						e1.printStackTrace();
					}
			}

		});

		table.addMouseMotionListener(new MouseMotionAdapter() {
			public void mouseMoved(MouseEvent e) {
				int col = table.columnAtPoint(e.getPoint());
				if (col == 0 || col == 5)
					setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
				else
					setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
			}
		});

		columnModel.getColumn(0).setPreferredWidth(270);
		columnModel.getColumn(1).setPreferredWidth(50);
		columnModel.getColumn(2).setPreferredWidth(100);
		columnModel.getColumn(3).setPreferredWidth(50);
		columnModel.getColumn(4).setPreferredWidth(50);
		columnModel.getColumn(5).setPreferredWidth(50);

		JScrollPane scrollPane = new JScrollPane(table);
		scrollPane.setPreferredSize(new Dimension(570, 30 * rows - 7));
		table.setFillsViewportHeight(true);
		container.setBorder(new EmptyBorder(20, 20, 20, 20));
		container.add(scrollPane);

		pack();
		setLocationRelativeTo(null);
		setVisible(false);
	}

	private static void open(URI uri) {
		if (Desktop.isDesktopSupported()) {
			try {
				Desktop.getDesktop().browse(uri);
			} catch (IOException e) {
				e.printStackTrace();
			}
		} else {
			throw new UnsupportedOperationException("OS doesn't support desktop");
		}
	}

	public class EventTableModel extends DefaultTableModel {
		private static final long serialVersionUID = -1569611711485519837L;

		public EventTableModel() {
			super(new String[] { "Évènement", "Majeur", "Objets d'intérêt", "Du", "Au", "Alerte" }, 0);
		}

		@Override
		public Class<?> getColumnClass(int columnIndex) {
			Class<?> clazz = String.class;
			switch (columnIndex) {
			case 1, 2, 5:
				clazz = Boolean.class;
				break;
			}
			return clazz;
		}

		@Override
		public boolean isCellEditable(int row, int column) {
			return column == 5;
		}

		@Override
		public void setValueAt(Object aValue, int row, int column) {
			if (aValue instanceof Boolean) {
				@SuppressWarnings("unchecked")
				Vector<Boolean> rowData = (Vector<Boolean>) getDataVector().get(row);
				rowData.set(5, (boolean) aValue);
//				events.getJSONObject(row).put("isAlert", aValue);
				fireTableCellUpdated(row, column);
			}
		}

	}

	public class EventTableCellRenderer extends DefaultTableCellRenderer {
		private static final long serialVersionUID = -6562953308971470296L;

		@Override
		public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
				int row, int col) {
			JSONObject event = events.getJSONObject(row);
			boolean toHighlight = rowsToHighlight.contains(event.getString("key"));
			if (value instanceof String) {
				JLabel l = new JLabel((String) value);
				l.setFont(MainFrame.CELL_FONT);
				l.setBorder(new EmptyBorder(0, 5, 0, 0));
				if (col == 0) {
					l.setToolTipText(event.getString("link"));
					Font font = l.getFont();
					Map<TextAttribute, Object> attributes = new HashMap<>(font.getAttributes());
					attributes.put(TextAttribute.UNDERLINE, TextAttribute.UNDERLINE_ON);
					l.setFont(font.deriveFont(attributes));
					if (hasFocus)
						l.setForeground(LINK_CLICK_TEXT);
					else
						l.setForeground(LINK_TEXT);
				}

				if (toHighlight) {
					l.setBackground(GREEN_BACKGROUND);
					l.setOpaque(true);
				}

				return l;
			} else if (value instanceof Boolean) {
				JCheckBox check = new JCheckBox();
				check.setHorizontalAlignment(JCheckBox.CENTER);
				check.setSelected((Boolean) value);
				if (col != 5)
					check.setEnabled(false);
				if (col == 2 && event.has("comments"))
					check.setToolTipText(event.getString("comments"));
				if (toHighlight)
					check.setBackground(GREEN_BACKGROUND);
				return check;
			} else
				throw new IllegalStateException("Cell should contain a boolean or a string");
		}
	}

}
