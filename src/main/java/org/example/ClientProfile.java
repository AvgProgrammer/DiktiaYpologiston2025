package org.example;

import java.util.ArrayList;

public class ClientProfile {
    public int id;
    public String name;
    public String email;
    public String password;
    public ArrayList<Integer> Followers;
    public ArrayList<Integer> Following;
    public ArrayList<Post> Posts;

    public ClientProfile(int id, String name, String email, String password) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.password = password;
        this.Followers = new ArrayList<>();
        this.Following = new ArrayList<>();
        this.Posts = new ArrayList<>();
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

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

    public ArrayList<Integer> getFollowers() {
        return Followers;
    }

    public void setFollowers(ArrayList<Integer> followers) {
        Followers = followers;
    }

    public void removeFollower(int id) {
        Followers.remove(id);
    }
    public void addFollower(int id) {
        Followers.add(id);
    }
    public ArrayList<Integer> getFollowing() {
        return Following;
    }

    public void setFollowing(ArrayList<Integer> following) {
        Following = following;
    }

    public ArrayList<Post> getPosts() {
        return Posts;
    }

    public void addToPosts(Post post){
        this.Posts.add(post);
    }
}
