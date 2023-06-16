package com.example.MedTracker.model;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
@Repository
public interface ReminderTimeRepository extends CrudRepository<ReminderTime, Long> {
    List<ReminderTime> findByMedication_User(User user);

}
