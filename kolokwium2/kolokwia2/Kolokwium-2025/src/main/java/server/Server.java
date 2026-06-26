package server;

import server.database.DatabaseConnection;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import server.database.DatabaseConnection;
import server.database.UsersDatabase;
public class Server {
    private ServerSocket serverSocket;
    private List<ClientHandler> handlers = new ArrayList<ClientHandler>();

    public Server(int port) throws IOException {
        this.serverSocket = new ServerSocket(port);
    }

    public static void main(String[] args) throws IOException {
        DatabaseConnection.connect("chat.db");

        UsersDatabase usersDb = new UsersDatabase();
        usersDb.createTable();

        Server server = new Server(3000);
        server.listen();
    }

    public void listen() throws IOException {
        System.out.println("Server on. Waiting for connection... :)");
        while (true) {
            Socket clientSocket = serverSocket.accept();
            System.out.println("User connected.");
            ClientHandler handler = new ClientHandler(clientSocket, this);
            Thread thread = new Thread(handler);
            thread.start();
            handlers.add(handler);
        }
    }

    public void sendMessage(String message, ClientHandler sender) {
        for (ClientHandler handler : handlers) {
            if (handler != sender) {
                handler.send(message);
            }
        }
    }

    public void online(ClientHandler requester) {
        StringBuilder sb = new StringBuilder();
        sb.append("--- ONLINE USERS ---\r\n");
        for (ClientHandler handler : handlers) {
            if (handler.getLogin() != null) {
                sb.append("  -> ").append(handler.getLogin()).append("\r\n");
            }
        }
        sb.append("--------------------");
        requester.send(sb.toString());
    }

    public void whisper(String message, ClientHandler sender, String receiver) {
        for (ClientHandler handler : handlers) {
            if (handler.getLogin() != null && handler.getLogin().equalsIgnoreCase(receiver)) {
                handler.send(String.format("[PRIVATE CHAT] %s: %s", sender.getLogin(), message));
                sender.send(String.format("[PRIVATE CHAT]: %s", message));
                return;
            }
        }
        sender.send(String.format("[SERVER] User %s is offline.", receiver));
    }

    public void broadcast(String message, ClientHandler sender) {
        for (ClientHandler handler : handlers) {
            if (handler != sender) {
                handler.send(message);
            }
        }
    }
//    public void sendToSpecificClient(String targetLogin, String message) {
//        for (ClientHandler handler : handlers) {
//            if (handler.getLogin() != null && handler.getLogin().equals(targetLogin)) {
//                handler.send(message);
//                return;
//            }
//        }
//    }

    public void removeClient(ClientHandler handler) {
        handlers.remove(handler);
    }
}
