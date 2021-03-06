package main;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Random;
import java.util.Scanner;
import java.util.HashSet;

import javax.swing.*;
import javax.swing.text.html.HTMLDocument.Iterator;
import javax.xml.stream.events.StartDocument;

import ClueBoardGUI.ClueBoardGUI;
import ClueBoardGUI.WestPanel;

import main.Card.CardType;

/**
 * @author: Craig Carlson
 * @author: Lars Walen
 */

public class Board extends JPanel implements MouseListener {

	private ArrayList<BoardCell> cells = new ArrayList<BoardCell>();
	private Map<Character, String> rooms = new HashMap<Character, String>();
	private Map<String, Character> iRooms = new HashMap<String, Character>(); // stores
																				// the
																				// room
																				// initial
	private ArrayList<Card> allCards = new ArrayList<Card>(); // ArrayList of
																// all possible
																// cards in the
																// game...Note
																// their
																// awesomeness
	private ArrayList<ComputerPlayer> compPlayers = new ArrayList<ComputerPlayer>();// Stores
																					// a
																					// list
																					// of
																					// all
																					// computerPlayers
	private ArrayList<String> answers = new ArrayList<String>(); // stores 3
																	// strings
																	// that will
																	// be the
																	// game
																	// answer
	private ArrayList<String> accusations = new ArrayList<String>(); // stores 3
																		// strings
																		// that
																		// will
																		// be a
																		// person's
																		// accusation
	private ArrayList<String> suggestions = new ArrayList<String>(); // stores 3
																		// strings
																		// that
																		// will
																		// be a
																		// person's
																		// suggestion
	private ArrayList<Player> allPlayers = new ArrayList<Player>(); // stores
																	// all
																	// players

	private HumanPlayer self = new HumanPlayer();

	private int numRows;
	private int numColumns;
	// The following will be used to check card configurations
	private int numPlayers = 0;
	private int numRooms = 0;
	private int numWeapons = 0;
	// The following will be used to check cards dealt
	private int numDealt; // will track number of total cards dealt
	// the following will be used to track winning status
	private boolean won = false;

	// Adjacencies and targets related members
	private Map<Integer, LinkedList<Integer>> adjMatrix;
	public HashSet<Integer> targets;
	public HashSet<BoardCell> targetCells = new HashSet<BoardCell>();
	private boolean[] visited;

	// Who's Turn is it?? hmmmmm
	private Player currentPlayer; // human should start the game. default
	private int currentPlayerIndex; // index in allPlayers array

	Graphics g; // for use in drawing players and cells

	int diceRoll; // for use in gameplay
	private String cardShown = " ";

	// booleans used in turns
	private boolean playerSelTarget = true;
	private boolean playerEnteredRoom = false;
	private boolean turnComplete = true;
	private boolean isInRoom = false;
	private boolean submissionComplete = true;

	/**
	 * Creates board given filenames of legend file and board config file
	 * 
	 * @param legendFilename
	 * @param boardFilename
	 */
	public Board(String legendFilename, String boardFilename,
			String playersFilename, String cardsFilename) {
		try {
			loadConfigFiles(legendFilename, boardFilename, playersFilename,
					cardsFilename);
		} catch (BadConfigFormatException e) {
			System.out.println(e);
		}
		visited = new boolean[numRows * numColumns];
		adjMatrix = new HashMap<Integer, LinkedList<Integer>>();
		targets = new HashSet<Integer>();

		calcAdjacencies();
		currentPlayerIndex = allPlayers.size() - 1;
		currentPlayer = allPlayers.get(currentPlayerIndex);

		ArrayList<String> defaultList = new ArrayList<String>();
		defaultList.add("person");
		defaultList.add("room");
		defaultList.add("weapon");
		setSuggestions(defaultList);
		setCardShown("No current card shown");
		dealCards(); // Shuffles cards and causes loadCards to fail. Use in GUI
						// for actual gameplay
		rollDice();
		addMouseListener(this);
	}

	public void paintComponent(Graphics g) {
		int i = 0;

		// draw the board
		while (i < cells.size()) {
			for (int j = 0; j < numRows; ++j) {
				for (int k = 0; k < numColumns; ++k) {
					cells.get(i).draw(g, k, j, rooms);
					cells.get(i).setRow(j);
					cells.get(i).setCol(k);
					++i;
				}
			}
		}

		// paint the available targets if the human player
		// if (getCurrentPlayer() == self) {
		java.util.Iterator<Integer> it = targets.iterator();
		int index;
		int coord[];
		while (it.hasNext()) {
			index = it.next();
			coord = calcCoords(index);
			cells.get(index).highlight(g, coord[1], coord[0]);
			repaint();
		}

		// }

		// draw the human player
		self.draw(g);
		// draw the computer player
		for (int l = 0; l < compPlayers.size(); ++l) {
			compPlayers.get(l).draw(g);
		}

	}

