package org.mart.theo.app;

import java.awt.AWTException;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Image;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.SystemTray;
import java.awt.Toolkit;
import java.awt.TrayIcon;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;

import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

import org.apache.logging.log4j.LogManager;

public class TrayApp extends TrayIcon {

	public TrayApp(Image image, String tooltip) {
		super(image, tooltip);
		TrayApp trayIcon = this;
		try {
			SystemTray tray = SystemTray.getSystemTray();

			setImageAutoSize(true);
			JPopupMenu popup = new JPopupMenu();
			JMenuItem mi = new JMenuItem("Quitter");
			mi.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					tray.remove(trayIcon);
					System.exit(0);
				}
			});
			popup.add(mi);

			addMouseListener(new MouseAdapter() {
				@Override
				public void mouseClicked(MouseEvent e) {
					if (e.getButton() == MouseEvent.BUTTON3 && e.getClickCount() == 1) {
						Rectangle bounds = getSafeScreenBounds(e.getPoint());
						Point point = e.getPoint();
						int x = point.x;
						int y = point.y;
						if (y < bounds.y) {
							y = bounds.y;
						} else if (y > bounds.y + bounds.height) {
							y = bounds.y + bounds.height;
						}
						if (x < bounds.x) {
							x = bounds.x;
						} else if (x > bounds.x + bounds.width) {
							x = bounds.x + bounds.width;
						}
						if (x + popup.getPreferredSize().width > bounds.x + bounds.width) {
							x = (bounds.x + bounds.width) - popup.getPreferredSize().width;
						}
						if (y + popup.getPreferredSize().height > bounds.y + bounds.height) {
							y = (bounds.y + bounds.height) - popup.getPreferredSize().height;
						}
						popup.setLocation(x, y);
						popup.setVisible(true);
					}
				}

				@Override
				public void mousePressed(MouseEvent e) {
					if (e.getClickCount() >= 2) {
						App.frame.setVisible(true);
					}
				}
			});

			tray.add(trayIcon);
		} catch (AWTException e) {
			e.printStackTrace();
			LogManager.getLogger(getClass()).error("Unable to initialize tray - exiting app...");
			App.showErrorWindowAndExitApp("Unable to initialize tray - The app will close.");
		}
	}

	public static Rectangle getSafeScreenBounds(Point pos) {
		Rectangle bounds = getScreenBoundsAt(pos);
		Insets insets = getScreenInsetsAt(pos);

		bounds.x += insets.left;
		bounds.y += insets.top;
		bounds.width -= (insets.left + insets.right);
		bounds.height -= (insets.top + insets.bottom);

		return bounds;
	}

	public static Insets getScreenInsetsAt(Point pos) {
		GraphicsDevice gd = getGraphicsDeviceAt(pos);
		Insets insets = null;
		if (gd != null) {
			insets = Toolkit.getDefaultToolkit().getScreenInsets(gd.getDefaultConfiguration());
		}
		return insets;
	}

	public static Rectangle getScreenBoundsAt(Point pos) {
		GraphicsDevice gd = getGraphicsDeviceAt(pos);
		Rectangle bounds = null;
		if (gd != null) {
			bounds = gd.getDefaultConfiguration().getBounds();
		}
		return bounds;
	}

	public static GraphicsDevice getGraphicsDeviceAt(Point pos) {
		GraphicsDevice device = null;

		GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
		GraphicsDevice lstGDs[] = ge.getScreenDevices();

		ArrayList<GraphicsDevice> lstDevices = new ArrayList<GraphicsDevice>(lstGDs.length);

		for (GraphicsDevice gd : lstGDs) {
			GraphicsConfiguration gc = gd.getDefaultConfiguration();
			Rectangle screenBounds = gc.getBounds();
			if (screenBounds.contains(pos)) {
				lstDevices.add(gd);
			}
		}

		if (lstDevices.size() > 0) {
			device = lstDevices.get(0);
		} else {
			device = ge.getDefaultScreenDevice();
		}

		return device;
	}

}
