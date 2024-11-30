
import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.sql.*;

class ChatServer extends Thread {
    private ServerSocket serverSocket;
    private ArrayList<PrintWriter> clientWriters;
    private int port;

    public ChatServer(int port) {
        this.port = port;
        clientWriters = new ArrayList<>();
        initializeServerSocket();
    }

    private void initializeServerSocket() {
        int maxTries = 5; 
        for (int i = 0; i < maxTries; i++) {
            try {
                serverSocket = new ServerSocket(port);
                System.out.println("Server started on port: " + port);
                return; 
            } catch (IOException e) {
                System.out.println("Port " + port + " is in use, trying the next one...");
                port++; 
            }
        }
        System.err.println("Failed to bind server to any port after " + maxTries + " attempts.");
        System.exit(1);
    }

    public void run() {
        try {
            while (true) {
                Socket clientSocket = serverSocket.accept();
                PrintWriter writer = new PrintWriter(clientSocket.getOutputStream(), true);
                clientWriters.add(writer);

                
                new ClientHandler(clientSocket, writer).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    
    private void broadcast(String message) {
        for (PrintWriter writer : clientWriters) {
            writer.println(message);
        }
    }

    private class ClientHandler extends Thread {
        private Socket socket;
        private PrintWriter clientWriter;
        private BufferedReader reader;

        public ClientHandler(Socket socket, PrintWriter writer) {
            this.socket = socket;
            this.clientWriter = writer;
            try {
                reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        public void run() {
            String message;
            try {
                while ((message = reader.readLine()) != null) {
                    broadcast(message);
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                clientWriters.remove(clientWriter);
                try {
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}


class DatabaseManager{
     
    private static final String URL = "<url>";
    private static final String USER = "<user>";
    private static final String PASS = "<pass>";

    public static Connection getConnection() throws SQLException {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            return DriverManager.getConnection(URL, USER, PASS);
        } catch (ClassNotFoundException e) {
            throw new SQLException("MySQL JDBC Driver not found.", e);
        }
    }

    public static boolean validateLogin(String username, String password) {
        String query = "SELECT password FROM users WHERE username = ?";
        
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            
            pstmt.setString(1, username);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    String storedPassword = rs.getString("password");
                    return storedPassword.equals(password);
                }
                return false;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static boolean validateCreate(String username, String password){
        String query = "SELECT password FROM users WHERE username = ?";
        String query2="INSERT INTO users (username, password) VALUES (?, ?)";
        try (Connection conn = getConnection();
        PreparedStatement pstmt = conn.prepareStatement(query);
        PreparedStatement pstmt2 = conn.prepareStatement(query2)) {
       
        pstmt.setString(1, username);
       
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
               
               JOptionPane.showMessageDialog(null,
                "Invalid username or password",
                "Login Failed",
                JOptionPane.ERROR_MESSAGE);
               return true;
                }
                pstmt2.setString(1, username);
                pstmt2.setString(2, password);
                pstmt2.executeUpdate();
                return true;
            }
   } catch (SQLException e) {
       e.printStackTrace();
       return false;
   }
    }
}

class ChatWindow extends JFrame {
    private JTextArea chatArea;
    private JTextField messageField;
    private PrintWriter writer;
    private String username;
    private SimpleDateFormat timeFormat;
    private Color themeColor;
    private static final String DB_URL = "<url>";
    private static final String DB_USER = "<user>";
    private static final String DB_PASSWORD = "<password>";

    public ChatWindow(String username, Point location, Color themeColor) {
        this.username = username;
        this.themeColor = themeColor;
        this.timeFormat = new SimpleDateFormat("HH:mm:ss");

        setTitle(username + "'s Chat");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(400, 600);
        setLocation(location);
        setLayout(new BorderLayout());
        chatArea = new JTextArea();
        chatArea.setEditable(false);
        chatArea.setLineWrap(true);
        chatArea.setWrapStyleWord(true);
        JScrollPane scrollPane = new JScrollPane(chatArea);
        add(scrollPane, BorderLayout.CENTER);
        JPanel headerPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        headerPanel.setBackground(themeColor);
        JLabel userLabel = new JLabel("Logged in as: " + username);
        headerPanel.setSize(400, 200);
        userLabel.setForeground(Color.WHITE);
        userLabel.setFont(new Font("Arial", Font.BOLD, 14));
        headerPanel.add(userLabel);
        add(headerPanel, BorderLayout.NORTH);
        JPanel messagePanel = new JPanel(new BorderLayout(5, 5));
        messagePanel.setBackground(themeColor);
        messagePanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        messageField = new JTextField();
        JButton sendButton = new JButton("Send");
        sendButton.addActionListener(e -> sendMessage());

        messagePanel.add(messageField, BorderLayout.CENTER);
        messagePanel.add(sendButton, BorderLayout.EAST);
        add(messagePanel, BorderLayout.SOUTH);
        loadChatHistory();
        connectToServer();
    }

    private void loadChatHistory() {
        String query = "SELECT time, name, content FROM history ORDER BY time ASC";

        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
             PreparedStatement stmt = conn.prepareStatement(query);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                String time = rs.getString("time");
                String name = rs.getString("name");
                String content = rs.getString("content");
                displayMessage(time, name, content);
            }
        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, 
                "Error loading chat history: " + e.getMessage(),
                "Database Error", 
                JOptionPane.ERROR_MESSAGE);
        }
    }

    private void displayMessage(String time, String name, String content) {
        chatArea.append("[" + time + "] " + name + ": " + content + "\n");
        chatArea.setCaretPosition(chatArea.getDocument().getLength());
    }

    private void connectToServer() {
        try {
            Socket socket = new Socket("localhost", 5000);
            writer = new PrintWriter(socket.getOutputStream(), true);

            new Thread(() -> {
                try {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                    String message;
                    while ((message = reader.readLine()) != null) {
                        String finalMessage = message;
                        SwingUtilities.invokeLater(() -> {
                            chatArea.append(finalMessage + "\n");
                            chatArea.setCaretPosition(chatArea.getDocument().getLength());
                        });
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }).start();

        } catch (IOException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, 
                "Could not connect to server!", 
                "Connection Error", 
                JOptionPane.ERROR_MESSAGE);
        }
    }

    private void sendMessage() {
        String message = messageField.getText().trim();
        if (!message.isEmpty() && writer != null) {
            String time = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
            writer.println("[" + time + "] " + username + ": " + message);
            messageField.setText("");

            saveMessageToDatabase(time, username, message);
        } else {
            JOptionPane.showMessageDialog(this, 
                "Not connected to the server.", 
                "Connection Error", 
                JOptionPane.ERROR_MESSAGE);
        }
    }

    private void saveMessageToDatabase(String time, String username, String message) {
        String query = "INSERT INTO history (time, name, content) VALUES (?, ?, ?)";

        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, time);
            stmt.setString(2, username);
            stmt.setString(3, message);
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, 
                "Error saving message to database: " + e.getMessage(),
                "Database Error", 
                JOptionPane.ERROR_MESSAGE);
        }
    }
}

class CreateAccount extends JFrame{
    private JTextField usernameField;
    private JPasswordField passwordField;
    private ChatWindow chatWindow;
    private Color themeColor;

    public CreateAccount(String title, Point location, Color themeColor) {
        this.themeColor = themeColor;
        
        setTitle("Create -" + title);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(400, 600);
        setLocation(location);
        setLayout(new BorderLayout());

        // Create main panel
        JPanel mainPanel = new JPanel(new GridBagLayout());
        mainPanel.setBackground(themeColor);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);

        // Username field
        gbc.gridx = 0; gbc.gridy = 0;
        mainPanel.add(new JLabel("Username:"), gbc);
        gbc.gridx = 1;
        usernameField = new JTextField(15);
        mainPanel.add(usernameField, gbc);

        // Password field
        gbc.gridx = 0; gbc.gridy = 1;
        mainPanel.add(new JLabel("Password:"), gbc);
        gbc.gridx = 1;
        passwordField = new JPasswordField(15);
        mainPanel.add(passwordField, gbc);

        //create button
        gbc.gridx = 1; gbc.gridy = 2;
        JButton createButton = new JButton("Create");
        createButton.addActionListener(e -> create());
        mainPanel.add(createButton, gbc);

        add(mainPanel, BorderLayout.CENTER);

        }
        private void create(){
            String username = usernameField.getText();
            String password = new String(passwordField.getPassword());

            if (DatabaseManager.validateCreate(username, password)) {
                chatWindow = new ChatWindow(username, getLocation(), themeColor);
                chatWindow.setVisible(true);
                dispose();
            } else {
                
                JOptionPane.showMessageDialog(this,
                "Invalid username or password",
                "Creation Failed",
                JOptionPane.ERROR_MESSAGE);
                passwordField.setText("");
                
        }
        }
}

class LoginWindow extends JFrame {
    private JTextField usernameField;
    private JPasswordField passwordField;
    private ChatWindow chatWindow;
    private Color themeColor;
    private CreateAccount createNewAccount;
    private String title;

