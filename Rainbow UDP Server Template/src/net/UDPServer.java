package net;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JTextField;

public class UDPServer {

	// Global variables
	private DatagramSocket serverSocket;
	private int currentPort;
	
	private final int defaultPort = 10127;
	
	private boolean active = true;
	
	private byte[] lastReceivedData = new byte[1024];
	
	private String log = "";
	
	// GUI elements and settings
	private JFrame frame;
	private JPanel panel;
	
	private GridBagLayout layout;
	
	private final Color defaultBackgroundColor = new Color(8, 8, 8);
	private final Color defaultForegroundColor = new Color(64, 255, 16);
	
	private final Dimension defaultPreferredSize = new Dimension(800, 600);
	
	private JTextArea consoleOutput;
	private JTextField consoleInput;
	
	private int logIndex;
	
	private String[] commands = new String[] {"clear", "reset", "port"};
	private Color commandColor = defaultForegroundColor;
	private String[] warningCommands = new String[] {"shutdown", "exit", "halt"};
	private Color warningColor = new Color(215, 201, 32);
	
	private Font consoleFont = new Font("Consolas", Font.PLAIN, 12);
	
	public UDPServer() throws IOException {
		
		log("Starting server...", true);
		
		currentPort = defaultPort;
		
		try {
			initializeSocket(defaultPort);
		} catch (SocketException e) {
			e.printStackTrace();
		}
		
		log("Loading user interface...", true);
		
		initializeUIElements(defaultBackgroundColor, defaultForegroundColor, defaultPreferredSize);
		
		updateConsoleElements();
		
		while (active) {			
			// Initialize packet
			lastReceivedData = new byte[1024];
			DatagramPacket packet = new DatagramPacket(lastReceivedData, lastReceivedData.length);
			
			// Receive data
			try {
				serverSocket.receive(packet);
			} catch (SocketException e) {
				log("[WARNING] Canceled receiving data", true);
				continue;
			}

			// Convert data into String
			String receivedData = new String(packet.getData());
			
			// Process data
			String processedData = processData(receivedData);
			byte[] pendingData = processedData.getBytes();
			
			// Get client IP address and port
			InetAddress IPAddress = packet.getAddress();
			int port = packet.getPort();
			
			// Console output
			log("Received \"" + receivedData.trim() + "\" from client " + IPAddress.toString().substring(1) + ":" + port, true);
			log("Sending \"" + processedData.trim() + "\" to client " + IPAddress.toString().substring(1) + ":" + port, true);
			
			// Initialize packet to send to client
			DatagramPacket pendingPacket = new DatagramPacket(pendingData, pendingData.length, IPAddress, port);
			
			// Send the packet
			try {
				serverSocket.send(pendingPacket);
			} catch (IOException e) {
				e.printStackTrace();
			}
			
			updateConsoleElements();
		}
	}
	
	public static void main(String args[]) throws IOException {
		new UDPServer();
	}	
	
	private void initializeSocket(int port) throws SocketException {
		// Close the existing socket so we can use the port again later
		if (!(serverSocket == null)) serverSocket.close();
		
		serverSocket = new DatagramSocket(port);
		
		if (!(port == currentPort)) {
			currentPort = port;
			log("Changed port to " + port, true);
		}
		else log("Initialized socket on port " + port, true);
	}
	
	public String processData(String data) { 
		return data.toUpperCase();
	}
	
	public String processCommand(String input) {
		boolean successful = false;
		String response = "No command issued; invalid input: " + input;
		
		// Process commands with no arguments
		if (parseCommand(input, "shutdown") || parseCommand(input, "exit") || parseCommand(input, "halt")) {
			if (JOptionPane.showConfirmDialog(frame, "Are you sure you would like to exit?") == JOptionPane.YES_OPTION) System.exit(0);
		} else if (parseCommand(input, "clear")) {
			logIndex = log.length();
			updateConsoleElements();
			response = "Cleared console display";
			successful = true;
		} else if (parseCommand(input, "reset")) {
			logIndex = 0;
			updateConsoleElements();
			response = "Restored console display";
			successful = true;
		} else if (parseCommandStrict(input, "port")) {
			log("Current port: " + currentPort, true);
			response = "Current port: " + currentPort;
			successful = true;
		}
		
		// Process commands with arguments
		if (parseCommand(input, "port") && !parseCommandStrict(input, "port")) {
			String args = input.substring("port ".length());
			int newPort = 0;
			try {
				newPort = Integer.valueOf(args);
			} catch (NumberFormatException e) {
				
			}
			if (newPort > 0 && newPort < 65535) {
				try {
					initializeSocket(newPort);
					response = "Changed port to " + newPort;
					successful = true;
				} catch (SocketException e) {
					log(" [ERROR] Port " + newPort + " already in use", true);
					response = "[ERROR] Failed to change port to " + newPort + ": already in use";
					successful = true;
				}
			}
		}
		
		// Send to console if no command detected
		if (!successful) log("[SERVER] " + input, true);
		
		consoleInput.setText("");
		
		return response;
	}
	
