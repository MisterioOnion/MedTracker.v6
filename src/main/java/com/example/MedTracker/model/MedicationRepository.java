package com.example.MedTracker.model;

import org.springframework.data.jpa.repository.JpaRepository;


public interface MedicationRepository extends JpaRepository<Medication, Long> {


}
