package org.example;

import java.util.ArrayList;

public class Post {
    public String ImageName;
    public String Post_Language;
    public String Owner;
    public int Id;
    public int Occupied;
    public int NumberOfRequests;
    public int UserWithAccess;
    public ArrayList<Integer> ListOfClients;

    public Post(String imageName, String post_Language, String Owner,int Id) {
        this.ImageName = imageName;
        this.Post_Language = post_Language;
        this.Owner = Owner;
        this.Id = Id;
        this.Occupied = 0;
        this.UserWithAccess = 0;
        this.ListOfClients = new ArrayList<>();
        this.NumberOfRequests = 0;
    }
    public Post(){};

    public String getImageName() {
        return ImageName;
    }

    public String getOnwer() {return Owner;}

    public String getPost_Language() {
        return Post_Language;
    }

    public int getOccupied() {
        return Occupied;
    }

    public void setOccupied(int occupied) {
        Occupied = occupied;
    }

    public int getUserWithAccess() {
        return UserWithAccess;
    }

    public void addUserWithAccess() {
        UserWithAccess ++;
    }

    public ArrayList<Integer> getListOfClients() {return ListOfClients;}

    public void addToListOfClient(int clientId){ListOfClients.add(clientId);}

    public int getNumberOfRequests() {return NumberOfRequests;}

    public void addNumberOfRequests() {NumberOfRequests++;}

}
