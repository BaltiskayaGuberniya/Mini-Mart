package com.example.application.data.service;

import com.example.application.data.entity.Costumers;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CostumersRepository extends JpaRepository<Costumers, UUID> {

}