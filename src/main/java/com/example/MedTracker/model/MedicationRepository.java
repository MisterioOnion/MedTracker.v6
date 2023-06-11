package com.example.MedTracker.model;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.CrudRepository;

import java.util.List;


public interface MedicationRepository extends CrudRepository<Medication, Long> {
    List<Medication> findByUser(User user);
}
