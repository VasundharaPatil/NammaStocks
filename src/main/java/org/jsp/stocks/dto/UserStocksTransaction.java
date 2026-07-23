package org.jsp.stocks.dto;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

@Entity
public class UserStocksTransaction {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;
    private String stock_ticker;
    private double quantity;
    private double price;
    
    // Default constructor
    public UserStocksTransaction() {}
    
    // Getters
    public int getId() {
        return id;
    }
    
    public String getStock_ticker() {
        return stock_ticker;
    }
    
    public double getQuantity() {
        return quantity;
    }
    
    public double getPrice() {
        return price;
    }
    
    // Setters
    public void setId(int id) {
        this.id = id;
    }
    
    public void setStock_ticker(String stock_ticker) {
        this.stock_ticker = stock_ticker;
    }
    
    public void setQuantity(double quantity) {
        this.quantity = quantity;
    }
    
    public void setPrice(double price) {
        this.price = price;
    }
}