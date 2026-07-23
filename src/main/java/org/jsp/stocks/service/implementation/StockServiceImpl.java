package org.jsp.stocks.service.implementation;

import java.text.DecimalFormat;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;

import org.jsp.stocks.dto.AdminData;
import org.jsp.stocks.dto.Stock;
import org.jsp.stocks.dto.User;
import org.jsp.stocks.dto.UserStocksTransaction;
import org.jsp.stocks.repository.AdminDataRepository;
import org.jsp.stocks.repository.StockRepository;
import org.jsp.stocks.repository.UserRepository;
import org.jsp.stocks.service.StockService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;

import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpSession;

@Service
public class StockServiceImpl implements StockService {

    DecimalFormat format = new DecimalFormat("#0.00");

    @Autowired
    StockRepository stockRepository;

    @Autowired
    UserRepository userRepository;
    
    @Autowired
    JavaMailSender mailSender;
    
    @Autowired
    AdminDataRepository dataRepository;

    int generateOtp() {
        return new Random().nextInt(900000) + 100000;
    }
    
    public void sendOtpEmail(String toEmail, int otp) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(toEmail);
            message.setSubject("NammaStocks - OTP Verification");
            message.setText("Hello,\n\nYour OTP for account verification is: " + otp + "\n\nThank you,\nNammaStocks Team");
            mailSender.send(message);
            System.out.println("✅ Email sent to: " + toEmail);
        } catch (Exception e) {
            System.err.println("❌ Failed to send email: " + e.getMessage());
        }
    }
    
    @PostConstruct
    public void initAdminData() {
        if (dataRepository.findById(1).isEmpty()) {
            AdminData adminData = new AdminData();
            adminData.setId(1);
            adminData.setPlatformFeePercentage(0.04);
            adminData.setTotalPlatformFee(0.0);
            adminData.setTotalStocksBought(0.0);
            adminData.setTotalStocksSold(0.0);
            adminData.setTotalTransaction(0.0);
            dataRepository.save(adminData);
            System.out.println("✅ Default AdminData created with ID 1");
        }
    }

    @Override
    public String register(User user, Model model) {
        model.addAttribute("user", user);
        return "register.html";
    }

    @Override
    public String register(User user, BindingResult result, HttpSession session) {

        if (user.getPassword() == null || user.getConfirmPassword() == null ||
                !user.getPassword().equals(user.getConfirmPassword())) {
            result.rejectValue("confirmPassword", "error", "Password mismatch");
            return "register.html";
        }

        if (user.getDob() != null) {
            if (LocalDate.now().getYear() - user.getDob().getYear() < 18) {
                result.rejectValue("dob", "error", "Must be 18+");
                return "register.html";
            }
        }

        if (userRepository.existsByEmail(user.getEmail())) {
            result.rejectValue("email", "error", "Email already exists");
            return "register.html";
        }

        if (userRepository.existsByMobile(user.getMobile())) {
            result.rejectValue("mobile", "error", "Mobile number already exists");
            return "register.html";
        }

        if (result.hasErrors()) {
            return "register.html";
        }

        user.setOtp(generateOtp());
        user.setPassword(user.getPassword());
        user.setVerified(false);
        user.setAmount(0.0);
        userRepository.save(user);

        System.out.println("=================================");
        System.out.println("OTP for " + user.getEmail() + " is: " + user.getOtp());
        System.out.println("=================================");

        sendOtpEmail(user.getEmail(), user.getOtp());

        session.setAttribute("pass", "OTP sent to your email");
        return "redirect:/otp/" + user.getId();
    }

    @Override
    public String verifyOtp(int id, int otp, HttpSession session) {

        Optional<User> optionalUser = userRepository.findById(id);

        if (optionalUser.isEmpty()) {
            session.setAttribute("fail", "User not found");
            return "redirect:/register";
        }

        User user = optionalUser.get();

        if (user.getOtp() == otp) {
            user.setVerified(true);
            user.setOtp(0);
            userRepository.save(user);

            session.setAttribute("pass", "Account created successfully!");
            return "redirect:/login";
        } else {
            session.setAttribute("fail", "Invalid OTP");
            return "redirect:/otp/" + id;
        }
    }
    
    public void removeMessage() {
        // Called by Thymeleaf templates
    }

    @Override
    public String login(String email, String password, HttpSession session) {
        System.out.println("=== LOGIN ATTEMPT ===");
        System.out.println("Email: " + email);
        
        session.removeAttribute("user");
        session.removeAttribute("admin");
        
        // Admin login
        if (email.equals("admin@gmail.com") && password.equals("admin123")) {
            System.out.println("✅ Admin login successful");
            session.setAttribute("admin", "admin");
            session.setAttribute("pass", "Login Success - Welcome Admin");
            return "redirect:/";
        }
        
        // User login
        Optional<User> userOptional = userRepository.findByEmail(email);
        
        if (userOptional.isEmpty()) {
            System.out.println("❌ User not found: " + email);
            session.setAttribute("fail", "Invalid Email");
            return "redirect:/login";
        }
        
        User user = userOptional.get();
        System.out.println("✅ User found: " + user.getName());
        
        if (password.equals(user.getPassword())) {
            System.out.println("✅ Password match!");
            if (user.isVerified()) {
                session.setAttribute("user", user);
                session.setAttribute("pass", "Login Success, Welcome " + user.getName());
                return "redirect:/";
            } else {
                System.out.println("❌ User not verified!");
                session.setAttribute("fail", "Please verify your account first. Check OTP.");
                return "redirect:/login";
            }
        } else {
            System.out.println("❌ Password mismatch!");
            session.setAttribute("fail", "Invalid Password");
            return "redirect:/login";
        }
    }

    @Override
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/";
    }

    @Override
    public String addStock(HttpSession session) {
        if (session.getAttribute("admin") != null) {
            return "add-stock.html";
        } else {
            session.setAttribute("fail", "Admin access required");
            return "redirect:/login";
        }
    }

    @Override
    public String addStock(HttpSession session, Stock stock) {
        if (session.getAttribute("admin") != null) {
            if (stockRepository.existsById(stock.getTicker())) {
                session.setAttribute("fail", "Stock already exists");
            } else {
                stockRepository.save(stock);
                session.setAttribute("pass", "Stock added successfully");
            }
            return "redirect:/manage-stocks";
        } else {
            session.setAttribute("fail", "Admin access required");
            return "redirect:/login";
        }
    }

    @Override
    public String fetchStocks(HttpSession session, Model model) {
        if (session.getAttribute("admin") != null) {
            List<Stock> stocks = stockRepository.findAll();
            model.addAttribute("stocks", stocks);
            return "admin-view-stocks.html";
        } else {
            session.setAttribute("fail", "Admin access required");
            return "redirect:/login";
        }
    }

    @Override
    public String deleteStock(String ticker, HttpSession session) {
        if (session.getAttribute("admin") != null) {
            stockRepository.deleteById(ticker);
            session.setAttribute("pass", "Stock deleted");
            return "redirect:/manage-stocks";
        } else {
            session.setAttribute("fail", "Admin access required");
            return "redirect:/login";
        }
    }

    @Override
    public String viewStocks(HttpSession session, Model model, String company) {
        if (session.getAttribute("user") != null) {
            List<Stock> stocks;
            if (company == null || company.isEmpty()) {
                stocks = stockRepository.findAll();
            } else {
                stocks = stockRepository.findByCompanyNameLike("%" + company + "%");
            }
            
            if (stocks.isEmpty()) {
                session.setAttribute("fail", "No stocks available");
                model.addAttribute("stocks", new ArrayList<>());
            } else {
                model.addAttribute("stocks", stocks);
            }
            return "user-view-stocks.html";
        } else {
            session.setAttribute("fail", "Please login first");
            return "redirect:/login";
        }
    }

    @Override
    public String viewStock(HttpSession session, Model model, String ticker) {
        if (session.getAttribute("user") != null) {
            Optional<Stock> stock = stockRepository.findById(ticker);
            if (stock.isPresent()) {
                model.addAttribute("stock", stock.get());
                return "view-stock.html";
            } else {
                session.setAttribute("fail", "Stock not found");
                return "redirect:/view-stocks";
            }
        } else {
            session.setAttribute("fail", "Please login first");
            return "redirect:/login";
        }
    }

    // ✅ VIEW WALLET METHOD - IMPLEMENTED AND WORKING
    @Override
    public String viewWallet(HttpSession session, Model model) {
        if (session.getAttribute("user") != null) {
            User user = (User) session.getAttribute("user");
            model.addAttribute("amount", user.getAmount());
            System.out.println("Wallet balance: " + user.getAmount());
            return "wallet.html";
        } else {
            session.setAttribute("fail", "Please login first");
            return "redirect:/login";
        }
    }

    @Override
    public String rechargeWallet(double amount, HttpSession session, Model model) {
        if (session.getAttribute("user") != null) {
            model.addAttribute("amount", amount);
            return "payment.html";
        } else {
            session.setAttribute("fail", "Please login first");
            return "redirect:/login";
        }
    }

    @Override
    public String paymentSuccess(double amount, HttpSession session) {
        if (session.getAttribute("user") != null) {
            User user = (User) session.getAttribute("user");
            user.setAmount(user.getAmount() + amount);
            userRepository.save(user);
            session.setAttribute("user", user);
            session.setAttribute("pass", "Wallet recharged successfully!");
            return "redirect:/wallet";
        } else {
            return "redirect:/login";
        }
    }

    // ✅ FIXED: Added wallet balance to the model
    @Override
    public String buyStock(String ticker, double quantity, HttpSession session, Model model) {
        if (session.getAttribute("user") != null) {
            Optional<Stock> stockOpt = stockRepository.findById(ticker);
            if (stockOpt.isPresent()) {
                Stock stock = stockOpt.get();
                if (quantity <= stock.getQuantity()) {
                    double totalPrice = stock.getPrice() * quantity;
                    User user = (User) session.getAttribute("user");
                    
                    System.out.println("=== BUY STOCK ===");
                    System.out.println("Ticker: " + ticker);
                    System.out.println("Quantity: " + quantity);
                    System.out.println("Price per share: " + stock.getPrice());
                    System.out.println("Total Price: " + totalPrice);
                    System.out.println("Wallet Balance: " + user.getAmount());
                    
                    model.addAttribute("totalPrice", totalPrice);
                    model.addAttribute("quantity", quantity);
                    model.addAttribute("ticker", ticker);
                    model.addAttribute("price", stock.getPrice());
                    model.addAttribute("wallet", user.getAmount());  // ✅ ADDED THIS LINE
                    
                    return "confirm-buy.html";
                } else {
                    session.setAttribute("fail", "Not enough quantity available");
                    return "redirect:/view-stocks";
                }
            } else {
                session.setAttribute("fail", "Stock not found");
                return "redirect:/view-stocks";
            }
        } else {
            session.setAttribute("fail", "Please login first");
            return "redirect:/login";
        }
    }

    // ✅ REPLACED CONFIRM PURCHASE METHOD - UPDATES ADMIN DATA
    @Override
    public String confirmPurchase(HttpSession session, String ticker, double quantity, double price) {
        if (session.getAttribute("user") != null) {
            User user = (User) session.getAttribute("user");
            double totalPrice = price * quantity;
            
            if (user.getAmount() >= totalPrice) {
                Optional<Stock> stockOpt = stockRepository.findById(ticker);
                if (stockOpt.isPresent()) {
                    Stock stock = stockOpt.get();
                    
                    // Update stock quantity
                    stock.setQuantity(stock.getQuantity() - quantity);
                    stockRepository.save(stock);
                    
                    // Update user wallet
                    user.setAmount(user.getAmount() - totalPrice);
                    
                    // Add transaction to user's portfolio
                    List<UserStocksTransaction> transactions = user.getTransactions();
                    if (transactions == null) {
                        transactions = new ArrayList<>();
                    }
                    
                    UserStocksTransaction transaction = new UserStocksTransaction();
                    transaction.setStock_ticker(ticker);
                    transaction.setQuantity(quantity);
                    transaction.setPrice(price);
                    transactions.add(transaction);
                    user.setTransactions(transactions);
                    
                    userRepository.save(user);
                    
                    // ✅ UPDATE ADMIN DATA - This was missing!
                    AdminData adminData = dataRepository.findById(1).orElse(new AdminData());
                    adminData.setTotalStocksBought(adminData.getTotalStocksBought() + quantity);
                    adminData.setTotalTransaction(adminData.getTotalTransaction() + totalPrice);
                    adminData.setTotalPlatformFee(adminData.getTotalPlatformFee() + (totalPrice * adminData.getPlatformFeePercentage()));
                    dataRepository.save(adminData);
                    
                    session.setAttribute("user", user);
                    session.setAttribute("pass", "Stock purchased successfully!");
                    return "redirect:/portfolio";
                }
            } else {
                session.setAttribute("fail", "Insufficient wallet balance");
                return "redirect:/wallet";
            }
        }
        return "redirect:/login";
    }

    @Override
    public String viewOverview(HttpSession session, Model model) {
        if (session.getAttribute("admin") != null) {
            Optional<AdminData> data = dataRepository.findById(1);
            if (data.isPresent()) {
                model.addAttribute("data", data.get());
                return "overview.html";
            } else {
                session.setAttribute("fail", "No data available");
                return "redirect:/";
            }
        } else {
            session.setAttribute("fail", "Admin access required");
            return "redirect:/login";
        }
    }

    // ✅ VIEW PORTFOLIO METHOD - IMPLEMENTED AND WORKING
    @Override
    public String viewPortfolio(HttpSession session, Model model) {
        if (session.getAttribute("user") != null) {
            User user = (User) session.getAttribute("user");
            List<UserStocksTransaction> transactions = user.getTransactions();
            
            if (transactions == null || transactions.isEmpty()) {
                session.setAttribute("fail", "Your portfolio is empty");
                model.addAttribute("transactions", new ArrayList<>());
                return "portfolio.html";
            }
            
            double totalInvested = 0;
            double currentValue = 0;
            
            for (UserStocksTransaction transaction : transactions) {
                totalInvested += transaction.getPrice() * transaction.getQuantity();
                
                Optional<Stock> stockOpt = stockRepository.findById(transaction.getStock_ticker());
                if (stockOpt.isPresent()) {
                    currentValue += stockOpt.get().getPrice() * transaction.getQuantity();
                } else {
                    currentValue += transaction.getPrice() * transaction.getQuantity();
                }
            }
            
            model.addAttribute("totalInvested", totalInvested);
            model.addAttribute("currentValue", currentValue);
            model.addAttribute("transactions", transactions);
            return "portfolio.html";
        } else {
            session.setAttribute("fail", "Please login first");
            return "redirect:/login";
        }
    }
    
    @Override
    public String viewSell(String ticker, HttpSession session, Model model) {
        if (session.getAttribute("user") != null) {
            Optional<Stock> stock = stockRepository.findById(ticker);
            if (stock.isPresent()) {
                model.addAttribute("stock", stock.get());
                return "enter-quantity.html";
            } else {
                session.setAttribute("fail", "Stock not found");
                return "redirect:/portfolio";
            }
        } else {
            session.setAttribute("fail", "Please login first");
            return "redirect:/login";
        }
    }

    // ✅ REPLACED SELL STOCKS METHOD - FIXED WALLET UPDATE AND ADMIN DATA
    @Override
    public String sellStocks(double quantity, String ticker, HttpSession session) {
        System.out.println("=== SELL STOCKS METHOD CALLED ===");
        System.out.println("Ticker: " + ticker);
        System.out.println("Quantity to sell: " + quantity);
        
        if (session.getAttribute("user") != null) {
            User user = (User) session.getAttribute("user");
            System.out.println("Current wallet balance before sell: " + user.getAmount());
            
            List<UserStocksTransaction> transactions = user.getTransactions();
            
            if (transactions != null && !transactions.isEmpty()) {
                boolean sold = false;
                
                for (UserStocksTransaction transaction : transactions) {
                    if (transaction.getStock_ticker().equals(ticker) && transaction.getQuantity() >= quantity) {
                        Optional<Stock> stockOpt = stockRepository.findById(ticker);
                        if (stockOpt.isPresent()) {
                            Stock stock = stockOpt.get();
                            double currentPrice = stock.getPrice();
                            double sellValue = currentPrice * quantity;
                            
                            System.out.println("Current stock price: " + currentPrice);
                            System.out.println("Sell value: " + sellValue);
                            
                            // Update transaction quantity
                            double remainingQuantity = transaction.getQuantity() - quantity;
                            transaction.setQuantity(remainingQuantity);
                            
                            // Add money to wallet
                            double newBalance = user.getAmount() + sellValue;
                            user.setAmount(newBalance);
                            
                            System.out.println("Old wallet balance: " + (newBalance - sellValue));
                            System.out.println("New wallet balance: " + newBalance);
                            
                            // Remove transaction if quantity becomes 0
                            if (remainingQuantity <= 0) {
                                transactions.remove(transaction);
                                System.out.println("Transaction removed (all shares sold)");
                            }
                            
                            // Save user to database
                            userRepository.save(user);
                            
                            // ✅ UPDATE ADMIN DATA - This was missing!
                            AdminData adminData = dataRepository.findById(1).orElse(new AdminData());
                            adminData.setTotalStocksSold(adminData.getTotalStocksSold() + quantity);
                            dataRepository.save(adminData);
                            
                            // Update session
                            session.setAttribute("user", user);
                            session.setAttribute("pass", "✅ Sold " + quantity + " shares of " + ticker + " for ₹" + sellValue);
                            
                            sold = true;
                            break;
                        }
                    }
                }
                
                if (sold) {
                    System.out.println("Sale completed successfully!");
                    return "redirect:/portfolio";
                } else {
                    System.out.println("Not enough quantity to sell");
                    session.setAttribute("fail", "Not enough quantity to sell");
                    return "redirect:/portfolio";
                }
            } else {
                System.out.println("No transactions found");
                session.setAttribute("fail", "You don't own any stocks");
                return "redirect:/portfolio";
            }
        } else {
            session.setAttribute("fail", "Please login first");
            return "redirect:/login";
        }
    }
}