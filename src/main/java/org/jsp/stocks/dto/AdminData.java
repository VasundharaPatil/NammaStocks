package org.jsp.stocks.dto;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

@Entity
public class AdminData {
    @Id
    private int id;
    private double platformFeePercentage;
    private double totalPlatformFee;
    private double totalStocksBought;
    private double totalStocksSold;
    private double totalTransaction;
    
    public AdminData() {}
    
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    
    public double getPlatformFeePercentage() { return platformFeePercentage; }
    public void setPlatformFeePercentage(double platformFeePercentage) { this.platformFeePercentage = platformFeePercentage; }
    
    public double getTotalPlatformFee() { return totalPlatformFee; }
    public void setTotalPlatformFee(double totalPlatformFee) { this.totalPlatformFee = totalPlatformFee; }
    
    public double getTotalStocksBought() { return totalStocksBought; }
    public void setTotalStocksBought(double totalStocksBought) { this.totalStocksBought = totalStocksBought; }
    
    public double getTotalStocksSold() { return totalStocksSold; }
    public void setTotalStocksSold(double totalStocksSold) { this.totalStocksSold = totalStocksSold; }
    
    public double getTotalTransaction() { return totalTransaction; }
    public void setTotalTransaction(double totalTransaction) { this.totalTransaction = totalTransaction; }
}