    public LoginWindow(String title, Point location, Color themeColor) {
        this.themeColor = themeColor;
        this.title=title;
        setTitle("Login - " + title);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(400, 600);
        setLocation(location);
        setLayout(new BorderLayout());

        // Create main panel
        JPanel mainPanel = new JPanel(new GridBagLayout());
        mainPanel.setBackground(themeColor);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);

        // Username field
        gbc.gridx = 0; gbc.gridy = 0;
        mainPanel.add(new JLabel("Username:"), gbc);
        gbc.gridx = 1;
        usernameField = new JTextField(15);
        mainPanel.add(usernameField, gbc);

        // Password field
        gbc.gridx = 0; gbc.gridy = 1;
        mainPanel.add(new JLabel("Password:"), gbc);
        gbc.gridx = 1;
        passwordField = new JPasswordField(15);
        mainPanel.add(passwordField, gbc);

        // Login button
        gbc.gridx = 1; gbc.gridy = 2;
        JButton loginButton = new JButton("Login");
        loginButton.addActionListener(e -> login());
        mainPanel.add(loginButton, gbc);
       
        // Create button
        gbc.gridx = 0; gbc.gridy = 2;
        JButton createButton = new JButton("Create");
        createButton.addActionListener(e -> create());
        mainPanel.add(createButton, gbc);

