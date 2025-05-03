package org.example;

import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.HashMap;

public class Server extends Thread {

    private int port=1234;
    private Socket socket=new Socket();
    public static HashMap<Integer , String> Clients= new HashMap<>();
    public static HashMap<Integer , Socket> clientConnections= new HashMap<>();
    public static HashMap<Integer, ArrayList<Integer> >Followers= new HashMap<>();
    public static HashMap<Integer, ArrayList<Integer> >Following= new HashMap<>();
    public static HashMap<Integer, ArrayList<Notifications> > Notification= new HashMap<>();
    public static HashMap<String, ArrayList<Integer>> waitingClients = new HashMap<>();
    public static HashMap<String, Integer> fileOccupied = new HashMap<>();

    public static void main(String args[]) {
        loadSocialGraphFromFile("src/main/java/org/example/SocialGraph.txt");

        Server server=new Server();
        server.start();
    }
    ServerSocket serverSocket;

    public static void loadSocialGraphFromFile(String filePath) {
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) continue;

                String[] parts = line.split(":");
                if (parts.length != 2) continue;

                int clientId = Integer.parseInt(parts[0].trim());
                String[] followingStrings = parts[1].trim().split("\\s+");

                ArrayList<Integer> followingList = new ArrayList<>();

                for (String followingStr : followingStrings) {
                    if (!followingStr.isEmpty()) {
                        int followingId = Integer.parseInt(followingStr);
                        followingList.add(followingId);

                        // Build reverse mapping: who follows this user
                        Followers.computeIfAbsent(followingId, k -> new ArrayList<>()).add(clientId);
                    }
                }

                // Store who this client is following
                Following.put(clientId, followingList);

                // Ensure this client is present in the followers map (even if no one follows them)
                Followers.putIfAbsent(clientId, Followers.getOrDefault(clientId, new ArrayList<>()));
            }

            System.out.println("Social graph loaded successfully.");
        } catch (IOException | NumberFormatException e) {
            System.out.println("Error reading social graph file: " + e.getMessage());
        }
    }

    @Override
    public void run() {
        try {
            serverSocket = new ServerSocket(port);
            System.out.println("Server Is Open");
            while (true) {
                socket=serverSocket.accept();
                new  ClientHandler(socket).start();

            }
        } catch (IOException ioException) {
            ioException.printStackTrace();
        }
    }

}