	/**
	 * Calls helper functions to load data from legend and and board config
	 * files
	 */
	public void loadConfigFiles(String legendFilename, String boardFilename,
			String playersFilename, String cardsFilename)
			throws BadConfigFormatException {

		try {
			readLegend(legendFilename);
		} catch (FileNotFoundException e) {
			System.out.println("could not read legend file");
		}

		try {
			readBoard(boardFilename);
		} catch (FileNotFoundException e) {
			System.out.println("could not read board file");
		}
		try {
			readPlayers(playersFilename);
		} catch (FileNotFoundException e) {
			System.out.println("could not read players file");
		}
		try {
			readCards(cardsFilename);
		} catch (FileNotFoundException e) {
			System.out.println("could not read cards file");
		}
	}

	/**
	 * Iterates through lines in legend file, splits each line at ", ", and adds
	 * initial and name to rooms map. Also adds to inverted map, to allow
	 * getting initials from names
	 */
	public void readLegend(String legendFilename) throws FileNotFoundException,
			BadConfigFormatException {
		String legendLine;
		FileReader legendFile = new FileReader(legendFilename);
		Scanner scan = new Scanner(legendFile);

		while (scan.hasNextLine()) {
			legendLine = scan.nextLine();
			String[] line;
			line = legendLine.split(", ");
			if (line.length > 2)
				throw new BadConfigFormatException(
						"Legend file has more than two items per line");
			rooms.put(line[0].charAt(0), line[1]);
			iRooms.put(line[1], line[0].charAt(0));
		}
		scan.close();
	}

	/**
	 * Iterates through lines in board config file, splitting by "," and adding
	 * each symbol to cells as the appropriate cell type
	 */
	public void readBoard(String boardFilename) throws FileNotFoundException,
			BadConfigFormatException {
		FileReader boardFile = new FileReader(boardFilename);
		Scanner scan = new Scanner(boardFile);
		char walkwayKey = getInitial("Walkway");

		LinkedList<Integer> rowSize = new LinkedList<Integer>(); // stores
																	// number of
																	// "cells"
																	// per line
																	// for EVERY
																	// LINE
		numColumns = 0;
		numRows = 0;
		while (scan.hasNext()) {
			String line = scan.next();
			String cellInits[] = line.split(",");

			numColumns = cellInits.length;

			for (int i = 0; i < cellInits.length; i++) {
				String cellInit = cellInits[i];

				BoardCell cell;
				if (cellInit.charAt(0) == walkwayKey) {
					cell = new WalkwayCell();
				} else {
					cell = new RoomCell(cellInit);
				}
				cells.add(cell);
				// System.out.print(cellInit + "\t");
			}
			// System.out.print("\n");

			rowSize.add(numColumns);
			numRows++;
		}
		scan.close();
		for (int i = 1; i < rowSize.size(); i++) {
			if (rowSize.get(i) != rowSize.get(i - 1))
				throw new BadConfigFormatException(
						"Not all rows the same length");
		}

	}

	// Reads in Players
	public void readPlayers(String playersFilename)
			throws FileNotFoundException, BadConfigFormatException {
		String playersLine;
		FileReader playersFile = new FileReader(playersFilename);
		Scanner scan = new Scanner(playersFile);
		playersLine = scan.nextLine();
		String[] line = playersLine.split(", ");
		self.setName(line[0]);
		self.setColor(line[1]);
		self.setRow(Integer.parseInt(line[2]));
		self.setCol(Integer.parseInt(line[3]));
		self.setCurrentLocation(calcIndex(Integer.parseInt(line[2]),
				Integer.parseInt(line[3])));
		allPlayers.add(self);
		while (scan.hasNextLine()) {
			playersLine = scan.nextLine();
			String[] l;
			l = playersLine.split(", ");
			ComputerPlayer comp = new ComputerPlayer();
			comp.setName(l[0]);
			comp.setColor(l[1]);
			comp.setRow(Integer.parseInt(l[2]));
			comp.setCol(Integer.parseInt(l[3]));
			comp.setCurrentLocation(calcIndex(comp.getRow(), comp.getCol()));
			compPlayers.add(comp);
			allPlayers.add(comp);
		}
		scan.close();
	}