        add(mainPanel, BorderLayout.CENTER);
    }

    private void login() {
        String username = usernameField.getText();
        String password = new String(passwordField.getPassword());

        if (DatabaseManager.validateLogin(username, password)) {
            chatWindow = new ChatWindow(username, getLocation(), themeColor);
            chatWindow.setVisible(true);
            dispose();
        } else {
            JOptionPane.showMessageDialog(this,
                "Invalid username or password",
                "Login Failed",
                JOptionPane.ERROR_MESSAGE);
            passwordField.setText("");
        }
    }

    private void create(){
        createNewAccount= new CreateAccount(title, getLocation(),themeColor);
        createNewAccount.setVisible(true);
        dispose();
    }
}

public class ChatApplicationBeginsHere { 
    public static void main(String[] args) {
       
       ChatServer server = new ChatServer(5000);
        server.start();
        
        SwingUtilities.invokeLater(() -> {
         
            new LoginWindow("User 1", new Point(50, 50), 
                new Color(224, 122, 95)).setVisible(true);
            
            new LoginWindow("User 2", new Point(400, 50), 
                new Color(227, 181, 5)).setVisible(true);
            
            new LoginWindow("User 3", new Point(750, 50), 
                new Color(129, 178, 154)).setVisible(true);
        });
    }
}
