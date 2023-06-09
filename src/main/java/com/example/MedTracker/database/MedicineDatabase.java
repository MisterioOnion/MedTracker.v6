package com.example.MedTracker.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Scanner;

public class MedicineDatabase {
    private static final String DB_URL = "jdbc:postgresql://localhost:5432/your_database_name";
    private static final String DB_USER = "your_username";
    private static final String DB_PASSWORD = "your_password";

    public static void main(String[] args) {
        try (Scanner scanner = new Scanner(System.in)) {
            // Получение информации о лекарстве от пользователя
            System.out.print("Введите название лекарства: ");
            String name = scanner.nextLine();

            System.out.print("Введите описание лекарства: ");
            String description = scanner.nextLine();

            System.out.print("Введите день начала приема лекарства: ");
            int startDay = scanner.nextInt();

            System.out.print("Введите день окончания приема лекарства: ");
            int endDay = scanner.nextInt();

            System.out.print("Введите количество дней между повторным применением: ");
            int intervalDays = scanner.nextInt();

            System.out.print("Введите количество примений лекарства в течение дня: ");
            int dosageCount = scanner.nextInt();

            // Подключение к базе данных PostgreSQL
            try (Connection connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {
                // Создание SQL-запроса для добавления информации о лекарстве
                String insertQuery = "INSERT INTO medicines (name, description, start_day, end_day, interval_days, dosage_count) " +
                        "VALUES (?, ?, ?, ?, ?, ?)";

                // Создание подготовленного выражения для выполнения SQL-запроса
                try (PreparedStatement statement = connection.prepareStatement(insertQuery)) {
                    // Установка параметров в подготовленном выражении
                    statement.setString(1, name);
                    statement.setString(2, description);
                    statement.setInt(3, startDay);
                    statement.setInt(4, endDay);
                    statement.setInt(5, intervalDays);
                    statement.setInt(6, dosageCount);

                    // Выполнение SQL-запроса
                    int rowsAffected = statement.executeUpdate();

                    if (rowsAffected > 0) {
                        System.out.println("Информация о лекарстве успешно добавлена в базу данных.");
                    } else {
                        System.out.println("Ошибка при добавлении информации о лекарстве в базу данных.");
                    }
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }
}

