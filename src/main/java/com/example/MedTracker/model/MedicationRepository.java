package com.example.MedTracker.model;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MedicationRepository extends CrudRepository<Medication, Long> {
    List<Medication> findByUser(User user);
    Medication findByMedicineNameAndUser(String medicationName, User user);
}
