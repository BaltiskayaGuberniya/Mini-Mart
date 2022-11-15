package com.example.application.data.service;

import com.example.application.data.entity.ShopGoods;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ShopGoodsRepository extends JpaRepository<ShopGoods, UUID> {

}