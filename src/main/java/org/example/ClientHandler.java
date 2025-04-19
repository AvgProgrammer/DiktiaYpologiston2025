package org.example;

import java.io.*;
import java.net.*;
import java.util.*;
import java.nio.file.*;

public class ClientHandler extends Thread {
    private final Socket socket;
    private ObjectInputStream in;
    private ObjectOutputStream out;

    public ClientHandler(Socket socket) {
        this.socket = socket;
    }

    public void run() {
        try {
            out = new ObjectOutputStream(socket.getOutputStream());
            in = new ObjectInputStream(socket.getInputStream());

            while (!socket.isClosed()) {
                int num;
                try {
                    num = in.readInt();
                } catch (EOFException e) {
                    break; // client disconnected
                }

                switch (num) {
                    case 0 -> handleSignIn();
                    case 1 -> handleLogin();
                    case 11 -> {
                        DataInputStream dis = new DataInputStream(socket.getInputStream());
                        int clientId = dis.readInt();
                        String fileName = dis.readUTF();
                        long fileSize = dis.readLong();
                        String saveDir = "src/main/java/org/example/ServerFolder/Client" + clientId + "Images/";
                        String fullImagePath = saveDir + fileName;
                        FileOutputStream fos = new FileOutputStream(fullImagePath);
                        byte[] buffer = new byte[4096];
                        int bytesRead;
                        long totalRead = 0;
                        while (totalRead < fileSize && (bytesRead = dis.read(buffer, 0, (int)Math.min(buffer.length, fileSize - totalRead))) != -1) {
                            fos.write(buffer, 0, bytesRead);
                            totalRead += bytesRead;
                        }
                        fos.close();
                        String profilePath = "src/main/java/org/example/ServerFolder/Profile_XClient" + clientId + ".txt";
                        updateFile(profilePath,clientId);
                        System.out.println("Image received and saved to: " + fullImagePath);
                        DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
                        dos.writeInt(1);
                        dos.flush();
                        for (int follower : Server.Followers.get(clientId)){
                            String title=clientId+" post a photo with name: "+ fileName.replaceFirst("\\.png$", "");
                            Notifications not=new Notifications(title,follower,0,clientId);
                            Server.Notification.get(follower).add(not);
                        }
                    }
                    case 12 -> {
                        int clientId = in.readInt();
                        String filePath = "src/main/java/org/example/ServerFolder/Profile_XClient" + clientId + ".txt";
                        handleProfileAccess(clientId, filePath);
                    }
                    case 13 -> {
                        String photoname=(String) in.readObject();
                        ArrayList<Integer> ReturnList= Search(photoname);
                        System.out.println("Array was sent");
                        out.writeObject(ReturnList);
                        out.flush();
                    }
                    case 14 -> {
                        int social_choise=in.readInt();
                        int clientId=in.readInt();
                        int id=in.readInt();

                        if(social_choise==1){
                            Notifications notifications=new Notifications("Follow Request",clientId, 1,id);
                            Server.Notification.get(id).add(notifications);
                        }else{
                            System.out.println("Client "+clientId+" has unfollowed "+ id);
                            Server.Following.get(clientId).remove(id);
                        }
                    }
                    case 15 -> {
                        int clientId=in.readInt();
                        out.writeObject(Server.Notification.get(clientId));
                        out.flush();
                    }
                    case 16 -> {
                        int clientId=in.readInt();
                        Notifications notification1=(Notifications) in.readObject();
                        Server.Notification.get(clientId).remove(notification1);
                        int social_choice=in.readInt()+1;
                        String title="";
                        if(social_choice!=1){
                            if (social_choice==2){
                                title="Follow Request Accepted";
                                System.out.println("Client "+clientId+" has accept "+ notification1.getId());
                                Server.Followers.get(clientId).add(notification1.getId());
                            }else if(social_choice==3){
                                System.out.println("Client "+clientId+" has reject "+ notification1.getId());
                                title="Follow Request Rejected";
                            }
                            Notifications sendNotification=new Notifications(title,notification1.getId(),social_choice,clientId);
                            Server.Notification.get(notification1.getId()).add(sendNotification);
                        }
                    }
                    case 17 -> handlePhotoRequest();
                    default -> System.out.println("Unknown request code: " + num);
                }
            }

        } catch (IOException | ClassNotFoundException e) {
            System.out.println("❌ Client disconnected or error occurred.");
        } finally {
            try {
                socket.close();
                System.out.println("🔒 Closed client socket: " + socket);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    //Server Wide Methods
    public void updateFile(String fileName,int id){
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(fileName, true))) {
            String fileBaseName = fileName.contains(".") ? fileName.substring(0, fileName.lastIndexOf('.')) : fileName;
            writer.write( id+ " posted " + fileBaseName);
            writer.newLine();
            System.out.println("Post entry written to profile file.");
        } catch (IOException e) {
            System.out.println("Error writing to profile file: " + e.getMessage());
        }
    }
    public static void CreateFiles(String fileName, String folderPath, int maxKey){
        try{
            File file1 = new File(folderPath+ fileName);
            System.out.println(folderPath+fileName);
            if (file1.createNewFile()) {
                System.out.println("File created: " + file1.getName());
            } else {
                System.out.println("File already exists.");
            }

        }catch (IOException e){
            e.printStackTrace();
        }
        try{
            Path path = Paths.get(folderPath+ "Client"+(maxKey+1)+"Images");
            Files.createDirectory(path);
        }catch (IOException e){
            System.out.println("Folder already exists.");
        }
    }
    public ArrayList<Integer> Search(String PhotoName){
        ArrayList<Integer> matchingClients = new ArrayList<>();

        String baseFolder = "src/main/java/org/example/ServerFolder/";

        for (int clientId : Server.Clients.keySet()) {
            String imageFolderPath = baseFolder + "Client" + clientId + "Images/";
            File imageFolder = new File(imageFolderPath);

            if (imageFolder.exists() && imageFolder.isDirectory()) {
                File[] files = imageFolder.listFiles();
                if (files != null) {
                    for (File file : files) {
                        if (file.isFile()) {
                            String fileName = file.getName();
                            String nameWithoutExtension = fileName.contains(".") ? fileName.substring(0, fileName.lastIndexOf('.')) : fileName;

                            if (nameWithoutExtension.equalsIgnoreCase(PhotoName)) {
                                matchingClients.add(clientId);
                                break; // Found match for this client, no need to check more files
                            }
                        }
                    }
                }
            }
        }

        return matchingClients;
    }
    public static void copyfiles(String sourcePath,String destinationFolder){
        try {
            Path sourceFile = Paths.get(sourcePath);
            Path destinationFile = Paths.get(destinationFolder + sourceFile.getFileName());

            // Create destination folder if it doesn't exist
            Files.createDirectories(Paths.get(destinationFolder));

            // Copy the file (REPLACE_EXISTING will overwrite if file already exists)
            Files.copy(sourceFile, destinationFile, StandardCopyOption.REPLACE_EXISTING);

            System.out.println("File copied successfully to " + destinationFile);
        } catch (IOException e) {
            System.out.println("Error while copying file: " + e.getMessage());
        }
    }
    //SingIN
    private void handleSignIn() throws IOException, ClassNotFoundException {
        String email = (String) in.readObject();
        String password = (String) in.readObject();
        String value = email + " , " + password;

        int newId = Server.Clients.keySet().stream().max(Integer::compareTo).orElse(-1) + 1;
        Server.Clients.put(newId, value);
        Server.clientConnections.put(newId, socket);
        Server.Followers.putIfAbsent(newId, new ArrayList<>());
        Server.Following.putIfAbsent(newId, new ArrayList<>());
        Server.Notification.putIfAbsent(newId, new ArrayList<>());

        out.writeInt(newId);
        out.flush();

        out.writeObject(Server.Followers.get(newId));
        out.flush();

        out.writeObject(Server.Following.get(newId));
        out.flush();

        String fileName = "Profile_XClient" + newId + ".txt";
        String folderPath = "src/main/java/org/example/ServerFolder/";
        File profile = new File(folderPath + fileName);
        if (!profile.exists()) {
            profile.createNewFile();
        }

        Files.createDirectories(Paths.get(folderPath + "Client" + newId + "Images/"));

        System.out.println("🆕 Registered client " + newId);
        System.out.println("Welcome client "+ newId);
    }
    //Login
    private void handleLogin() throws IOException, ClassNotFoundException {
        String email = (String) in.readObject();
        String password = (String) in.readObject();
        String value = email + " , " + password;

        int foundId = -1;
        for (Map.Entry<Integer, String> entry : Server.Clients.entrySet()) {
            if (entry.getValue().equals(value)) {
                foundId = entry.getKey();
                break;
            }
        }

        out.writeInt(foundId);
        out.flush();

        if (foundId != -1) {
            out.writeObject(Server.Followers.get(foundId));
            out.flush();

            out.writeObject(Server.Following.get(foundId));
            out.flush();

            Server.clientConnections.put(foundId, socket);
            Server.Notification.putIfAbsent(foundId, new ArrayList<>());
            System.out.println("🔐 Client logged in: " + foundId);
            System.out.println("Welcome client "+foundId);
        } else {
            System.out.println("❗ Login failed");
        }
    }
    //Access Profile
    private void handleProfileAccess(int clientId, String filePath) throws IOException, ClassNotFoundException {
        System.out.println("Client " + clientId + " is attempting to access: " + filePath);

        synchronized (Server.class) {
            File lockFile = new File(filePath + ".lock");
            ArrayList<Integer> waitingClients = Server.waitingClients.computeIfAbsent(filePath, k -> new ArrayList<>());

            if (lockFile.exists()) {
                waitingClients.add(clientId);
                out.writeObject("File is currently locked. You are in queue.");
                out.flush();
                return;
            }

            lockProfileFile(lockFile);

            startTimeoutTimer(clientId, lockFile);

            sendProfileContent(filePath);

            waitForUnlockSignal(clientId, lockFile, waitingClients);
        }
    }

    private void lockProfileFile(File lockFile) throws IOException {
        if (!lockFile.createNewFile()) {
            throw new IOException("Failed to create lock file.");
        }
    }

    private void startTimeoutTimer(int clientId, File lockFile) {
        new Thread(() -> {
            try {
                Thread.sleep(15000); // 15 seconds
                if (lockFile.exists()) {
                    try {
                        ObjectOutputStream timeoutOut = new ObjectOutputStream(socket.getOutputStream());
                        timeoutOut.writeObject("⚠️ Timeout: Please save and release the file. It will be auto-unlocked soon.");
                        timeoutOut.flush();
                    } catch (IOException e) {
                        System.out.println("Unable to send timeout warning to client " + clientId);
                    }
                }
            } catch (InterruptedException ignored) {}
        }).start();
    }

    private void sendProfileContent(String filePath) throws IOException {
        BufferedReader reader = new BufferedReader(new FileReader(filePath));
        StringBuilder content = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            content.append(line).append("\n");
        }
        reader.close();
        out.writeObject(content.toString());
        out.flush();
    }