	// Reads in Cards
	public void readCards(String cardsFilename) throws FileNotFoundException,
			BadConfigFormatException {
		String cardsLine;
		FileReader cardsFile = new FileReader(cardsFilename);
		Scanner scan = new Scanner(cardsFile);
		while (scan.hasNextLine()) {
			cardsLine = scan.nextLine();
			String[] card;
			card = cardsLine.split(", ");
			CardType type = CardType.NONE;
			switch (card[1]) {
			case "P":
				type = CardType.PERSON;
				numPlayers++;
				break;
			case "R":
				type = CardType.ROOM;
				numRooms++;
				break;
			case "W":
				type = CardType.WEAPON;
				numWeapons++;
				break;
			}
			Card c = new Card(card[0], type);
			allCards.add(c);
		}
		scan.close();
	}

	public void dealCards() {
		boolean personSet = false;
		boolean roomSet = false;
		boolean weaponSet = false;

		// takes the list of all cards and shuffles them into a new array for
		// random distribution
		Collections.shuffle(allCards);

		ArrayList<Card> c = new ArrayList<Card>();
		ArrayList<String> a = new ArrayList<String>();

		for (int j = 0; j < allCards.size(); j++) {
			if (allCards.get(j).getCardType() == CardType.PERSON
					&& personSet == false) {
				a.add(allCards.get(j).getName());
				personSet = true;
			} else if (allCards.get(j).getCardType() == CardType.ROOM
					&& roomSet == false) {
				a.add(allCards.get(j).getName());
				roomSet = true;
			} else if (allCards.get(j).getCardType() == CardType.WEAPON
					&& weaponSet == false) {
				a.add(allCards.get(j).getName());
				weaponSet = true;
			} else {
				c.add(allCards.get(j));
			}
		}
		setAnswers(a);
		System.out.println("Answers for this game ");
		for (int m = 0; m < answers.size(); ++m) {
			System.out.println(answers.get(m));
		}

		Collections.shuffle(c);

		for (int i = 0; i < c.size();) {
			for (int j = 0; j < compPlayers.size() && i < c.size(); j++) {
				compPlayers.get(j).addCards(c.get(i));
				c.get(i).incTimesDealt();
				compPlayers.get(j).updateSeen(c.get(i).getName());
				numDealt++;
				i++;
			}
			if (i < c.size()) {
				self.addCards(c.get(i));
				// System.out.println(self.getCards());
				c.get(i).incTimesDealt();
				numDealt++;
				i++;
			}
		}
	}

	/**
	 * Calculates adjacencies list for all points on board
	 */
	public void calcAdjacencies() {

		// System.out.println("calculating adjacencies");
		for (int row = 0; row < numRows; row++) {
			for (int col = 0; col < numColumns; col++) {

				LinkedList<Integer> list = new LinkedList<Integer>();
				// System.out.println("adj list for " + calcIndex(row, col));
				// if( getCellAt(calcIndex(row, col)).isRoom() ) {
				// System.out.println(" with door dir " + getRoomCellAt(row,
				// col).getDoorDirection());
				// }

				if (getCellAt(calcIndex(row, col)).isWalkway()
						|| getCellAt(calcIndex(row, col)).isDoorway()) {

					// Up
					if (row - 1 >= 0
							&& (getCellAt(calcIndex(row - 1, col)).isDoorway() || getCellAt(
									calcIndex(row - 1, col)).isWalkway())) {
						if (getCellAt(calcIndex(row - 1, col)).isWalkway()
								|| getRoomCellAt(row - 1, col)
										.getDoorDirection() == RoomCell.DoorDirection.DOWN) {
							list.add(calcIndex(row - 1, col));
							// System.out.println("\tadding " + calcIndex(row-1,
							// col));
						}
					}
					// Down
					if (row + 1 < numRows
							&& (getCellAt(calcIndex(row + 1, col)).isDoorway() || getCellAt(
									calcIndex(row + 1, col)).isWalkway())) {
						if (getCellAt(calcIndex(row + 1, col)).isWalkway()
								|| getRoomCellAt(row + 1, col)
										.getDoorDirection() == RoomCell.DoorDirection.UP) {
							list.add(calcIndex(row + 1, col));
							// System.out.println("\tadding " + calcIndex(row+1,
							// col));
						}
					}
					// Left
					if (col - 1 >= 0
							&& (getCellAt(calcIndex(row, col - 1)).isDoorway() || getCellAt(
									calcIndex(row, col - 1)).isWalkway())) {
						if (getCellAt(calcIndex(row, col - 1)).isWalkway()
								|| getRoomCellAt(row, col - 1)
										.getDoorDirection() == RoomCell.DoorDirection.RIGHT) {
							list.add(calcIndex(row, col - 1));
							// System.out.println("\tadding " + calcIndex(row,
							// col-1));
						}
					}
					// Right
					if (col + 1 < numColumns
							&& (getCellAt(calcIndex(row, col + 1)).isDoorway() || getCellAt(
									calcIndex(row, col + 1)).isWalkway())) {
						if (getCellAt(calcIndex(row, col + 1)).isWalkway()
								|| getRoomCellAt(row, col + 1)
										.getDoorDirection() == RoomCell.DoorDirection.LEFT) {
							list.add(calcIndex(row, col + 1));
							// System.out.println("\tadding " + calcIndex(row,
							// col+1));
						}
					}
				}
				adjMatrix.put(calcIndex(row, col), list);
			}
		}
	}

