package com.example.MedTracker.serviceBot;


import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import com.example.MedTracker.model.Medication;
import com.example.MedTracker.model.MedicationRepository;
import com.example.MedTracker.model.User;
import com.example.MedTracker.model.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;


@Slf4j
@Service
public class MedicationService {
    private UserRepository userRepository;
    private MedicationRepository medicationRepository;
    public Connection connection;



    @Autowired
    public MedicationService(UserRepository userRepository, MedicationRepository medicationRepository) {
        this.userRepository = userRepository;
        this.medicationRepository = medicationRepository;
    }
}






