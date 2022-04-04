package com.jonty.garagenetwork.Model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class Seller implements Serializable {
    private String name,country,state,address,district;
    private int pinCode,price,height,width;
    private List<Long> phoneNumber;
    //extras
    int type; // 0 for rent, 1 for sell
    private List<String> imageLinks;
    private boolean sold;
    private String key,authKey;

    public Seller(){
        this.phoneNumber=new ArrayList<>();
        this.imageLinks=new ArrayList<>();
        sold=false;
    }

    public Seller(String name,String country,String state,String address,int pinCode,int price,int height,int width,long phoneNumber,
                  int type){
        this.name=name;
        this.country=country;
        this.state=state;
        this.address=address;
        this.pinCode=pinCode;
        this.price=price;
        this.height=height;
        this.width=width;
        this.phoneNumber=new ArrayList<>();
        this.imageLinks=new ArrayList<>();
        this.sold=false;
        this.type=type;
        addPhone(phoneNumber);
        key="";
    }

    public String getAuthKey() {
        return authKey;
    }

    public void setAuthKey(String authKey) {
        this.authKey = authKey;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getDistrict() {
        return district;
    }

    public void setDistrict(String district) {
        this.district = district;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public boolean isSold() {
        return sold;
    }

    public void setSold(boolean sold) {
        this.sold = sold;
    }

    public List<String> getImageLinks() {
        return imageLinks;
    }

    public void setImageLinks(List<String> imageLinks) {
        this.imageLinks = imageLinks;
    }

    public void addImage(String imageLink){
        this.imageLinks.add(imageLink);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public int getPinCode() {
        return pinCode;
    }

    public void setPinCode(int pinCode) {
        this.pinCode = pinCode;
    }

    public int getPrice() {
        return price;
    }

    public void setPrice(int price) {
        this.price = price;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public List<Long> getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(List<Long> phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public void addPhone(long phoneNumber){
        this.phoneNumber.add(phoneNumber);
    }

    public boolean search(String search){
        if(name.contains(search) || country.contains(search) || state.contains(search) || district.contains(search) || address.contains(search))
            return true;
        return false;
    }
}