	/**
	 * 
	 * @return HashSet<BoardCell>
	 */
	public HashSet<BoardCell> getTargets() {

		// maybe put this in calcTargets
		for (int cell = 0; cell < numRows * numColumns; cell++) {
			visited[cell] = false;
		}

		HashSet<BoardCell> targetCells = new HashSet<BoardCell>();
		for (int i : targets) {
			targetCells.add(getCellAt(i));
		}

		return targetCells;
	}

	public void calcTargets(int startLocation, int numberOfSteps) {
		targets.clear();
		if (getCellAt(startLocation).isDoorway()) {
			visited[startLocation] = true;
			targets = calcTargetsRecursively(getAdjList(startLocation)
					.getLast(), numberOfSteps - 1);
		} else {
			targets = calcTargetsRecursively(startLocation, numberOfSteps);
		}
		//prevents two characters sharing the same cell
		for (int q = 0; q < allPlayers.size(); ++q) {
			if (targets.contains(allPlayers.get(q).getCurrentLocation()) && !getCellAt(allPlayers.get(q).getCurrentLocation()).isRoom()) {
				targets.remove(allPlayers.get(q).getCurrentLocation());
			}
		}
	}

	public HashSet<Integer> calcTargetsRecursively(int startLocation,
			int numberOfSteps) {
		visited[startLocation] = true;
		HashSet<Integer> set = new HashSet<Integer>();
		if (numberOfSteps == 0 || getCellAt(startLocation).isDoorway()) {
			set.add(startLocation);
			visited[startLocation] = false;
			return set;
		}

		for (int i : getAdjList(startLocation)) {
			if (!visited[i]) {
				visited[i] = true;
				set.addAll(calcTargetsRecursively(i, numberOfSteps - 1));
				visited[i] = false;
			}
		}
		return set;
	}

	public String disproveSuggestion(ArrayList<String> suggestion,
			Player current) {
		Random p_rand = new Random(); // for random player
		Random c_rand = new Random(); // for random card
		int playerInt;
		int cardInt;
		ArrayList<Player> players = new ArrayList<Player>(); // all players in
																// one list
		players.add(self);
		for (int j = 0; j < compPlayers.size(); j++) {
			players.add(compPlayers.get(j));
		}
		//move the accused player to the accused room in a suggestion
		for(int q = 0; q < allPlayers.size(); q++ ) {
			if (suggestion.contains(players.get(q).getName())) {
				allPlayers.get(q).setCurrentLocation(currentPlayer.getCurrentLocation());
				allPlayers.get(q).setRow(currentPlayer.getRow());
				allPlayers.get(q).setCol(currentPlayer.getCol());
				repaint();
			}
		}
		playerInt = p_rand.nextInt(players.size());
		ArrayList<String> playerCards = new ArrayList<String>(); // copy list of the cards each player has
		int counter = players.size(); // iteration through each player only once
		ArrayList<String> matches = new ArrayList<String>(); // holds which cards would match the suggestion
	
		
		while (counter > 0) {
			if (!(current.getName().equals(players.get(playerInt).getName()))) {
				for (int i = 0; i < players.get(playerInt).getCards().size(); ++i) {
					playerCards.add(players.get(playerInt).getCards().get(i)
							.getName());
				}
				if (playerCards.contains(suggestion.get(0)))
					matches.add(suggestion.get(0));
				if (playerCards.contains(suggestion.get(1)))
					matches.add(suggestion.get(1));
				if (playerCards.contains(suggestion.get(2)))
					matches.add(suggestion.get(2));
				if (matches.size() > 0) {
					cardInt = c_rand.nextInt(matches.size());
					for (int f = 0; f < compPlayers.size(); f++) {
						compPlayers.get(f).updateSeen(matches.get(cardInt));
					}
					return matches.get(cardInt); // randomly returns match
				}
			}
			// if the random playerInt reaches end of the array, restarts at the
			// beginning
			if (playerInt < players.size() - 1) {
				playerInt++;
			} else {
				playerInt = 0;
			}
			counter--;
		}
		// return "null";
		return null;
	}

