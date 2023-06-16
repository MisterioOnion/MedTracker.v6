package com.example.MedTracker.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalTime;

@Getter
@Setter
@Entity(name = "TimeDataTable")
public class ReminderTime {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "medication_id")
    private Medication medication;

    private LocalTime time;

    @Override
    public String toString() {
        return "ReminderTime{" +
                //"id=" + id +
                //", medication=" + medication +
                ", time='" + time + '\'' +
                '}';
    }
}
