import java.io.*;
import java.net.*;
import java.util.Scanner;
import java.util.Set;
import java.util.HashSet;

public class ChatWorld {
    
    // Main method to start either server or client
    public static void main(String[] args) throws IOException {
        try (Scanner scanner = new Scanner(System.in)) {
            System.out.println("Welcome to ChatWorld!");
            System.out.println("Type 'server' to start the server or 'client' to connect as a client:");

            String mode = scanner.nextLine();
            
            if (mode.equalsIgnoreCase("server")) {
                startServer();
            } else if (mode.equalsIgnoreCase("client")) {
                startClient();
            } else {
                System.out.println("Invalid input. Please restart the program.");
            }
        }
    }

    // Start the chat server
    public static void startServer() throws IOException {
        try (ServerSocket serverSocket = new ServerSocket(12345)) {
            Set<Socket> clientSockets = new HashSet<>();
            Set<String> clientNames = new HashSet<>();
            System.out.println("Chat Server is running...");

            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("New client connected");

                // Create and start a new thread for each client
                ClientHandler clientHandler = new ClientHandler(clientSocket, clientSockets, clientNames);
                clientSockets.add(clientSocket);
                new Thread(clientHandler).start();
            }
        }
    }

    // Start the chat client
    public static void startClient() {
        try {
            Socket socket = new Socket("localhost", 12345);
            BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            PrintWriter writer = new PrintWriter(socket.getOutputStream(), true);
            try (Scanner scanner = new Scanner(System.in)) {
                // Start a thread to read incoming messages from the server
                new Thread(new IncomingReader(reader)).start();

                // Ask user for their name
                System.out.print("Enter your name: ");
                String userName = scanner.nextLine();
                writer.println(userName);

                // Send messages to the server
                String message;
                while (true) {
                    message = scanner.nextLine();
                    if (message.equalsIgnoreCase("exit")) {
                        writer.println("exit");
                        break;
                    }
                    writer.println(message);
                }
            }

            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Client Handler class to manage each connected client on the server
    static class ClientHandler implements Runnable {
        private Socket clientSocket;
        private Set<Socket> clientSockets;
        private Set<String> clientNames;
        private BufferedReader reader;
        private PrintWriter writer;
        private String clientName;

        public ClientHandler(Socket socket, Set<Socket> clientSockets, Set<String> clientNames) {
            this.clientSocket = socket;
            this.clientSockets = clientSockets;
            this.clientNames = clientNames;
            try {
                this.reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                this.writer = new PrintWriter(clientSocket.getOutputStream(), true);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void run() {
            try {
                // Ask for client name
                writer.println("Enter your name:");
                clientName = reader.readLine();
                synchronized (clientNames) {
                    if (clientName != null && !clientNames.contains(clientName)) {
                        clientNames.add(clientName);
                        broadcastMessage(clientName + " has joined the chat!");
                    } else {
                        writer.println("Name already taken. Please reconnect with a different name.");
                        clientSocket.close();
                        return;
                    }
                }

                // Handle messages from the client
                String message;
                while ((message = reader.readLine()) != null) {
                    if (message.equalsIgnoreCase("exit")) {
                        synchronized (clientNames) {
                            clientNames.remove(clientName);
                        }
                        broadcastMessage(clientName + " has left the chat.");
                        break;
                    }
                    broadcastMessage(clientName + ": " + message);
                }

            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    clientSocket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        // Broadcast the message to all connected clients
        private void broadcastMessage(String message) {
            System.out.println(message);  // Print message to server console
            synchronized (clientSockets) {
                for (Socket socket : clientSockets) {
                    try {
                        if (!socket.isClosed()) {
                            PrintWriter pw = new PrintWriter(socket.getOutputStream(), true);
                            pw.println(message);
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    // IncomingReader class to manage incoming messages on the client side
    static class IncomingReader implements Runnable {
        private BufferedReader reader;

        public IncomingReader(BufferedReader reader) {
            this.reader = reader;
        }

        @Override
        public void run() {
            String message;
            try {
                while ((message = reader.readLine()) != null) {
                    System.out.println(message);  // Print received message to console
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