	/**
	 * Convert row and column coordinates to cell index
	 */
	public int calcIndex(int row, int col) {
		return col + (row * numColumns);
	}

	public int[] calcCoords(int index) {
		int[] coords = { index / numColumns, index % numColumns };
		return coords;
	}

	/**
	 * Get RoomCell at row and column coordinates
	 */
	public RoomCell getRoomCellAt(int row, int col) {
		return (RoomCell) cells.get(calcIndex(row, col));
	}

	// convert cell type to a string! ^.^
	public String findMapValue(char Initial) {
		for (Map.Entry<Character, String> entry : rooms.entrySet()) {
			if (entry.getKey().equals(Initial)) {
				return entry.getValue();
			}
		}
		return null;
	}

	// calculate the next move
	public void nextMove() {
		playerSelTarget = false;
		playerEnteredRoom = false;
		// sets the next player
		if (currentPlayerIndex == allPlayers.size() - 1) {
			currentPlayerIndex = 0;
			currentPlayer = allPlayers.get(0);
		} else {
			currentPlayerIndex++;
			currentPlayer = allPlayers.get(currentPlayerIndex);
		}
		rollDice();
		calcTargets(
				calcIndex(getCurrentPlayer().getRow(), getCurrentPlayer()
						.getCol()), getDiceRoll());
		// if next player is a computer
		if (currentPlayer != self) {
			ComputerPlayer comp = (ComputerPlayer) currentPlayer;
			// System.out.println(targets);
			// System.out.println(getTargets());
			comp.makeMove(getTargets());
			targets.clear();
			playerSelTarget = true;
			comp.setCurrentLocation(calcIndex(comp.getRow(), comp.getCol()));
			if (getCellAt(comp.getCurrentLocation()).isRoom()) {
				BoardCell tempCell = getCellAt(comp.getCurrentLocation());
				char initial = tempCell.getCellType();
				String roomS = findMapValue(initial);
				ArrayList<String> compSuggestions = comp.createSuggestion(roomS);
				setSuggestions(compSuggestions);
				String shown = disproveSuggestion(getSuggestions(), comp);
				setCardShown(shown);
				if (cardShown == null) {
					setAccusations(getSuggestions());
					setWon();
				}
				WestPanel.resetLastGuess();
				WestPanel.resetLastDisprovement();
			}
			repaint();
		} else {
			repaint();
		}
	}

	// roll random dice
	public void rollDice() {
		Random roll = new Random();
		int newRoll = roll.nextInt(6);
		setDiceRoll(newRoll + 1);
	}

	// Mouse Listener for the board
	public void mouseClicked(MouseEvent e) {
	}

	public void mouseEntered(MouseEvent e) {
	}

	public void mouseExited(MouseEvent e) {
	}

	public void mouseReleased(MouseEvent e) {
	}

	public void mousePressed(MouseEvent e) {
		// turnComplete = false;
		setSubmissionComplete(false);
		finishTurn(e);
	}

