package org.example;

import java.io.*;
import java.net.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Scanner;

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

    public static void main(String[] args) throws InterruptedException {
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
            email = sc.nextLine();
            System.out.println("Password");
            password = sc.nextLine();
            Client client = new Client(menu_choice);
            client.setEmail(email);
            client.setPassword(password);
            client.start();
            client.join();
            clientId = client.getReceivedId();
        } while (clientId == -1);

        createFiles(clientId);

        do {
            System.out.println(
                    "Please choose one of the following:\n1)Post \n2)Access Profile\n3)Search Photo\n4)Update Social Graph\n5)Notification\n6)Exit"
            );
            menu_choice = sc.nextInt();
            int number = 10 + menu_choice;
            Client client = new Client(number);
            System.out.println("-----------------------------------------");

            if (menu_choice == 1) {
                System.out.println("Enter the path of the photograph:");
                sc.nextLine();
                String photoPath = sc.nextLine();

                Path destPath = Paths.get("src/main/java/org/example/DataFolder/Client" + clientId + "Image");
                client.setImagePath(Post(clientId,destPath,photoPath));
                client.setFindId(clientId);
                client.start();
                client.join();
            } else if (menu_choice == 2) {
                System.out.println("The available profiles are: ");
                for (int i = 0; i < following.size(); i++) {
                    System.out.print(following.get(i)+"|");
                }
                System.out.println("Please enter the Id:");
                client.setFindId(sc.nextInt());
                client.setUpdateId(clientId);
                client.start();
                client.join();
            } else if (menu_choice == 3) {
                System.out.println("Enter the image's name:");
                sc.nextLine();
                String imagesName = sc.nextLine();

                client.setEmail(imagesName);
                client.start();
                client.join();

                ArrayList<Integer> match=client.getMatchingClients();
                System.out.println("-----");
                if(match.isEmpty()){
                    System.out.println("No image was found with the name \""+imagesName+"\"");
                }else{
                    int i=0;
                    for(Integer item: match){
                        System.out.println(match);
                    }
                    System.out.println("-----");
                    System.out.println("Choose one:");

                    int SenderID=sc.nextInt();
                    Client client1=new Client(17);
                    client1.setFindId(clientId);
                    client1.setUpdateId(SenderID);
                    client1.setEmail(imagesName);
                    client1.start();
                    client1.join();

                    System.out.println("Enter a comment to include with the repost:");
                    sc.nextLine(); // clear the buffer after nextInt
                    String comment = sc.nextLine();

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

            } else if (menu_choice == 4) {
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
                client.setFindId(clientId);
                client.start();
                client.join();
                PrintNotifications();
                if(!getNotificationList().isEmpty()){
                    System.out.println("Enter the Notification you want to see:");
                    int index=sc.nextInt();
                    int NotificationsType=getNotificationList().get(index).getType();

                    Client client1= new Client(16);
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

                    }else{
                        client1.setSocialChoice(0);
                        if(NotificationsType==2){
                            following.add(getNotificationList().get(index).getId());
                        }
                    }
                    client1.start();
                    client1.join();
                }
            }
        } while (menu_choice != 6);
        sc.close();
    }

    private int number;
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

    public Client(int number) {
        this.number = number;
    }


    @Override
    public void run() {
        Socket socket = null;
        ObjectOutputStream out = null;

        try {
            socket = new Socket("localhost", 1234);
            out = new ObjectOutputStream(socket.getOutputStream());

            out.writeInt(number);
            out.flush();
            if (number == 0 || number == 1) {
                out.writeObject(getEmail());
                out.flush();

                out.writeObject(getPassword());
                out.flush();

                ObjectInputStream in = new ObjectInputStream(socket.getInputStream());
                receivedId = in.readInt();
                if (receivedId!=-1){
                    followers=(ArrayList<Integer>)in.readObject();
                    following=(ArrayList<Integer>)in.readObject();
                }
            } else if (number == 11) {
                Path imagePath = getImagepath();
                if (imagePath != null) {
                    FileInputStream fis = getFileInputStream(imagePath, socket);
                    fis.close();
                } else {
                    System.out.println("Image path is null!");
                }
            } else if (number == 12) {
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

            } else if (number == 13) {
                out.writeObject(getEmail()); //Using The Email Variable to save storage
                out.flush();

                ObjectInputStream in = new ObjectInputStream(socket.getInputStream());
                ArrayList<Integer> matchingClients = (ArrayList<Integer>) in.readObject();
                setMatchingClients(matchingClients);
                findMatchingKeys(followers,matchingClients);

            } else if (number == 14) {
                out.writeInt(getSocialChoice());
                out.flush();

                out.writeInt(getFindId());
                out.flush();

                out.writeInt(getUpdateId());
                out.flush();


            }else if(number == 15){
                out.writeInt(getFindId());
                out.flush();

                ObjectInputStream in = new ObjectInputStream(socket.getInputStream());
                setNotificationList((ArrayList<Notifications>) in.readObject());

            } else if (number==16) {
                out.writeInt(getFindId());
                out.flush();

                out.writeInt(getUpdateId());
                out.flush();

                out.writeObject(getNotifications());
                out.flush();

                out.writeInt(getSocialChoice());
                out.flush();

            } else if (number==17) {
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

                    out.writeObject(getEmail()); //Using The Email Variable to save storage
                    out.flush();

                    ArrayList<byte[]> receivedChunks = new ArrayList<>();
                    boolean[] flags = new boolean[10];
                    boolean allTrue = true;
                    for (int i = 0; i < flags.length; i++) {
                        if (!flags[i]) {
                            allTrue = false;
                        }
                    }
                    boolean check_number=false;
                    boolean Itetional_Crash=true;
                    while(!allTrue || !check_number) {
                        int partNumber = in.readInt();
                         if(partNumber == -1){
                            System.out.println("❌ Server aborted transmission due to delivery failure.");
                            return;
                         }else if(partNumber == -2){
                             System.out.println("Part Number:"+ partNumber);
                             check_number=true;

                         }else{
                             int length = in.readInt();
                             System.out.println("------------------------------------------------");
                             System.out.println("Part Number:"+ partNumber + " Length:"+ length);
                             byte[] chunk = new byte[length];

                             System.out.println("Received part " + partNumber + ", size: " + length);

                             in.readFully(chunk);
                             if(!flags[partNumber]){
                                 receivedChunks.add(chunk);
                             }
                             flags[partNumber]=true;
                             if (partNumber == 2 && Itetional_Crash) {
                                 System.out.println("Client intentionally skips ACK for message 3");// Simulate long delay
                                 Itetional_Crash=false;

                                 flags[partNumber]=false;

                                 out.writeInt(-3);
                                 out.flush();
                             }else if (partNumber == 5) {
                                 System.out.println("Client delays ACK for message 6");
                                 Thread.sleep(2000); // Simulate long delay

                                 out.writeInt(partNumber);
                                 out.flush();

                                 Thread.sleep(500); // Duplicate ACK simulation

                                 out.writeInt(partNumber);
                                 out.flush();
                             }else{
                                 System.out.println("Normal ACK for message " + partNumber);
                                 out.writeInt(partNumber); // Normal ACK
                                 out.flush();
                             }


                             allTrue = true;
                             for (int j = 0; j < flags.length; j++) {
                                 if (!flags[j]) {
                                     allTrue = false;
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
                    try {
                        socket.close();
                    } catch (IOException e) {
                        System.out.println("⚠️ Error closing socket: " + e.getMessage());
                    }
                }

            }
        } catch (IOException | ClassNotFoundException e) {
            throw new RuntimeException(e);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
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
}
