package com.example.MedTracker.serviceBot;

import com.example.MedTracker.model.*;
import org.springframework.stereotype.Service;

@Service
public class ReminderService {
    private final ReminderTimeRepository reminderTimeRepository;
    private final UserRepository userRepository;

    public ReminderService(ReminderTimeRepository reminderTimeRepository, UserRepository userRepository) {
        this.reminderTimeRepository = reminderTimeRepository;
        this.userRepository = userRepository;
    }


}