	// if player has not moved, they cannot continue
	public void finishTurn(MouseEvent e) {
		java.util.Iterator<BoardCell> it = getTargets().iterator();
		BoardCell twazock;
		while (it.hasNext()) {

			twazock = it.next();

			if (twazock.cellClick(e.getX(), e.getY())) {
				getCurrentPlayer().setCurrentLocation(
						calcIndex(twazock.getRow(), twazock.getCol()));
				getCurrentPlayer().setRow(twazock.getRow());
				getCurrentPlayer().setCol(twazock.getCol());
				playerSelTarget = true;
				targets.clear();
				BoardCell cell = getCellAt(self.getCurrentLocation());
				if (cell.isRoom()) {
					int row = getCurrentPlayer().getRow();
					int col = getCurrentPlayer().getCol();
					char initial = cell.getCellType();
					String roomS = findMapValue(initial);
					DetNotesGUI.makeSuggestion sugpanel = new DetNotesGUI.makeSuggestion(
							ClueBoardGUI.getBoard(), roomS);
					sugpanel.setVisible(true);
				} else {
					setSubmissionComplete(true);
				}
				repaint();
				return;
			}
		}
		// no improper target can be selected
		if (getCurrentPlayer() == getSelf() && playerSelTarget == false) {
			JOptionPane.showMessageDialog(null,
					"FATALITY, Please select another target");
		}
	}

	/*
	 * Getters
	 */
	public LinkedList<Integer> getAdjList(int index) {
		return adjMatrix.get(index);
	}

	public char getInitial(String roomName) {
		return iRooms.get(roomName);
	}

	public Map<Character, String> getRooms() {
		return rooms;
	}

	public ArrayList<BoardCell> getCells() {
		return cells;
	}

	public int getNumRows() {
		return numRows;
	}

	public int getNumColumns() {
		return numColumns;
	}

	public ArrayList<ComputerPlayer> getCompPlayers() {
		return compPlayers;
	}

	public void setCompPlayers(ArrayList<ComputerPlayer> compPlayers) {
		this.compPlayers = compPlayers;
	}

	public void addCompPlayers(ComputerPlayer compPlayer) {
		compPlayers.add(compPlayer);
	}

	public HumanPlayer getSelf() {
		return self;
	}

	public ArrayList<Card> getAllCards() {
		return allCards;
	}

	public void setAllCards(ArrayList<Card> allCards) {
		this.allCards = allCards;
	}

	public ArrayList<Player> getAllPlayers() {
		return allPlayers;
	}

	public int getNumPlayers() {
		return numPlayers;
	}

	public int getNumRooms() {
		return numRooms;
	}

	public int getNumWeapons() {
		return numWeapons;
	}

	public int getNumDealt() {
		return numDealt;
	}

	public boolean isWon() {
		return won;
	}

	public ArrayList<String> getAnswers() {
		return answers;
	}

	public void setAnswers(ArrayList<String> answers) {
		this.answers = answers;
	}

	public ArrayList<String> getAccusations() {
		return accusations;
	}

	public void setAccusations(ArrayList<String> newAccusations) {
		accusations = newAccusations;
	}

	public void setWon() {
		// determines if a winner has occured by comparing accusation to answers
		if (getAccusations().contains(getAnswers().get(0))
				&& getAccusations().contains(getAnswers().get(1))
				&& getAccusations().contains(getAnswers().get(2))) {
			won = true;
			JOptionPane.showMessageDialog(null, getCurrentPlayer().getName() + " has solved the crime!!! Congratulations!\n"	+ accusations);
			//Play, cause we really needed to at this point
			if (currentPlayer == self) {
				JOptionPane.showMessageDialog(null, "End? No, it doesn't end here. Death is just a new path, a path everyone has to take.");
			} else {
				JOptionPane.showMessageDialog(null, "There never was much hope...just a fool's hope");
			}
			
			setVisible(false);
		} else {
			won = false;
			JOptionPane.showMessageDialog(null, getCurrentPlayer().getName()
					+ " has accused incorrectly!\n" + accusations);
			setVisible(true);
		}
	}

	public Player getCurrentPlayer() {
		return currentPlayer;
	}

	public void setCurrentPlayer(Player currentPlayer) {
		this.currentPlayer = currentPlayer;
	}

	public ArrayList<String> getSuggestions() {
		return suggestions;
	}

	public void setSuggestions(ArrayList<String> suggestions) {
		this.suggestions = suggestions;
	}

	public BoardCell getCellAt(int i) {
		return cells.get(i);
	}

	public int getDiceRoll() {
		return diceRoll;
	}

	public void setDiceRoll(int diceRoll) {
		this.diceRoll = diceRoll;
	}

	public void setSubmissionComplete(boolean status) {
		submissionComplete = status;
	}

	public boolean getSubmissionComplete() {
		return submissionComplete;
	}

	public String getCardShown() {
		return cardShown;
	}

	public void setCardShown(String c) {
		cardShown = c;
	}

	public boolean isPlayerSelTarget() {
		return playerSelTarget;
	}

}
