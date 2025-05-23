package org.example;

import java.io.Serializable;

public class Notifications implements Serializable{
    private int id;
    private String Title;
    private int ClientId;
    private int type; /*The Number '0' for a post notification
                        The Number '1' for a request
                        The Number '2' for a request accepted
                        The number '3' for file */
    public Notifications(String title, int clientId, int type,int id) {
        this.Title = title;
        this.ClientId = clientId;
        this.type = type;
        this.id = id;
    }

    public String getTitle() {
        return Title;
    }

    public void setTitle(String title) {
        Title = title;
    }

    public int getClientId() {
        return ClientId;
    }

    public void setClientId(int clientId) {
        ClientId = clientId;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }
    public String PrintNotification(){
        return Title;
    }
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }
}
