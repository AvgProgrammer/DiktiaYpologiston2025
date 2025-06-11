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
                String menu="";
                try {
                    menu=(String)in.readObject();
                } catch (EOFException e) {
                    break; // client disconnected
                }

                switch (menu) {
                    case "Sign Up" -> handleSignUp();
                    case "Log In" -> handleLogin();
                    case "Post" -> {
                        String LanguageText=(String)in.readObject();
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
                        int j=0;
                        for(int i=0; i<Server.TestClients.size();i++){
                            if(Server.TestClients.get(i).getId()==clientId){
                                j=i;
                            }
                        }
                        for (int follower : Server.TestClients.get(j).getFollowers()){
                            if(Server.clientConnections.containsKey(follower)){
                                String title=clientId+" post a photo with name: "+ fileName.replaceFirst("\\.png$", "");
                                Notifications notific=new Notifications(title,follower,0,clientId);
                                Server.Notification.get(follower).add(notific);
                            }
                        }
                        System.out.println("Language:"+LanguageText);
                        Post post=new Post(fileName.replaceFirst("\\.png$", ""),LanguageText,"Client"+clientId,clientId);
                        System.out.println();
                        Server.Posts.get(clientId).add(post);
                        Server.TestClients.get(clientId).addToPosts(post);
                        broadcastLiveToFollowers(clientId, post);
                    }
                    case "Access Profile" -> {
                        int requesterId=in.readInt();
                        int clientId = in.readInt();
                        String filePath = "src/main/java/org/example/ServerFolder/Profile_XClient" + clientId + ".txt";
                        handleProfileAccess(requesterId, filePath);
                    }
                    case "Search Photo" -> {
                        String photoname=(String) in.readObject();
                        String photoLanguage=(String) in.readObject();
                        ArrayList<Integer> ReturnList= Search(photoname,photoLanguage);
                        System.out.println("Array was sent");
                        out.writeObject(ReturnList);
                        out.flush();
                    }
                    case "Update Social Graph" -> {
                        int social_choise=in.readInt();
                        int clientId=in.readInt();
                        int id=in.readInt();

                        if(social_choise==1){
                            Notifications notifications=new Notifications("Follow Request",clientId, 1,id);
                            Server.Notification.get(id).add(notifications);
                        }else{
                            System.out.println("Client "+clientId+" has unfollowed "+ id);
                            ArrayList<Integer> dummyArrayList=Server.TestClients.get(clientId).getFollowing();
                            dummyArrayList.remove(id);
                            Server.TestClients.get(clientId).setFollowing(dummyArrayList);
                            dummyArrayList=null;
                        }
                    }
                    case "Notification" -> {
                        int clientId=in.readInt();
                        out.writeObject(Server.Notification.get(clientId));
                        out.flush();
                    }
                    case "Invitation" -> {
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
                                int j=0;
                                for(int i=0; i<Server.TestClients.size();i++){
                                    if(Server.TestClients.get(i).getId()==clientId){
                                        j=i;
                                    }
                                }
                                Server.TestClients.get(j).addFollower(notification1.getId());
                            }else if(social_choice==3){
                                System.out.println("Client "+clientId+" has reject "+ notification1.getClientId());
                                title="Follow Request Rejected";
                            }else if(social_choice==5 ){
                                title="Comment Request Accepted";
                                String comment=(String) in.readObject();
                                try {
                                    FileWriter writer = new FileWriter("src/main/java/org/example/ServerFolder/Profile_XClient" + notification1.getId() + ".txt");
                                    writer.write(comment);
                                    writer.close();
                                    System.out.println("✅ Successfully wrote to the file.");
                                } catch (IOException e) {
                                    System.out.println("❌ An error occurred.");
                                    e.printStackTrace();
                                }
                            }else if(social_choice==6){
                                title="Comment Request Rejected";
                            }else if(social_choice==8){
                                title="Photo Request Accepted";
                                System.out.println("The Receiver:"+notification1.getId());
                                System.out.println("The Sender:"+clientId);
                                System.out.println("notification1.getClientId():"+notification1.getClientId());
                                System.out.println("notification1.getId():"+notification1.getId());
                            }else if(social_choice==9){
                                title="Photo Request Rejected";
                            }
                            Notifications sendNotification=new Notifications(title,notification1.getId(),social_choice,clientId);
                            if(social_choice==8){
                                String comment=(String) in.readObject();
                                sendNotification.setMessage(comment);
                            }
                            Server.Notification.get(notification1.getId()).add(sendNotification);
                        }
                    }
                    case "Photo Request" -> handlePhotoRequest();
                    case "Photo Request Invitation" -> {
                        int senderId=in.readInt();
                        int receiverId=in.readInt();
                        System.out.println("request part:");
                        String imagesName=(String) in.readObject();
                        Notifications requestImageNotification=new Notifications("The user "+senderId+" request the image:"+ imagesName,receiverId,7,senderId);
                        requestImageNotification.setMessage(imagesName);
                        for (int i=0; i<Server.TestClients.get(receiverId).getPosts().size();i++){
                            if(Server.TestClients.get(receiverId).getPosts().get(i).getImageName().equals(imagesName)){
                                Server.TestClients.get(receiverId).getPosts().get(i).addNumberOfRequests();
                            }
                        }
                        Server.Notification.get(receiverId).add(requestImageNotification);
                    }
                    case "Comment" ->{
                        int senderId=in.readInt();
                        int receiverId=in.readInt();

                        String ImagesName=(String) in.readObject();
                        String comment=(String) in.readObject();
                        Notifications CommentNotification=new Notifications("Comment for Image:"+ImagesName,receiverId,4,senderId);
                        CommentNotification.setMessage(comment);
                        Server.Notification.get(receiverId).add(CommentNotification);
                    }
                    default -> System.out.println("Unknown request code: " + menu);
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
    //Search Photo
    public ArrayList<Integer> Search(String PhotoName,String PhotoLanguage){
        ArrayList<Integer> matchingClients = new ArrayList<>();

        for (Map.Entry<Integer, ArrayList<Post>> entry : Server.Posts.entrySet()) {
            Integer clientId = entry.getKey();
            ArrayList<Post> postsList = entry.getValue();

            for (Post post : postsList) {
                if (post.getImageName().equals(PhotoName) &&
                        post.getPost_Language().equals(PhotoLanguage)) {
                    matchingClients.add(clientId);
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
    //SignUp
    private void handleSignUp() throws IOException, ClassNotFoundException {
        String email = (String) in.readObject();
        String password = (String) in.readObject();

        int newId = Server.TestClients.size();

        Server.clientConnections.put(newId, socket);

        ClientProfile TestClient=new ClientProfile(newId,"Client"+newId,email,password);
        System.out.println(TestClient);
        Server.TestClients.add(TestClient);
        Server.Notification.putIfAbsent(newId, new ArrayList<>());

        out.writeInt(newId);
        out.flush();

        ArrayList<Integer> testArray=new ArrayList<>();

        out.writeObject(testArray);
        out.flush();

        out.writeObject(testArray);
        out.flush();

        testArray=null;

        String fileName = "Profile_XClient" + newId + ".txt";
        String folderPath = "src/main/java/org/example/ServerFolder/";
        File profile = new File(folderPath + fileName);
        if (!profile.exists()) {
            profile.createNewFile();
        }

        Server.fileOccupied.put(folderPath + fileName,0);
        ArrayList<Integer> ArrayList= new ArrayList<>();
        Server.waitingClients.put(folderPath + fileName,ArrayList);
        Files.createDirectories(Paths.get(folderPath + "Client" + newId + "Images/"));

        ArrayList<Post> TestPosts=new ArrayList<>();
        Server.Posts.put(newId,TestPosts);
        TestPosts=null;

        System.out.println("🆕 Registered client " + newId);
        System.out.println("Welcome client "+ newId);
    }
    //Login
    private void handleLogin() throws IOException, ClassNotFoundException {
        String email = (String) in.readObject();
        String password = (String) in.readObject();

        int foundId = -1;
        for (ClientProfile client : Server.TestClients) {
            if (client.getEmail().equals(email) && client.getPassword().equals(password)) {
                foundId = client.getId();
                break;
            }
        }

        out.writeInt(foundId);
        out.flush();

        if (foundId != -1) {
            String fileName = "Profile_XClient" + foundId + ".txt";
            String folderPath = "src/main/java/org/example/ServerFolder/";
            Server.fileOccupied.put(folderPath + fileName,0);
            out.writeObject(Server.TestClients.get(foundId).getFollowers());
            out.flush();

            out.writeObject(Server.TestClients.get(foundId).getFollowing());
            out.flush();

            Server.clientConnections.put(foundId, socket);
            Server.Notification.putIfAbsent(foundId, new ArrayList<>());
            System.out.println("🔐 Client logged in: " + foundId);
            System.out.println("Welcome client "+foundId);

            sendAllImagesForClient(foundId);
        } else {
            System.out.println("❗ Login failed");
        }
    }
    //Access Profile
    private void sendAllImagesForClient(int clientId) throws IOException {
        File folder = new File("src/main/java/org/example/ServerFolder/Client" + clientId + "Images");
        DataOutputStream dos = new DataOutputStream(socket.getOutputStream());

        if (folder.exists() && folder.isDirectory()) {
            File[] files = folder.listFiles();
            if (files != null) {
                for (File image : files) {
                    if (!image.isFile()) continue;

                    dos.writeUTF("IMAGE_TRANSFER");
                    dos.writeUTF(image.getName());
                    dos.writeLong(image.length());

                    FileInputStream fis = new FileInputStream(image);
                    byte[] buffer = new byte[4096];
                    int bytesRead;
                    while ((bytesRead = fis.read(buffer)) != -1) {
                        dos.write(buffer, 0, bytesRead);
                    }
                    fis.close();
                }
            }
        }

        // Signal end of transfer
        dos.writeUTF("IMAGE_TRANSFER_DONE");
        dos.flush();
    }

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

        boolean success=false;
        //success = sendPhotoChunksWithStopAndWait(fileBytes);
        success = sendPhotoChunksWithGoBackN(fileBytes);

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

        for (int i=0; i<Server.TestClients.get(serverId).getPosts().size();i++){
            if(Server.TestClients.get(serverId).getPosts().get(i).getImageName().equals(photoName)){
                Server.TestClients.get(serverId).getPosts().get(i).addToListOfClient(clientId);
                Server.TestClients.get(serverId).getPosts().get(i).addUserWithAccess();
            }
        }

    }

    private boolean sendPhotoChunksWithStopAndWait(byte[] fileBytes) throws IOException {
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

    private boolean sendPhotoChunksWithGoBackN(byte[] fileBytes) throws IOException{
        final int TOTAL_PARTS = 10;
        int chunkSize = fileBytes.length / TOTAL_PARTS;
        int extra = fileBytes.length % TOTAL_PARTS;
        boolean[] acked = new boolean[TOTAL_PARTS];

        final int WINDOW_SIZE = 3;
        Map<Integer, byte[]> chunkMap = new HashMap<>();

        for (int i = 0; i < TOTAL_PARTS; i++) {
            int start = i * chunkSize;
            int end = (i == TOTAL_PARTS - 1) ? (start + chunkSize + extra) : (start + chunkSize);
            byte[] chunk = Arrays.copyOfRange(fileBytes, start, end);
            chunkMap.put(i, chunk);
        }
        int first=0;
        int last=WINDOW_SIZE-1;
        while(first<TOTAL_PARTS){

            int j=first;
            System.out.println("🚚 Sending window: " + first + " to " + last);
            while(j<=last){
                out.writeInt(j);
                out.flush();

                byte[] chunk = chunkMap.get(j);
                out.writeInt(chunk.length);
                out.flush();

                out.write(chunk);
                out.flush();
                j++;
            }

            long startTime = System.currentTimeMillis();
            long timeout = 2500;
            int ack=-1;
            while (System.currentTimeMillis() - startTime < timeout) {
                try {
                    if (in.available() > 0) {
                        ack = in.readInt();
                        if(first==ack){
                            System.out.println("🛂 Received ACK: " + ack + " (expecting " + first + ")");
                            acked[first] = true;
                            first++;
                            if(last<TOTAL_PARTS-1){
                                last++;
                            }
                        }else if(ack<first){
                            System.out.println("🔁 Duplicate ACK received for part " + ack);
                        }

                    }
                } catch (IOException e) {
                    System.out.println("❌ Error reading ACK: " + e.getMessage());
                }
            }
            if(ack==-1){
                System.out.println("♻ ACK timeout for part: " + ack + ", retrying...");
            }
        }
        out.writeInt(-2); // End signal
        out.flush();

        for (int i = 0; i < acked.length; i++) {
            if (!acked[i]) {
                System.out.println("❌ Chunk " + i + " was not acknowledged.");
                return false;
            }
        }

        System.out.println("✅ All chunks were successfully acknowledged.");
        return true;
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

    private void broadcastLiveToFollowers(int posterId, Post post) {
        for (ClientProfile client : Server.TestClients) {
            if (client.getFollowing().contains(posterId)) {
                int followerId = client.getId();
                ObjectOutputStream followerOut = Server.notificationStreams.get(followerId);
                if (followerOut != null) {
                    try {
                        followerOut.writeObject("NewFollowedPost");
                        followerOut.flush();
                        followerOut.writeObject("Client:" + posterId + "\nPhotos Name:" + post.getImageName() + "\nLanguage:" + post.getPost_Language());
                        followerOut.flush();
                    } catch (IOException e) {
                        System.err.println("Failed to send post to client " + followerId);
                    }
                }
            }
        }
    }
}