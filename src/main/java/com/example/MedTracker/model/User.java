package com.example.MedTracker.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.sql.Timestamp;
import java.util.List;

/*бд для пользователей*/
@Getter
@Setter
@Entity(name = "usersDataTable")
public class User {

    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
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

    @OneToMany(fetch = FetchType.EAGER, mappedBy = "user", cascade = CascadeType.ALL)
    private List<Medication> medications;

    @Override
    public String toString() {
        return "User{" +
                "id=" + id +
                ", chatId=" + chatId +
                ", firstName='" + firstName + '\'' +
                ", lastName='" + lastName + '\'' +
                ", userName='" + userName + '\'' +
                ", registeredAt=" + registeredAt +
                ", registration=" + registration +
                ", medications=" + medications +
                '}';
    }
}
