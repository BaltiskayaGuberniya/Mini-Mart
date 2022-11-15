package com.example.application.data.service;

import com.example.application.data.entity.ShopGoods;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
public class ShopGoodsService {

    private final ShopGoodsRepository repository;

    @Autowired
    public ShopGoodsService(ShopGoodsRepository repository) {
        this.repository = repository;
    }

    public Optional<ShopGoods> get(UUID id) {
        return repository.findById(id);
    }

    public ShopGoods update(ShopGoods entity) {
        return repository.save(entity);
    }

    public void delete(UUID id) {
        repository.deleteById(id);
    }

    public Page<ShopGoods> list(Pageable pageable) {
        return repository.findAll(pageable);
    }

    public int count() {
        return (int) repository.count();
    }

}