    private void waitForUnlockSignal(int clientId, File lockFile, ArrayList<Integer> waitingClients) throws IOException, ClassNotFoundException {
        String unlockSignal = (String) in.readObject();
        if ("UNLOCK".equalsIgnoreCase(unlockSignal)) {
            lockFile.delete();
            System.out.println("Client " + clientId + " has unlocked the file.");

            if (!waitingClients.isEmpty()) {
                waitingClients.remove(0);
            }
        }
    }
    //Search
    private void handlePhotoRequest() throws IOException, ClassNotFoundException {
        int request = in.readInt();
        if (request != 1) {
            out.writeInt(2);
            out.flush();
            return;
        }

        out.writeInt(1);
        out.flush();

        int clientId = in.readInt();
        int serverId = in.readInt();
        String photoName = (String) in.readObject();

        String photoPath = "src/main/java/org/example/ServerFolder/Client" + serverId + "Images/" + photoName + ".png";
        File file = new File(photoPath);

        if (!file.exists()) {
            out.writeUTF("ERROR: Photo not found.");
            out.flush();
            return;
        }

        byte[] fileBytes = Files.readAllBytes(file.toPath());
        sendPhotoChunks(fileBytes);

        sendAccompanyingText(serverId, photoName);
        out.writeUTF("The transmission is completed");
        out.flush();

        copyImageAndTextFiles(serverId, clientId, photoName);
        System.out.println("Server synced files for Client" + clientId);
    }
    private void sendPhotoChunks(byte[] fileBytes) throws IOException {
        int totalParts = 10;
        int chunkSize = fileBytes.length / totalParts;
        int extra = fileBytes.length % totalParts;

        for (int i = 0; i < totalParts; i++) {
            int start = i * chunkSize;
            int end = (i == totalParts - 1) ? (start + chunkSize + extra) : (start + chunkSize);
            byte[] chunk = Arrays.copyOfRange(fileBytes, start, end);

            boolean acknowledged = false;
            int retries = 0;

            while (!acknowledged && retries < 3) {
                try {
                    out.writeInt(i);
                    out.writeInt(chunk.length);
                    out.write(chunk);
                    out.flush();

                    socket.setSoTimeout(3000);

                    int ack = in.readInt();

                    if (ack == i) {
                        acknowledged = true;
                    } else if (ack == i - 1) {
                        System.out.println("⏸️ Received duplicate ACK for part " + ack + " — delaying before continuing...");
                        try {
                            Thread.sleep(1500); // 1.5-second delay
                        } catch (InterruptedException ignored) {}
                        acknowledged = true;
                    } else {
                        System.out.println("⚠️ Unexpected ACK: " + ack + " for part " + i);
                        retries++;
                    }

                } catch (SocketTimeoutException e) {
                    System.out.println("⏰ ACK timeout for part " + i + ", retrying...");
                    retries++;
                } catch (IOException e) {
                    System.out.println("💥 IOException while sending part " + i + ": " + e.getMessage());
                    return;
                }
            }

            if (!acknowledged) {
                System.out.println("❌ Failed to deliver part " + i + " after 3 retries. Aborting.");
                return;
            }
        }
    }
    private void sendAccompanyingText(int serverId, String photoName) throws IOException {
        String textPath = "src/main/java/org/example/ServerFolder/Client" + serverId + "Images/" + photoName + ".txt";
        File textFile = new File(textPath);
        if (textFile.exists()) {
            BufferedReader reader = new BufferedReader(new FileReader(textFile));
            StringBuilder text = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                text.append(line).append("\n");
            }
            reader.close();
            out.writeUTF(text.toString());
        } else {
            out.writeUTF("No accompanying text found for this photo.");
        }
        out.flush();
    }

    private void copyImageAndTextFiles(int serverId, int clientId, String photoName) {
        String photoPath = "src/main/java/org/example/ServerFolder/Client" + serverId + "Images/" + photoName + ".png";
        String textPath = "src/main/java/org/example/ServerFolder/Client" + serverId + "Images/" + photoName + ".txt";
        String destImageFolder = "src/main/java/org/example/ServerFolder/Client" + clientId + "Images/";
        String destTextFile = destImageFolder + photoName + ".txt";

        copyfiles(photoPath, destImageFolder);
        copyfiles(textPath, destTextFile);
    }
}
