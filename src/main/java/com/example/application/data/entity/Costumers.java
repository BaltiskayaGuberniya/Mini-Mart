package com.example.application.data.entity;

import java.time.LocalDate;
import javax.persistence.Entity;
import javax.validation.constraints.Email;

@Entity
public class Costumers extends AbstractEntity {

    private String name;
    @Email
    private String email;
    private String phone;
    private LocalDate lastPurchase;
    private boolean hasMembershipCard;

    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }
    public String getEmail() {
        return email;
    }
    public void setEmail(String email) {
        this.email = email;
    }
    public String getPhone() {
        return phone;
    }
    public void setPhone(String phone) {
        this.phone = phone;
    }
    public LocalDate getLastPurchase() {
        return lastPurchase;
    }
    public void setLastPurchase(LocalDate lastPurchase) {
        this.lastPurchase = lastPurchase;
    }
    public boolean isHasMembershipCard() {
        return hasMembershipCard;
    }
    public void setHasMembershipCard(boolean hasMembershipCard) {
        this.hasMembershipCard = hasMembershipCard;
    }

}
