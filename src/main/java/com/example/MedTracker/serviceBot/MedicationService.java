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







    public MedicationService() {
        // Инициализация подключения к базе данных
        try {
            connection = DriverManager.getConnection("jdbc:mysql://localhost:3306/your_database_name", "your_username", "your_password");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void addMedication(String name, String description, String startDate, int duration, boolean isRepeated, int frequency) {
        // Создание SQL-запроса для вставки новой записи в базу данных
        String query = "INSERT INTO medications (name, description, start_date, duration, is_repeated, frequency) VALUES (?, ?, ?, ?, ?, ?)";

        try {
            PreparedStatement statement = connection.prepareStatement(query);
            statement.setString(1, name);
            statement.setString(2, description);
            statement.setString(3, startDate);
            statement.setInt(4, duration);
            statement.setBoolean(5, isRepeated);
            statement.setInt(6, frequency);

            // Выполнение SQL-запроса
            statement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }


}






