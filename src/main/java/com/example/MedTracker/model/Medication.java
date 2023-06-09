package com.example.MedTracker.model;

import java.sql.Timestamp;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/*бд для лекарств*/
@Getter
@Setter
@Entity(name = "MedicationDataTable")
public class Medication {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    //private Long userId;
    private String medicineName;
    private String description;
    private Timestamp startDate;
    private int daysForEndDate;
    private int daysBetweenDoses;
    private Boolean isRepeated;
    private int dosesForDay;


    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    @Override
    public String toString() {
        return "Medication{" +
                "id=" + id +
                ", medicineName='" + medicineName + '\'' +
                ", description='" + description + '\'' +
                ", startDate=" + startDate +
                ", daysForEndDate=" + daysForEndDate +
                ", daysBetweenDoses=" + daysBetweenDoses +
                ", isRepeated=" + isRepeated +
                ", dosesForDay=" + dosesForDay +
                ", user=" + user +
                '}';
    }
}
