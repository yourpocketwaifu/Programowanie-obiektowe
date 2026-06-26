package server;

import server.database.UsersDatabase;

import java.io.*;
import java.net.Socket;
import java.util.Arrays;
import java.util.Locale;

public class ClientHandler implements Runnable {
    private final Socket socket;
    private final PrintWriter writer;
    private final BufferedReader reader;
    private final Server server;
    private String login;

    public ClientHandler(Socket socket, Server server) throws IOException {
        this.socket = socket;
        this.server = server;
        InputStream input = socket.getInputStream();
        OutputStream output = socket.getOutputStream();
        reader = new BufferedReader(new InputStreamReader(input));
        writer = new PrintWriter(output, true);
    }

    public String getLogin() {
        return login;
    }

    @Override
    public void run() {
        try {
            this.send("Welcome to the Chat Server!");
            boolean authenticated = false;

            while (!authenticated) {
                this.send("\r\nChoose an option:\r\n [1] Login\r\n [2] Register\r\n [3] Exit");
                String choice = reader.readLine();
                if (choice == null) return;
                choice = choice.trim();

                switch (choice) {
                    case "1" -> authenticated = handleLoginInput();
                    case "2" -> handleRegisterInput();
                    case "3" -> {
                        this.send("Goodbye!");
                        return;
                    }
                    default -> this.send("[SERVER] Invalid option. Type 1, 2 or 3.");
                }
            }

            String clientMessage;
            while ((clientMessage = reader.readLine()) != null) {
                System.out.println("Client sent: " + clientMessage);
                if (clientMessage.startsWith("/")) {
                    if (!commandHandler(clientMessage)) {
                        break;
                    }
                }
            }

        } catch (IOException e) {
            System.out.println("Client-server connection lost");
        } finally {
            server.removeClient(this);
            System.out.println("Client removed from the list");
            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }


    private boolean handleLoginInput() throws IOException {
        this.send("--- LOGIN PANEL ---");

        String username = readValidSingleWord("Username: ");
        if (username == null) return false;

        this.send("Password: ");
        String password = reader.readLine();
        if (password == null) return false;

        this.login = username;
        if (UsersDatabase.authenticate(this.login, password.trim())) {
            this.send("Welcome back, " + this.login + "! Type /show for help.");
            server.sendMessage(String.format(Locale.ROOT, "[SERVER] User %s has logged in!", this.login), this);
            return true;
        } else {
            this.send("[ERROR] Wrong username or password!");
            return false;
        }
    }

    private void handleRegisterInput() throws IOException {
        this.send("--- REGISTRATION PANEL ---");

        String username = readValidSingleWord("Choose Username: ");
        if (username == null) return;

        this.send("Choose Password: ");
        String password = reader.readLine();
        if (password == null) return;

        password = password.trim();
        if (password.isEmpty()) {
            this.send("[ERROR] Password cannot be empty!");
            return;
        }

        int resultId = UsersDatabase.register(username, password);
        if (resultId != -1) {
            this.send("[SUCCESS] Account created successfully! You can now log in.");
        } else {
            this.send("[ERROR] Registration failed. Username might be already taken.");
        }
    }

    private String readValidSingleWord (String prompt) throws IOException {
        while (true) {
            this.send(prompt);
            String input = reader.readLine();

            if (input == null) return null;
            input = input.trim();

            if (input.isEmpty() || input.contains(" ")) {
                this.send("[SERVER] Error: Field must be a single word and cannot be empty!");
            } else {
                return input;
            }
        }
    }

    public void send (String message){
        writer.println(message);
    }

    public boolean commandHandler (String clientMessage){
        String[] tokens = clientMessage.split(" ");
        String command = tokens[0];
        switch (command) {
            case "/online" -> server.online(this);

//            case "/w" -> server.whisper(
//                    String.join(" ", Arrays.copyOfRange(tokens, 2, tokens.length)),
//                    this,
//                    tokens[1]
//            );
            case "/w" -> {
                if (tokens.length >= 3) {
                    String targetLogin = tokens[1];
                    String messageBody = String.join(" ", Arrays.copyOfRange(tokens, 2, tokens.length));
                    server.whisper(messageBody, this, targetLogin);
                } else {
                    this.send("[SERVER] Usage: /w {username} {message}");
                }
            }

//            case "/all" -> server.broadcast(
//                    String.format(Locale.ROOT, "[GLOBAL] %s: %s", this.login, String.join(" ", Arrays.copyOfRange(tokens, 1, tokens.length))),
//                    this
//            );

            case "/all" -> {
                if (tokens.length >= 2) {
                    String messageBody = String.join(" ", Arrays.copyOfRange(tokens, 1, tokens.length));
                    server.broadcast(
                            String.format(Locale.ROOT, "[GLOBAL] %s: %s", this.login, messageBody),
                            this
                    );
                } else {
                    this.send("[SERVER] Usage: /all {message}");
                }
            }

            case "/show" -> this.showHelp();

            case "/logout" -> {
                this.send("[SERVER] Goodbye! Disconnecting...");
                server.sendMessage(String.format("[SERVER] User %s has logged out.", this.login), this);
                return false;
            }

            default -> this.send("[SERVER] Unknown command.");
        }
        return true;
    }

    private void showHelp () {
        this.send("Here is the list of available commands:\r\n" +
                " -> /online       - Show online users\r\n" +
                " -> /w {user} {m} - Send private message\r\n" +
                " -> /all {msg}    - Send broadcast message\r\n" +
                " -> /show         - Show this help list\r\n" +
                " -> /logout       - Exit chat\r\n");
    }

}
