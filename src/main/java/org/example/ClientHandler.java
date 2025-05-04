package org.example;

import javax.management.Notification;
import java.io.*;
import java.net.*;
import java.util.*;
import java.nio.file.*;

public class ClientHandler extends Thread {
    private final Socket socket;
    private ObjectInputStream in;
    private ObjectOutputStream out;
    public boolean timedOut=false;
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

                        String baseName = fileName.replaceFirst("\\.png$", "");
                        String txtPath = saveDir + baseName + ".txt";
                        try (BufferedWriter txtWriter = new BufferedWriter(new FileWriter(txtPath))) {
                            txtWriter.write("The "+ baseName + " is amazing, fantastic");
                            System.out.println("Text file created at: " + txtPath);
                        } catch (IOException e) {
                            System.out.println("❌ Failed to create .txt file: " + e.getMessage());
                        }

                        String profilePath = "src/main/java/org/example/ServerFolder/Profile_XClient" + clientId + ".txt";
                        updateFile(profilePath,clientId);
                        System.out.println("Image received and saved to: " + fullImagePath);
                        DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
                        dos.writeInt(1);
                        dos.flush();
                        for (int follower : Server.Followers.get(clientId)){
                            if(Server.clientConnections.containsKey(follower)){
                                String title=clientId+" post a photo with name: "+ fileName.replaceFirst("\\.png$", "");
                                Notifications notific=new Notifications(title,follower,0,clientId);
                                Server.Notification.get(follower).add(notific);
                            }
                        }
                    }
                    case 12 -> {
                        int requesterId=in.readInt();
                        int clientId = in.readInt();
                        String filePath = "src/main/java/org/example/ServerFolder/Profile_XClient" + clientId + ".txt";
                        handleProfileAccess(requesterId, filePath);
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
                        int notificationIndex= in.readInt();
                        Notifications notification1=(Notifications) in.readObject();

                        for(Notifications notif:Server.Notification.get(clientId)){
                            System.out.println(notif.getTitle()+"|");
                        }

                        Server.Notification.get(clientId).remove(notificationIndex);

                        System.out.println("----------------");
                        for(Notifications notif:Server.Notification.get(clientId)){
                            System.out.println(notif.getTitle()+"|");
                        }

                        int social_choice=in.readInt()+1;
                        String title="";
                        if(social_choice!=1){
                            if (social_choice==2){
                                title="Follow Request Accepted";
                                System.out.println("Client "+clientId+" has accept "+ notification1.getClientId());
                                Server.Followers.get(clientId).add(notification1.getId());
                            }else if(social_choice==3){
                                System.out.println("Client "+clientId+" has reject "+ notification1.getClientId());
                                title="Follow Request Rejected";
                            }
                            Notifications sendNotification=new Notifications(title,notification1.getId(),social_choice,clientId);
                            Server.Notification.get(notification1.getClientId()).add(sendNotification);
                        }
                    }
                    case 17 -> handlePhotoRequest();
                    default -> System.out.println("Unknown request code: " + num);
                }
            }

        } catch (IOException | ClassNotFoundException e) {
            System.out.println("❌ Client disconnected or error occurred.");
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
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

        if (!profile.exists()) {
            profile.createNewFile();
        }
        Server.fileOccupied.put(folderPath + fileName,0);
        ArrayList<Integer> ArrayList= new ArrayList<>();
        Server.waitingClients.put(folderPath + fileName,ArrayList);
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
    private void handleProfileAccess(int requesterID, String filePath) throws IOException, ClassNotFoundException, InterruptedException {
        System.out.println("Client " + requesterID + " is attempting to access: " + filePath);
        if(Server.fileOccupied.get(filePath)==0){

            Server.fileOccupied.replace(filePath,1);
            out.writeInt(-1);
            out.flush();

            out.writeObject(sendProfileContent(filePath));
            out.flush();
            Thread timer=startTimer(60000);
            int anwser=in.readInt();
            if(anwser==1){
                timer.join();
                String msg1="The Server will auto-unlock the file";
                out.writeObject(msg1);
                out.flush();
            }else{
                String msg2="ACK";
                out.writeObject(msg2);
                out.flush();
            }
            Server.fileOccupied.replace(filePath,0);
            notifyWaitingClients(filePath);
            System.out.println("The files was unlock and Notifications were send");

        }else{
            System.out.println("The file is locked");
            Server.waitingClients.get(filePath).add(requesterID);
            System.out.println("---------");
            for(int clients : Server.waitingClients.get(filePath)){
                System.out.println(clients);
            }
            System.out.println("---------");
            out.writeInt(1);
            out.flush();
        }

    }

    public Thread startTimer(long milliseconds) {
        Thread timerThread = new Thread(() -> {
            try {
                Thread.sleep(milliseconds);
                setTimedOut(true);
                System.out.println("⏰ Timer finished.");
            } catch (InterruptedException ignored) {}
        });
        timerThread.start();
        return timerThread;
    }

    public void setTimedOut(boolean timedOut) {
        this.timedOut = timedOut;
    }
    private String sendProfileContent(String filePath) throws IOException {
        BufferedReader reader = new BufferedReader(new FileReader(filePath));
        StringBuilder content = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            content.append(line).append("\n");
        }
        reader.close();
        return content.toString();
    }
    public static void notifyWaitingClients(String filePath) {
        ArrayList<Integer> clients = Server.waitingClients.get(filePath);

        if (clients != null) {
            for (int clientId : clients) {
                Notifications notific= new Notifications("The file is free",-1,3 ,clientId);
                Server.Notification.get(clientId).add(notific);
            }
        } else {
            System.out.println("ℹ️ No waiting clients for: " + filePath);
        }
    }
    //Search
    private void handlePhotoRequest() throws IOException, ClassNotFoundException {
        int request = in.readInt();
        if (request != 1) {
            out.writeInt(2);
            out.flush();
            System.out.println("First-Hand Rejected");
            return;
        }
        System.out.println("First-Hand Accepted");
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

        boolean success = sendPhotoChunks(fileBytes);
        if (!success) {
            System.out.println("❌ Aborting: One or more chunks failed.");
            return; // Exit without copying or sending additional data
        }
        out.writeInt(99); // Signal: text is coming
        out.flush();

        sendAccompanyingText(serverId, photoName);

        out.writeInt(100); // Signal: final confirmation message is coming
        out.flush();

        out.writeUTF("The transmission is completed");
        out.flush();

        copyImageAndTextFiles(serverId, clientId, photoName);
        System.out.println("Server synced files for Client" + clientId);

    }
    private boolean sendPhotoChunks(byte[] fileBytes) throws IOException {
        int totalParts = 10;
        int chunkSize = fileBytes.length / totalParts;
        int extra = fileBytes.length % totalParts;

        boolean[] flags = new boolean[totalParts];
        for (int i = 0; i < totalParts; i++) {
            int start = i * chunkSize;
            int end = (i == totalParts - 1) ? (start + chunkSize + extra) : (start + chunkSize);
            byte[] chunk = Arrays.copyOfRange(fileBytes, start, end);

            boolean acknowledged = false;
            int retries = 0;
            int ack;
            while (!acknowledged && retries < 3) {
                try {
                    System.out.println("------------------------------------------------");
                    System.out.println("Part Number: "+ i + " Length: "+ chunk.length);

                    out.writeInt(i);
                    out.flush();

                    out.writeInt(chunk.length);
                    out.flush();

                    out.write(chunk);
                    out.flush();

                    Thread timer=startTimer(2500);
                    timer.join();
                    ack = in.readInt();

                    System.out.println("After Reading");
                    System.out.println("🛂 Received ACK: " + ack + " (expecting " + i + ")");
                    if (ack == i) {
                        acknowledged = true;
                        flags[i] = true;
                    }else if (ack < i && ack!=-3) {
                        System.out.println("🔁 Duplicate ACK received for part " + ack);
                        // Don't retry, just ignore and wait again
                    }else  {
                        System.out.println("♻ ACK timeout for part: " + ack + ", retrying...");
                        retries++;
                    }

                }catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
            if(i==totalParts-1){
                System.out.println("We are at the chunk: "+ (totalParts-1) + " And the i is: "+ i);
                out.writeInt(-2);
                out.flush();
            }

            if (!acknowledged) {
                System.out.println("❌ Failed to deliver part " + i + " after 3 retries. Aborting.");
                out.writeInt(-1);  // special code for ABORT
                out.flush();
                return false;
            }
        }
        boolean allDelivered = true;
        for (int j = 0; j < flags.length; j++) {
            if (!flags[j]) {
                allDelivered = false;
                System.out.println("❌ Chunk " + j + " was not acknowledged.");
            }
        }
        if (allDelivered) {
            System.out.println("✅ All chunks were successfully acknowledged.");
            return true;
        } else {
            System.out.println("⚠️ Some chunks failed to be acknowledged.");
            return false;
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

        copyfiles(photoPath, destImageFolder);
        copyfiles(textPath, destImageFolder);
    }
}
