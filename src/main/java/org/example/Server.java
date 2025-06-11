package org.example;

import org.example.Client;
import org.example.ClientProfile;
import org.example.Notifications;
import org.example.Post;

import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.HashMap;

public class Server extends Thread {

    private int port=1234;
    private Socket socket=new Socket();
    public static ArrayList<ClientProfile> TestClients=new ArrayList<>();
    public static HashMap<Integer , Socket> clientConnections= new HashMap<>();
    public static HashMap<Integer, ObjectOutputStream> notificationStreams = new HashMap<>();
    public static HashMap<Integer, ArrayList<Notifications> > Notification= new HashMap<>();
    public static HashMap<String, ArrayList<Integer>> waitingClients = new HashMap<>();
    public static HashMap<String, Integer> fileOccupied = new HashMap<>();

    public static HashMap<Integer, ArrayList<Post>> Posts = new HashMap<>();

    public static void main(String args[]) {
        loadSocialGraphFromFile("src/main/java/org/example/SocialGraph.txt");

        Server server=new Server();
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("-------------------------------");
            System.out.println("🛑 Server is shutting down. Goodbye!");
            for(ClientProfile clientProfile:TestClients){
                if(!clientProfile.getPosts().isEmpty()){
                    System.out.println("-------------------------------");
                    System.out.println("For client "+ clientProfile.getId() +" the statistic were:");
                    for(Post post: clientProfile.getPosts()){
                        System.out.println("Post with name: "+post.getImageName());
                        System.out.println("Had "+post.getNumberOfRequests()+" requests");
                        System.out.println("From them only "+post.getUserWithAccess()+" got accept");
                        System.out.println("It was downloaded by those user:"+post.getListOfClients());
                    }
                }
            }
            System.out.println("-------------------------------");
            System.out.println("if no statistics were showed for the other users that means they did not have a post");
        }));

        server.start();

        new Thread(() -> {
            try (ServerSocket notificationServer = new ServerSocket(12345)) {
                System.out.println("🔔 Notification server listening on port 12345...");
                while (true) {
                    Socket notifSocket = notificationServer.accept();
                    new Thread(() -> handleNotificationClient(notifSocket)).start();
                }
            } catch (IOException e) {
                System.err.println("Notification server error: " + e.getMessage());
            }
        }).start();

    }
    ServerSocket serverSocket;

    public static void loadSocialGraphFromFile(String filePath) {
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            HashMap<Integer, ArrayList<Integer>> tempFollowersMap = new HashMap<>();
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
                        tempFollowersMap
                                .computeIfAbsent(followingId, k -> new ArrayList<>())
                                .add(clientId);
                    }
                }

                tempFollowersMap.putIfAbsent(clientId, new ArrayList<>());

                ClientProfile clientProfile=new ClientProfile(clientId,"a"+clientId,"a"+clientId,"a"+clientId);

                clientProfile.setFollowers(followingList);
                clientProfile.setFollowing(tempFollowersMap.get(clientId));

                ArrayList<Post> PostList= new ArrayList<>();
                Server.Posts.put(clientId,PostList);
                PostList=null;

                Server.TestClients.add(clientProfile);
                CreateFilesAndFolders(clientId);
            }
            System.out.println("Social graph loaded successfully.");
        } catch (IOException | NumberFormatException e) {
            System.out.println("Error reading social graph file: " + e.getMessage());
        }
    }
    public static void CreateFilesAndFolders(int clientId) throws IOException {
        String folderName ="src/main/java/org/example/ServerFolder/Client" + clientId + "Images";
        String fileName = "src/main/java/org/example/ServerFolder/Profile_XClient" + clientId + ".txt";

        File file = new File(fileName);
        if (!file.exists()) {
            file.createNewFile();
        }
        File folder = new File(folderName);
        if (!folder.exists()) {
            folder.mkdirs();
        }

    }
    private static void handleNotificationClient(Socket socket) {
        try {
            ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
            ObjectInputStream in = new ObjectInputStream(socket.getInputStream());

            int clientId = (int) in.readObject();  // Client sends its ID to register

            synchronized (notificationStreams) {
                notificationStreams.put(clientId, out);
            }

            System.out.println("🔔 Client " + clientId + " connected to notification server.");

        } catch (Exception e) {
            System.err.println("Notification handler error: " + e.getMessage());
        }
    }
    @Override
    public void run() {
        try {
            serverSocket = new ServerSocket(port);
            System.out.println("Server Is Open");
            while (true) {
                socket=serverSocket.accept();
                new ClientHandler(socket).start();

            }
        } catch (IOException ioException) {
            ioException.printStackTrace();
        }
    }


}
