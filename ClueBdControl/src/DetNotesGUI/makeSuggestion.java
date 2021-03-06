/**
 * Kira Combs
 * Maria Deslis
 */
package DetNotesGUI;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.*;
import java.awt.BorderLayout;
import java.util.ArrayList;

import javax.swing.*;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;

import ClueBoardGUI.WestPanel;

import main.Board;
import main.Card.CardType;


public class makeSuggestion extends JDialog{
	//Basic GUI Setup
	Board board;
	String room, weapon, person = "";
	private JComboBox personBox, weaponBox;
	
	public makeSuggestion(Board board, String room) {
		this.board = board;
		this.room = room;
		setSize(new Dimension(200,500));
		setTitle(board.getCurrentPlayer().getName() + " is making an suggestion!");
		setLayout(new GridLayout(5, 1)); 		//makes big button
		JPanel roomPanel = new JPanel();
		roomPanel.setBorder(new TitledBorder (new EtchedBorder(), "Room Guess"));
		JLabel roomLabel = new JLabel(room);
		roomPanel.add(roomLabel);
		add(roomPanel);

		JPanel personPanel = new JPanel();
		personPanel.setBorder(new TitledBorder (new EtchedBorder(), "Person Guess"));
		personBox = createPersonCombo();
		personPanel.add(personBox);
		add(personPanel);

		JPanel weaponPanel = new JPanel();
		weaponPanel.setBorder(new TitledBorder (new EtchedBorder(), "Weapon Guess"));
		weaponBox = createWeaponCombo();
		weaponPanel.add(weaponBox);
		add(weaponPanel);

		JButton submit = new JButton("Submit");
		JButton cancel = new JButton("Cancel");
		add(submit);
		add(cancel);
		
		cancel.addActionListener(new CancelListener());
		submit.addActionListener(new SubmitListener());
	}

	private JComboBox createWeaponCombo()
	{
		JComboBox combo = new JComboBox();
		combo.addItem("Pick a weapon");
		for (int i = 0; i < board.getAllCards().size(); ++i) {
			if (board.getAllCards().get(i).getCardType() == CardType.WEAPON) {
				combo.addItem(board.getAllCards().get(i).getName());
			} 
		}
		return combo;
	}

	private JComboBox createPersonCombo()
	{
		JComboBox combo = new JComboBox();
		combo.addItem("Pick a person");
		for (int i = 0; i < board.getAllCards().size(); ++i) {
			if (board.getAllCards().get(i).getCardType() == CardType.PERSON) {
				combo.addItem(board.getAllCards().get(i).getName());
			} 
		}
		return combo;
	}
	private JComboBox createRoomCombo()
	{
		JComboBox combo = new JComboBox();
		combo.addItem("Pick a room");
		for (int i = 0; i < board.getAllCards().size(); ++i) {
			if (board.getAllCards().get(i).getCardType() == CardType.ROOM) {
				combo.addItem(board.getAllCards().get(i).getName());
			} 
		}
		return combo;
	}
	
	//Creating File > Exit Menu for the GUI
	private JMenu createFileMenu(){
		JMenu menu = new JMenu("File");
		menu.add(createFileExitItem());
		return menu;
	}
	
	private JMenuItem createFileExitItem(){
		JMenuItem item = new JMenuItem("NeverMind, I'll make a suggestion another time");
		
		class MenuItemListener implements ActionListener {
			public void actionPerformed(ActionEvent e) {
				setVisible(false);							//hides the detective notes
			}
		}
		item.addActionListener(new MenuItemListener());
		return item;
	}
	private class CancelListener implements ActionListener {
		public void actionPerformed(ActionEvent e) {
			setVisible(false);
		}
	}
	private class SubmitListener implements ActionListener {
		public void actionPerformed(ActionEvent e) {
			weapon = weaponBox.getSelectedItem().toString();
			person = personBox.getSelectedItem().toString();
			if (!(weapon.equals("Pick a weapon")) && !(person.equals("Pick a person"))) {
				ArrayList<String> a = new ArrayList<String>();
				a.add(weapon);
				a.add(person);
				a.add(room);
				board.setSuggestions(a);
				String shown = board.disproveSuggestion(board.getSuggestions(), board.getCurrentPlayer());
				board.setCardShown(shown);
				WestPanel.resetLastGuess();
				WestPanel.resetLastDisprovement();
				board.setSubmissionComplete(true);
				repaint();
			} else {
				JOptionPane.showMessageDialog(null, "Incomplete suggestion! Please try again", "There was a problem!", JOptionPane.INFORMATION_MESSAGE);
				board.setSubmissionComplete(false);
			}
			checkComplete();
		}
	}

	private void checkComplete() {
		if (board.getSubmissionComplete()) {
			setVisible(false);
		} 
	}
	
}