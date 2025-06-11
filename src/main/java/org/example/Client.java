package org.example;


import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.util.*;

public class Client extends Thread {
    public static int clientId = -1;
    private static ArrayList<Integer> following = new ArrayList<>();
    private static ArrayList<Integer> followers = new ArrayList<>();

    public static ArrayList<Notifications> getNotificationList() {
        return NotificationList;
    }

    public static void setNotificationList(ArrayList<Notifications> notificationList) {
        NotificationList = notificationList;
    }

    private static ArrayList<Notifications> NotificationList = new ArrayList<>();

    public static HashMap<Integer, ArrayList<String> > DonwloadedImage= new HashMap<>();

    public static void createFiles(int clientId){
        try {
            String fileName = "Profile_XClient" + clientId + ".txt";
            String folderPath = "src\\main\\java\\org\\example\\DataFolder\\";
            File file = new File(folderPath + fileName);
            //System.out.println(folderPath + fileName);
            file.createNewFile();
            /*if (file.createNewFile()) {
                System.out.println("File created: " + file.getName());
            } else {
                System.out.println("File already exists.");
            }*/

            File file1 = new File("src\\main\\java\\org\\example\\DataFolder\\Profile_XClientGuest"+clientId+".txt");
            file1.createNewFile();
            /*if (file1.createNewFile()) {
                System.out.println("File created: " + file1.getName());
            } else {
                System.out.println("File already exists.");
            }*/

            File file2 = new File("src\\main\\java\\org\\example\\DataFolder\\Others_Xclient"+clientId+".txt");
            file2.createNewFile();
            /*if (file2.createNewFile()) {
                System.out.println("File created: " + file2.getName());
            } else {
                System.out.println("File already exists.");
            }*/

            String ImagefolderPath = "src\\main\\java\\org\\example\\DataFolder\\Client" + clientId + "Image";
            File folder = new File(ImagefolderPath);

            if (!folder.exists()) {
                boolean success = folder.mkdirs();
                if (success) {
                    //System.out.println("Folder created successfully at: " + folder.getAbsolutePath());
                } else {
                    //System.out.println("Failed to create folder.");
                }
            } else {
                //System.out.println("Folder already exists at: " + folder.getAbsolutePath());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void PrintNotifications(){
        ArrayList<Notifications> notifications = getNotificationList();
        if (notifications.isEmpty()) {
            System.out.println("No notifications available.");
        } else {
            System.out.println("Your Notifications:");
            int i=0;
            for (Notifications notification : notifications) {
                System.out.println(i+")"+notification.PrintNotification());
                i++;
            }
        }
    }
    public static Path Post(int clientId,Path destPath, String photoPath){
        try {
            Path PathofPhoto= Path.of(photoPath);

            Path imageFileName = PathofPhoto.getFileName();
            Path newImagePath = Files.copy(PathofPhoto, destPath.resolve(imageFileName), StandardCopyOption.REPLACE_EXISTING);

            String baseName = imageFileName.toString().replaceFirst("\\.png$", "");

            // ✅ Create accompanying .txt file in same folder
            Path txtFilePath = destPath.resolve(baseName + ".txt");
            try (BufferedWriter txtWriter = new BufferedWriter(new FileWriter(txtFilePath.toFile()))) {
                txtWriter.write("The "+ baseName + " is amazing, fantastic");
            }

            String profileFilePath = "src/main/java/org/example/DataFolder/Profile_XClient" + clientId + ".txt";

            try (BufferedWriter writer = new BufferedWriter(new FileWriter(profileFilePath, true))) {
                writer.write(clientId + " posted " + imageFileName);
                writer.newLine();
            } catch (IOException e) {
                System.out.println("Error updating client profile: " + e.getMessage());
            }

            System.out.println("Image saved at: " + newImagePath.toString());
            return newImagePath;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return destPath;
    }

    public static void main(String[] args) throws InterruptedException, IOException {
        int menu_choice;
        String email = "";
        String password = "";
        Scanner sc = new Scanner(System.in);
        do {
            System.out.println("Please choose one of the following\n0)Sing In\n1)Login");
            menu_choice = sc.nextInt();
            System.out.println("-----------------------------------------");
            System.out.println("Email");
            sc.nextLine();
            String menu="";
            if(menu_choice==0){
                menu = "Sign Up";
            }else if(menu_choice==1){
                menu="Log In";
            }
            email = sc.nextLine();
            System.out.println("Password");
            password = sc.nextLine();
            Client client = new Client(menu);
            client.setEmail(email);
            client.setPassword(password);
            client.start();
            client.join();
            clientId = client.getReceivedId();
        } while (clientId == -1);

        createFiles(clientId);

        try {
            // 🔔 Connect to the notification port
            Socket notifSocket = new Socket("localhost", 12345);
            ObjectOutputStream notifOut = new ObjectOutputStream(notifSocket.getOutputStream());
            ObjectInputStream notifIn = new ObjectInputStream(notifSocket.getInputStream());

            // Send client ID to register for updates
            notifOut.writeObject(clientId);
            notifOut.flush();

            // Start listener for updates
            startFollowedPostListener(notifIn);

        } catch (IOException e) {
            System.out.println("Failed to connect to notification server: " + e.getMessage());
        }

        do {

            System.out.println(
                    "Please choose one of the following:\n1)Post \n2)Access Profile\n3)Search Photo\n4)Update Social Graph\n5)Notification\n6)Comment\n7)Exit"
            );
            menu_choice = sc.nextInt();
            String menu="";
            System.out.println("-----------------------------------------");

            if (menu_choice == 1) {
                menu="Post";
                Client client = new Client(menu);
                System.out.println("Enter the path of the photograph:");
                sc.nextLine();
                String photoPath = sc.nextLine();


                System.out.println("Enter the Language of the text:");
                String photoLanguage = sc.nextLine();

                Path destPath = Paths.get("src/main/java/org/example/DataFolder/Client" + clientId + "Image");
                client.setImagePath(Post(clientId,destPath,photoPath));
                client.setFindId(clientId);
                client.setEmail(photoLanguage);
                client.start();
                client.join();
            } else if (menu_choice == 2) {
                menu="Access Profile";
                Client client = new Client(menu);
                System.out.println("The available profiles are: ");
                for (int i = 0; i < following.size(); i++) {
                    System.out.print(following.get(i)+"|");
                }
                System.out.println("Please enter the Id:");
                int id_choice=sc.nextInt();
                client.setFindId(id_choice);
                client.setUpdateId(clientId);
                client.start();
                client.join();
            } else if (menu_choice == 3) {
                menu="Search Photo";
                Client client = new Client(menu);

                System.out.println("Enter the image's name:");

                sc.nextLine();
                String imagesName = sc.nextLine();

                System.out.println("Enter the language of text:");
                String Language = sc.nextLine();

                client.setEmail(imagesName);
                client.setPassword(Language);
                client.start();
                client.join();

                ArrayList<Integer> match=client.getMatchingClients();
                System.out.println("-----");
                if(match.isEmpty()){
                    System.out.println("No image was found with the name \""+imagesName+"\"");
                }else{
                    for(Integer item: match){
                        System.out.println(item);
                    }
                    System.out.println("-----");
                    System.out.println("Choose one:");

                    int SenderID=sc.nextInt();
                    menu="Photo Request Invitation";
                    Client client1=new Client(menu);
                    client1.setFindId(clientId);
                    client1.setUpdateId(SenderID);
                    client1.setEmail(imagesName);
                    client1.start();
                    client1.join();
                }

            } else if (menu_choice == 4) {

                menu="Update Social Graph";
                Client client = new Client(menu);
                System.out.println(
                        "Please choose one of the following:\n1)Follow \n2)Unfollow"
                );
                int social_choice = sc.nextInt();
                System.out.println("Enter the Id:");

                int updateId = sc.nextInt();
                client.setFindId(clientId);
                client.setUpdateId(updateId);
                client.setSocialChoice(social_choice);
                client.start();
                client.join();

                if(social_choice==2){
                    following.remove(updateId);
                }
            }else if(menu_choice == 5){

                menu="Notification";
                Client client = new Client(menu);
                client.setFindId(clientId);
                client.start();
                client.join();
                PrintNotifications();
                if(!getNotificationList().isEmpty()){
                    System.out.println("Enter the Notification you want to see:");
                    int index=sc.nextInt();
                    int NotificationsType=getNotificationList().get(index).getType();
                    menu="Invitation";
                    Client client1= new Client(menu);
                    client1.setFindId(clientId);
                    client1.setUpdateId(index);
                    client1.setNotifications(getNotificationList().get(index));

                    if(NotificationsType==1){

                        System.out.println("1)Accept\n2)Decline");
                        int invite_choice=sc.nextInt();
                        if (invite_choice == 1) {
                            followers.add(getNotificationList().get(index).getId());
                        }
                        client1.setSocialChoice(invite_choice);

                    }else if(NotificationsType==4){
                        System.out.println("1)Accept\n2)Decline");
                        int invite_choice=sc.nextInt();
                        String comment=getNotificationList().get(index).getMessage();
                        if(invite_choice==1){
                            try {
                                FileWriter writer = new FileWriter("src/main/java/org/example/DataFolder/Profile_Xclient" + clientId + ".txt");
                                writer.write(comment);
                                writer.close();
                            } catch (IOException e) {
                                System.out.println("❌ An error occurred.");
                                e.printStackTrace();
                            }
                        }
                        client1.setPassword(comment);
                        client1.setSocialChoice(invite_choice+4);
                    }else if(NotificationsType==7) {
                        System.out.println("1)Accept\n2)Decline");
                        int invite_choice = sc.nextInt();
                        String imagePath=getNotificationList().get(index).getMessage();
                        client1.setPassword(imagePath);
                        client1.setSocialChoice(invite_choice + 6);
                    }else{
                        client1.setSocialChoice(0);
                        if(NotificationsType==2){
                            following.add(getNotificationList().get(index).getId());
                        }
                    }

                    client1.start();
                    client1.join();

                    if(NotificationsType==8){
                        menu="Photo Request";
                        Client client2=new Client(menu);
                        int SenderID=getNotificationList().get(index).getId();
                        String imagesName=getNotificationList().get(index).getMessage();
                        System.out.println("Images Name:"+imagesName);
                        client2.setFindId(clientId);
                        client2.setUpdateId(SenderID);
                        client2.setEmail(imagesName);
                        client2.start();
                        client2.join();

                        System.out.println("Enter a comment to include with the repost:");
                        sc.nextLine(); // clear the buffer after nextInt
                        String comment = sc.nextLine();
                        if(DonwloadedImage.containsKey(SenderID)){
                            DonwloadedImage.get(SenderID).add(imagesName);
                        }else{
                            ArrayList<String> NamesString=new ArrayList<>();
                            NamesString.add(imagesName);
                            DonwloadedImage.put(SenderID,NamesString);
                            NamesString=null;
                        }
                        // Log the repost to Others_XclientID.txt
                        String repostLogPath = "src/main/java/org/example/DataFolder/Others_Xclient" + clientId + ".txt";
                        try (BufferedWriter writer = new BufferedWriter(new FileWriter(repostLogPath, true))) {
                            writer.write("Reposted from Client " + SenderID + ": " + imagesName + ".png");
                            writer.newLine();
                            writer.write("Comment: " + comment);
                            writer.newLine();
                            writer.newLine();
                        } catch (IOException e) {
                            System.out.println("Failed to log repost: " + e.getMessage());
                        }
                    }
                }
            }else if(menu_choice == 6){
                menu="Comment";
                List<String> allPosts = new ArrayList<>();

                System.out.println("📂 Available downloaded posts:");

                for (Map.Entry<Integer, ArrayList<String>> entry : DonwloadedImage.entrySet()) {
                    Integer clientId = entry.getKey();
                    ArrayList<String> postsList = entry.getValue();

                    for (String post : postsList) {
                        String display = "Client " + clientId + ": " + post;
                        allPosts.add(display);
                        System.out.println(allPosts.size() + ". " + display);
                    }
                }
                if (allPosts.isEmpty()) {
                    System.out.println("⚠️ No downloaded images found.");
                    return;
                }

                int choice = -1;
                while (choice < 1 || choice > allPosts.size()) {
                    System.out.print("Select a post by number (1-" + allPosts.size() + "): ");
                    choice = sc.nextInt();
                }

                String[] parts = allPosts.get(choice-1).split(": ", 2); // Limit = 2 to keep post intact if it has ":"
                String clientPart = parts[0]; // "Client 3"
                String postPart = parts[1];   // actual post (e.g., "image.png")

                int receiverId = Integer.parseInt(clientPart.replace("Client ", ""));

                System.out.println("Comment:");
                sc.nextLine();
                String comment=sc.nextLine();

                Client client = new Client(menu);

                client.setFindId(clientId);
                client.setUpdateId(receiverId);
                client.setEmail(postPart);
                client.setPassword(comment);

                client.start();
            }
        } while (menu_choice != 7);
        sc.close();
    }

    private String menu;
    private int receivedId;

    public void setMatchingClients(ArrayList<Integer> matchingClients) {
        MatchingClients = matchingClients;
    }

    private ArrayList<Integer> MatchingClients;
    private String email;
    private String password;
    private int findId;

    public Notifications getNotifications() {
        return notifications;
    }

    public void setNotifications(Notifications notifications) {
        this.notifications = notifications;
    }

    private Notifications notifications;

    public int getUpdateId() {
        return updateId;
    }

    public void setUpdateId(int updateId) {
        this.updateId = updateId;
    }

    private int updateId;

    public int getSocialChoice() {
        return socialChoice;
    }

    public void setSocialChoice(int socialChoice) {
        this.socialChoice = socialChoice;
    }

    private int socialChoice;

    public Path getImagepath() {
        return Imagepath;
    }

    public void setImagePath(Path imagepath) {
        Imagepath = imagepath;
    }

    private Path Imagepath;

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public void setFindId(int number) {
        this.findId = number;
    }

    public int getFindId() {
        return findId;
    }

    public Client(String menu) {
        this.menu = menu;
    }


    @Override
    public void run() {
        Socket socket = null;
        ObjectOutputStream out = null;

        try {
            socket = new Socket("localhost", 1234);
            out = new ObjectOutputStream(socket.getOutputStream());

            out.writeObject(menu);
            out.flush();
            switch (menu){
                case "Sign Up" ->{
                    out.writeObject(getEmail());
                    out.flush();

                    out.writeObject(getPassword());
                    out.flush();

                    ObjectInputStream in = new ObjectInputStream(socket.getInputStream());
                    receivedId = in.readInt();
                    if (receivedId!=-1){
                        followers=(ArrayList<Integer>)in.readObject();
                        following=(ArrayList<Integer>)in.readObject();}
                }
                case "Log In" -> {
                    out.writeObject(getEmail());
                    out.flush();

                    out.writeObject(getPassword());
                    out.flush();

                    ObjectInputStream in = new ObjectInputStream(socket.getInputStream());
                    receivedId = in.readInt();
                    if (receivedId!=-1){
                        followers=(ArrayList<Integer>)in.readObject();
                        following=(ArrayList<Integer>)in.readObject();
                        DataInputStream dataIn = new DataInputStream(socket.getInputStream());
                        while (true) {
                            String command = dataIn.readUTF();
                            if (command.equals("IMAGE_TRANSFER")) {
                                String fileName = dataIn.readUTF();       // e.g., "avatar.png"
                                long fileSize = dataIn.readLong();

                                // Create a unique file path for this client
                                File outputFile = new File("src/main/java/org/example/DataFolder/Client" + receivedId + "Image/" + fileName);
                                outputFile.getParentFile().mkdirs(); // Ensure parent directories exist
                                FileOutputStream fos = new FileOutputStream(outputFile);
                                byte[] buffer = new byte[4096];
                                long totalRead = 0;
                                int bytesRead;

                                while (totalRead < fileSize &&
                                        (bytesRead = dataIn.read(buffer, 0, (int)Math.min(buffer.length, fileSize - totalRead))) != -1) {
                                    fos.write(buffer, 0, bytesRead);
                                    totalRead += bytesRead;
                                }
                                fos.close();
                            } else if (command.equals("IMAGE_TRANSFER_DONE")) {
                                break;
                            }
                        }
                    }
                }
                case "Post" ->{
                    Path imagePath = getImagepath();
                    String LanguageText= "";
                    out.writeObject(getEmail());
                    out.flush();
                    if (imagePath != null) {
                        FileInputStream fis = getFileInputStream(imagePath, socket);
                        fis.close();
                    } else {
                        System.out.println("Image path is null!");
                    }
                }
                case "Access Profile" -> {
                    out.writeInt(getUpdateId());
                    out.flush();

                    out.writeInt(getFindId());
                    out.flush();

                    ObjectInputStream in = new ObjectInputStream(socket.getInputStream());

                    int availability = in.readInt();

                    if(availability==-1){
                        String message= (String) in.readObject();
                        System.out.println("Profile ----------");
                        System.out.println(message);
                        System.out.println("Do you want to return the file\n0)Yes \n1)No:");
                        Scanner sc1 = new Scanner(System.in);
                        int anwser = sc1.nextInt();

                        out.writeInt(anwser);
                        out.flush();

                        String response=(String)in.readObject();

                        System.out.println(response);
                    }else{
                        System.out.println("The file is locked we will notify you when if unlocked");
                    }

                }
                case "Search Photo" ->  {
                    out.writeObject(getEmail()); //Using The Email Variable to save storage
                    out.flush();

                    out.writeObject(getPassword());
                    out.flush();

                    ObjectInputStream in = new ObjectInputStream(socket.getInputStream());
                    ArrayList<Integer> matchingClients = (ArrayList<Integer>) in.readObject();
                    setMatchingClients(findMatchingKeys(followers,matchingClients));

                }
                case "Update Social Graph" ->{
                    out.writeInt(getSocialChoice());
                    out.flush();

                    out.writeInt(getFindId());
                    out.flush();

                    out.writeInt(getUpdateId());
                    out.flush();


                }
                case "Notification"-> {
                    out.writeInt(getFindId());
                    out.flush();

                    ObjectInputStream in = new ObjectInputStream(socket.getInputStream());
                    setNotificationList((ArrayList<Notifications>) in.readObject());
                }
                case "Invitation"->{
                    out.writeInt(getFindId());
                    out.flush();

                    out.writeInt(getUpdateId());
                    out.flush();

                    out.writeObject(getNotifications());
                    out.flush();

                    out.writeInt(getSocialChoice());
                    out.flush();
                    if(getSocialChoice()==3 || getSocialChoice()==7){
                        out.writeObject(getPassword());
                        out.flush();
                    }

                }
                case "Photo Request Invitation"->{
                    out.writeInt(getFindId());
                    out.flush();

                    out.writeInt(getUpdateId());
                    out.flush();

                    out.writeObject(getEmail());
                    out.flush();

                }
                case "Photo Request"-> {
                    out.writeInt(1);
                    out.flush();
                    System.out.println("First-hand");

                    ObjectInputStream in = new ObjectInputStream(socket.getInputStream());
                    int anwser=in.readInt();
                    if(anwser==1){
                        System.out.println("Positive Response Continue with the data request");
                        out.writeInt(getFindId());
                        out.flush();

                        out.writeInt(getUpdateId());
                        out.flush();

                        out.writeObject(getEmail());
                        out.flush();
                        //receivePhotoWithStopAndGo(out,in,clientId,socket);
                        receivePhotoWithGoBackN(out,in,clientId,socket);

                        int signal = in.readInt();
                        System.out.println(signal);
                        if (signal == 99) {
                            String textMessage = in.readUTF();
                            System.out.println("Accompanying Text:\n" + textMessage);
                        }

                        int confirm = in.readInt();
                        System.out.println(confirm);
                        if (confirm == 100) {
                            String finalMsg = in.readUTF();
                            System.out.println(finalMsg);
                        }

                        socket.close();
                    }

                }
                case "Comment" -> {
                    out.writeInt(getFindId());
                    out.flush();

                    out.writeInt(getUpdateId());
                    out.flush();

                    out.writeObject(getEmail());
                    out.flush();

                    out.writeObject(getPassword());
                    out.flush();
                }
            }
        } catch (IOException | ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    public void receivePhotoWithStopAndGo(ObjectOutputStream out, ObjectInputStream in, int clientId, Socket socket) {
        try {
            ArrayList<byte[]> receivedChunks = new ArrayList<>();
            boolean[] flags = new boolean[10];
            boolean allTrue = false;
            boolean check_number = false;
            boolean Itentional_Crash = true;

            while (!allTrue || !check_number) {
                int partNumber = in.readInt();
                if (partNumber == -1) {
                    System.out.println("❌ Server aborted transmission due to delivery failure.");
                    return;
                } else if (partNumber == -2) {
                    System.out.println("Part Number: " + partNumber);
                    check_number = true;
                } else {
                    int length = in.readInt();
                    System.out.println("------------------------------------------------");
                    System.out.println("Part Number: " + partNumber + " Length: " + length);
                    byte[] chunk = new byte[length];
                    in.readFully(chunk);

                    System.out.println("Received part " + partNumber + ", size: " + length);

                    if (!flags[partNumber]) {
                        receivedChunks.add(chunk);
                    }
                    flags[partNumber] = true;

                    if (partNumber == 2 && Itentional_Crash) {
                        System.out.println("Client intentionally skips ACK for message 3");
                        Itentional_Crash = false;
                        flags[partNumber] = false;
                        out.writeInt(-3);
                    } else if (partNumber == 5) {
                        System.out.println("Client delays ACK for message 6");
                        Thread.sleep(2000);
                        out.writeInt(partNumber);
                        out.flush();
                        Thread.sleep(500);
                        out.writeInt(partNumber);
                    } else {
                        System.out.println("Normal ACK for message " + partNumber);
                        out.writeInt(partNumber);
                    }
                    out.flush();

                    allTrue = true;
                    for (boolean flag : flags) {
                        if (!flag) {
                            allTrue = false;
                            break;
                        }
                    }
                }
            }

            for (int j = 0; j < flags.length; j++) {
                if (!flags[j]) {
                    System.out.println("❌ Chunk " + j + " was not acknowledged.");
                }
            }

            Path savePath = Paths.get("src/main/java/org/example/DataFolder/Client" + clientId + "Image/" + getEmail() + ".png");
            try (FileOutputStream fos = new FileOutputStream(savePath.toFile())) {
                for (byte[] chunk : receivedChunks) {
                    fos.write(chunk);
                }
                System.out.println("✅ Image saved locally to: " + savePath);
            } catch (IOException e) {
                System.out.println("❌ Error saving image locally: " + e.getMessage());
            }

        } catch (Exception e) {
            System.out.println("⚠️ Error during photo reception: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void receivePhotoWithGoBackN(ObjectOutputStream out, ObjectInputStream in, int clientId, Socket socket){
        ArrayList<byte[]> chunks = new ArrayList<>();
        Set<Integer> receivedOnce = new HashSet<>();
        Map<Integer, Boolean> firstTransmission = new HashMap<>();
        boolean allTrue = false;
        boolean checkNumber=false;
        boolean Intentional_Crash_for_3 = true;
        boolean Intentional_Crash_for_6 = true;
        boolean Intentional_Crash_for_7 = true;
        boolean Intentional_Crash_for_8 = true;
        boolean Intentional_Crash_for_9 = true;
        boolean Intentional_Crash_for_10 = true;
        try {
            while (!allTrue || !checkNumber) {
                int packet_Number=in.readInt();
                if (packet_Number != -2) {
                    int length = in.readInt();
                    byte[] chunk = new byte[length];
                    System.out.println("------------------------------------------------");
                    System.out.println("Part Number: " + packet_Number + " Length: " + length);
                    in.readFully(chunk);
                    if (( (packet_Number+1) % 3 )!= 0 && packet_Number+1 <= 5) {
                        out.writeInt(packet_Number);
                        out.flush();
                        if(!firstTransmission.containsKey(packet_Number)){
                            receivedOnce.add(packet_Number);
                            firstTransmission.put(packet_Number,true);
                        }
                        chunks.add(chunk);
                    }else{
                        switch (packet_Number+1){
                            case 3:
                                if(Intentional_Crash_for_3){
                                    Intentional_Crash_for_3=false;
                                }else{
                                    out.writeInt(packet_Number);
                                    out.flush();
                                    if(!firstTransmission.containsKey(packet_Number)){
                                        receivedOnce.add(packet_Number);
                                        firstTransmission.put(packet_Number,true);
                                    }
                                }
                            case 6:
                                if(Intentional_Crash_for_6){
                                    Intentional_Crash_for_6=false;
                                }else{
                                    out.writeInt(packet_Number);
                                    out.flush();
                                    if(!firstTransmission.containsKey(packet_Number)){
                                        receivedOnce.add(packet_Number);
                                        firstTransmission.put(packet_Number,true);
                                    }
                                }
                            case 7:
                                if(Intentional_Crash_for_7){
                                    Intentional_Crash_for_7=false;
                                }else{
                                    out.writeInt(packet_Number);
                                    out.flush();
                                    if(!firstTransmission.containsKey(packet_Number)){
                                        receivedOnce.add(packet_Number);
                                        firstTransmission.put(packet_Number,true);
                                    }
                                }
                            case 8:
                                if(Intentional_Crash_for_8){
                                    Intentional_Crash_for_8=false;
                                }else{
                                    out.writeInt(packet_Number);
                                    out.flush();
                                    if(!firstTransmission.containsKey(packet_Number)){
                                        receivedOnce.add(packet_Number);
                                        firstTransmission.put(packet_Number,true);
                                    }
                                }
                            case 9:
                                if(Intentional_Crash_for_9){
                                    Intentional_Crash_for_9=false;
                                }else{
                                    out.writeInt(packet_Number);
                                    out.flush();
                                    if(!firstTransmission.containsKey(packet_Number)){
                                        receivedOnce.add(packet_Number);
                                        firstTransmission.put(packet_Number,true);
                                    }
                                }
                            case 10:
                                if(Intentional_Crash_for_10){
                                    Intentional_Crash_for_10=false;
                                }else{
                                    out.writeInt(packet_Number);
                                    out.flush();
                                    if(!firstTransmission.containsKey(packet_Number)){
                                        receivedOnce.add(packet_Number);
                                        firstTransmission.put(packet_Number,true);
                                    }
                                }
                        }
                    }
                }else{
                    checkNumber=true;
                }
                if(receivedOnce.size()==10){
                    allTrue = true;
                }
            }
            Path savePath = Paths.get("src/main/java/org/example/DataFolder/Client" + clientId + "Image/" + getEmail() + ".png");
            try (FileOutputStream fos = new FileOutputStream(savePath.toFile())) {
                for (byte[] chunk : chunks) {
                    fos.write(chunk);
                }
                System.out.println("✅ Image saved locally to: " + savePath);
            } catch (IOException e) {
                System.out.println("❌ Error saving image locally: " + e.getMessage());
            }


        }catch (IOException e) {
            e.printStackTrace();
        }
    }

    private FileInputStream getFileInputStream(Path imagePath, Socket socket) throws IOException {
        File imageFile = imagePath.toFile();

        DataOutputStream dos = new DataOutputStream(socket.getOutputStream());

        dos.writeInt(getFindId());
        dos.writeUTF(imageFile.getName());
        dos.writeLong(imageFile.length());

        FileInputStream fis = new FileInputStream(imageFile);
        byte[] buffer = new byte[4096];
        int bytesRead;
        while ((bytesRead = fis.read(buffer)) != -1) {
            dos.write(buffer, 0, bytesRead);
        }
        return fis;
    }

    public int getReceivedId() {
        return receivedId;
    }
    public ArrayList<Integer> getMatchingClients() {
        return MatchingClients;
    }
    public ArrayList<Integer> findMatchingKeys(ArrayList<Integer> following, ArrayList<Integer> IdOfClients) {
        ArrayList<Integer> matches = new ArrayList<>();
        for (Integer key : following) {
            if (IdOfClients.contains(key)) {
                matches.add(key);
            }
        }
        return matches;
    }

    private static void startFollowedPostListener(ObjectInputStream in) {
        new Thread(() -> {
            try {
                while (true) {
                    String type =(String) in.readObject();
                    if ("NewFollowedPost".equals(type)) {
                        String post = (String) in.readObject();
                        Path filePath = Paths.get("src/main/java/org/example/DataFolder/Others_Xclient" + clientId + ".txt");
                        Files.write(filePath, List.of("---------", post, "---------"), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                    }
                }
            } catch (Exception e) {
                System.out.println("❌ Notification listener stopped: " + e.getMessage());
            }
        }).start();
    }

}
