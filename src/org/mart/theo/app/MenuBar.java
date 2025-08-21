package org.mart.theo.app;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;

public class MenuBar extends JMenuBar {
	private static final long serialVersionUID = -2554234560206139641L;

	MenuBar() {
		super();

		JMenu fileMenu = new JMenu("Fichier");
		JMenuItem quitItem = new JMenuItem("Quitter");
		quitItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				System.exit(0);
			}
		});
		fileMenu.add(quitItem);
		add(fileMenu);

		JMenu editMenu = new JMenu("Éditer");
		JMenuItem eventItem = new JMenuItem("Évènements...");
		eventItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				App.frame.getEventsPopup().setVisible(true);
			}
		});
		editMenu.add(eventItem);
		add(editMenu);
	}
}
