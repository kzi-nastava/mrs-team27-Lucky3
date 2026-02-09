package com.example.mobile.models;

public class ProfileUserResponse {
    private String name;
    private String surname;
    private String email;
    private String phoneNumber;
    private String address;
    private String imageUrl;

    public ProfileUserResponse(){

    }
    public ProfileUserResponse(String name, String surname, String email, String phoneNumber, String address, String imageUrl){
        this.name = name;
        this.surname = surname;
        this.email = email;
        this.phoneNumber = phoneNumber;
        this.imageUrl = imageUrl;
        this.address = address;
    }
    //getters
    public String getSurname(){
        return surname;
    }
    public String getName(){
        return name;
    }
    public String getImageUrl(){
        return imageUrl;
    }
    public String getEmail(){
        return email;
    }
    public String getAddress(){
        return address;
    }
    public String getPhoneNumber(){
        return phoneNumber;
    }
    //setters
    public void setName(String name){
        this.name = name;
    }
    public void setSurname(String surname){
        this.surname = surname;
    }
    public void setEmail(String email){
        this.email = email;
    }
    public void setAddress(String address){
        this.address = address;
    }
    public void setPhoneNumber(String phoneNumber){
        this.phoneNumber = phoneNumber;
    }
    public void setImageUrl(String imageUrl){
        this.imageUrl = imageUrl;
    }

}