	public boolean parseCommand(String input, String target) {
		return ((input.startsWith(target) && input.length() == target.length()) || (input.startsWith(target) && input.charAt(target.length()) == " ".charAt(0)));
	}
	
	public boolean parseCommandStrict(String input, String target) {
		return ((input.startsWith(target) && input.length() == target.length()));
	}
	
	public String getTimestamp() {
		return "[" + System.currentTimeMillis() + "]";
	}
	
	public void log(String str) {
		log = log + getTimestamp() + " " + str;
	}
	
	public void log(String str, boolean includeNewline) {
		if (includeNewline) log(str + "\n");
		else log(str);
	}
	
	public void updateConsoleElements() {		
		consoleOutput.setText(log.substring(logIndex));
		
		String input = consoleInput.getText();
		consoleInput.setForeground(new Color(231, 231, 231));
		for (String str : commands) {
			if (parseCommand(input, str)) consoleInput.setForeground(commandColor);
		}
		for (String str : warningCommands) {
			if (parseCommand(input, str)) consoleInput.setForeground(warningColor);
		}
	}
	
	public void initializeUIElements(Color backgroundColor, Color foregroundColor, Dimension preferredSize) {
		// Parent elements
		frame = new JFrame("Rainbow UDP Server Terminal");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		
		panel = new JPanel();
		panel.setBackground(backgroundColor);
		panel.setPreferredSize(preferredSize);
		panel.setMinimumSize(new Dimension(256, 256));
		
		layout = new GridBagLayout();
		panel.setLayout(layout);
		
		// Sub-elements
		consoleOutput = new JTextArea();
		consoleOutput.setFont(consoleFont);
		consoleOutput.setEditable(false);
		consoleOutput.setText("Default Text");
		consoleOutput.setBackground(backgroundColor);
		consoleOutput.setForeground(foregroundColor);
		
		GridBagConstraints c = generateConstraints(GridBagConstraints.BOTH, 0, 0);
		c.weightx = 1;
		c.weighty = 0.75;
		c.anchor = GridBagConstraints.PAGE_START;
		panel.add(consoleOutput, c);

		consoleInput = new JTextField();
		consoleInput.setFont(consoleFont);
		consoleInput.setEditable(true);
		consoleInput.setBackground(backgroundColor);
		consoleInput.setForeground(new Color(231, 231, 231));
		consoleInput.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				System.out.println(processCommand(consoleInput.getText()));
			}});
		
		consoleInput.addKeyListener(new KeyListener() {
			public void keyTyped(KeyEvent e) {updateConsoleElements();}

			public void keyPressed(KeyEvent e) {updateConsoleElements();}

			public void keyReleased(KeyEvent e) {updateConsoleElements();}
		});
		
		c = generateConstraints(GridBagConstraints.HORIZONTAL, 0, 1);
		c.weightx = 0;
		c.weighty = 0;
		c.anchor = GridBagConstraints.PAGE_END;
		panel.add(consoleInput, c);
		
		// Activate UI
		panel.setVisible(true);
		frame.add(panel);
		frame.pack();
		frame.setVisible(true);
	}
	
	public GridBagConstraints generateConstraints(int gridX, int gridY) {
		GridBagConstraints constraints = new GridBagConstraints();

		constraints.gridx = gridX;
		constraints.gridy = gridY;
		
		return constraints;
	}
	
	public GridBagConstraints generateConstraints(int fill, int gridX, int gridY) {
		GridBagConstraints constraints = generateConstraints(gridX, gridY);
		
		constraints.fill = fill;
		
		return constraints;
	}
	
	public GridBagConstraints generateConstraints(int fill, int gridX, int gridY, int gridWidth, int gridHeight) {
		GridBagConstraints constraints = generateConstraints(fill, gridX, gridY);
		
		constraints.gridwidth = gridWidth;
		constraints.gridheight = gridHeight;
		
		return constraints;
	}
}
