package com.example.MedTracker.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.Data;
import org.glassfish.grizzly.http.util.TimeStamp;

import java.sql.Timestamp;

@Data
@Entity(name = "usersDataTable")
public class User {

    @Id
    private Long chatId;
    private String firstName;
    private String lastName;
    private String userName;
    private Timestamp registeredAt;
    private boolean registration;

    public boolean isRegistration() {
        return registration;
    }

    //public Long getChatId() {
    //    return chatId;
    //}

    //public void setChatId(Long chatId) {
    //    this.chatId = chatId;
    //}

    //public String getFirstName() {
    //    return firstName;
    //}

    //public void setFirstName(String firstName) {
    //    this.firstName = firstName;
    //}

    //public String getLastName() {
    //    return lastName;
    //}

    //public void setLastName(String lastName) {
    //    this.lastName = lastName;
    //}

    //public String getUserName() {
    //    return userName;
    //}

    //public void setUserName(String userName) {
    //    this.userName = userName;
    //}

    //public Timestamp getRegisteredAt() {
    //    return registeredAt;
    //}

   //public void setRegisteredAt(Timestamp registeredAt) {
   //    this.registeredAt = registeredAt;
   //}

    @Override
    public String toString() {
        return "User{" +
                "chatId=" + chatId +
                ", firstName='" + firstName + '\'' +
                ", lastName='" + lastName + '\'' +
                ", userName='" + userName + '\'' +
                ", registeredAt=" + registeredAt +
                ", registration=" + registration +
                '}';
    }


}
