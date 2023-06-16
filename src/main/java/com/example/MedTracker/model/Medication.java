package com.example.MedTracker.model;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

/*бд для лекарств*/
@Getter
@Setter
@Entity(name = "MedicationDataTable")
public class Medication {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String medicineName;
    private String description;
    private LocalDate startDate;
    private Timestamp todayDate;
    private int daysForEndDate;
    private int daysBetweenDoses;
    private Boolean isRepeated;
    private int dosesForDay;


    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    //Todo
    @OneToMany(fetch = FetchType.EAGER, mappedBy = "medication", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ReminderTime> reminderTimes = new ArrayList<>();


    public void addReminderTime(ReminderTime reminderTime) {
        reminderTimes.add(reminderTime);
        reminderTime.setMedication(this);
    }

    public void removeReminderTime(ReminderTime reminderTime) {
        reminderTimes.remove(reminderTime);
        reminderTime.setMedication(null);
    }


    @Override
    public String toString() {
        return "Medication{" +
                "id=" + id +
                ", medicineName='" + medicineName + '\'' +
                ", description='" + description + '\'' +
                ", startDate=" + startDate +
                ", todayDate=" + todayDate +
                ", daysForEndDate=" + daysForEndDate +
                ", daysBetweenDoses=" + daysBetweenDoses +
                ", isRepeated=" + isRepeated +
                ", dosesForDay=" + dosesForDay +
                //", user=" + user +
                //", reminderTimes=" + reminderTimes +
                '}';
    }
}
