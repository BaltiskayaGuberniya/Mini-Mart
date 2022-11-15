package com.example.application.data.service;

import com.example.application.data.entity.Costumers;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
public class CostumersService {

    private final CostumersRepository repository;

    @Autowired
    public CostumersService(CostumersRepository repository) {
        this.repository = repository;
    }

    public Optional<Costumers> get(UUID id) {
        return repository.findById(id);
    }

    public Costumers update(Costumers entity) {
        return repository.save(entity);
    }

    public void delete(UUID id) {
        repository.deleteById(id);
    }

    public Page<Costumers> list(Pageable pageable) {
        return repository.findAll(pageable);
    }

    public int count() {
        return (int) repository.count();
    }

}
