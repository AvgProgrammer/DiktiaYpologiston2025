package org.example;

import java.io.Serializable;

public class Notifications implements Serializable{
    private int id;
    private String Title;
    private int ClientId;
    private String Message;
    private int type; /*The Number '0' for a post notification
                        The Number '1' for a folow request
                        The Number '2' for a follow request accepted
                        The number '3' for file
                        The number '4' for a comment
                        The number '5' for a comment accept
                        The number '7' for a photo request
                        The number '8' for a photo accept*/
    public Notifications(String title, int clientId, int type,int id) {
        this.Title = title;
        this.ClientId = clientId;
        this.type = type;
        this.id = id;
        this.Message="";
    }

    public String getTitle() {
        return Title;
    }

    public int getClientId() {
        return ClientId;
    }

    public int getType() {
        return type;
    }

    public String PrintNotification(){
        return Title;
    }

    public int getId() {
        return id;
    }

    public String getMessage() {
        return Message;
    }
    public void setMessage(String message) {
        this.Message = message;
    }

}
