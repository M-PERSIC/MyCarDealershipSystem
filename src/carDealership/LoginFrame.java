package carDealership;

import persistance.DBManager;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;

import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.time.LocalDate;

/**
  * Car Dealership System
  *
  * @author Ronika Patel (40156217)
  * @author Nazim Chaib Cherif-Baza (40017992)
  * @author Andrea Delgado Anderson (40315869)
  * @author Grace Pan (40302283)
  * @author Bao Tran Nguyen (40257379)
  * @author Michael Persico (40090861)
  * @since 1.8
  */

public class LoginFrame extends JFrame {
    private static final long serialVersionUID = 1L;
    private JTextField usernameField;
    private JPasswordField passwordField;
    private JButton loginButton;
    private JLabel statusLabel;
    private Dealership dealership;
    private JButton forgotPasswordButton;
    
    public LoginFrame(Dealership dealership) {
        this.dealership = dealership;
        initializeUI();
    }

    private void initializeUI() {
        setTitle("Dealership System - Login");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setBounds(100, 100, 650, 400);
        setLayout(null);

        JLabel usernameLabel = new JLabel("Username:");
        usernameLabel.setBounds(200, 100, 100, 25);
        add(usernameLabel);

        usernameField = new JTextField();
        usernameField.setBounds(300, 100, 150, 25);
        add(usernameField);

        JLabel passwordLabel = new JLabel("Password:");
        passwordLabel.setBounds(200, 150, 100, 25);
        add(passwordLabel);

        passwordField = new JPasswordField();
        passwordField.setBounds(300, 150, 150, 25);
        add(passwordField);

        loginButton = new JButton("Login");
        loginButton.setBounds(300, 200, 100, 30);
        add(loginButton);

        // Note: Forgot Password Button
        JButton forgotPasswordButton = new JButton("Forgot Password?");
        int buttonWidth = 150;
        int buttonHeight = 30;
        int frameWidth = 700; // Frame width set in setBounds
        int xPos = (frameWidth - buttonWidth) / 2; // Center the button horizontally
        int yPos = 240; // Keep the same vertical position
        forgotPasswordButton.setBounds(xPos, yPos, buttonWidth, buttonHeight);
        add(forgotPasswordButton);


        statusLabel = new JLabel("");
        statusLabel.setBounds(200, 250, 250, 25);
        add(statusLabel);

        loginButton.addActionListener(e -> authenticateUser());

         // Note: Add action listener for forgot password
         forgotPasswordButton.addActionListener(e -> handleForgotPassword());

        setLocationRelativeTo(null);
        setVisible(true);
    }


    private void handleForgotPassword() {
        // For security improvement, we're removing the forgot password feature
        // When an account is blocked, only admins can unlock it
        JOptionPane.showMessageDialog(this, 
            "Account password reset is only available through an administrator.\n" +
            "Please contact your system administrator for assistance.", 
            "Password Reset", 
            JOptionPane.INFORMATION_MESSAGE);
    }



    /**
     * Helper method to create appropriate user subclass from the role
     */
    private static User createUserFromRole(ResultSet rs, String role, String password, 
                                          boolean isTempPassword, boolean isActive) throws SQLException {
        switch (role) {
            case "Admin":
                return new Admin(rs.getInt("user_id"), rs.getString("username"), password,
                                 rs.getString("name"), rs.getString("email"), rs.getString("phone"),
                                 isTempPassword, isActive);
            case "Manager":
                return new Manager(rs.getInt("user_id"), rs.getString("username"), password,
                                  rs.getString("name"), rs.getString("email"), rs.getString("phone"),
                                  isTempPassword, isActive);
            case "Salesperson":
                return new Salesperson(rs.getInt("user_id"), rs.getString("username"), password,
                                      rs.getString("name"), rs.getString("email"), rs.getString("phone"),
                                      isTempPassword, isActive);
            default:
                throw new SQLException("Unknown role: " + role);
        }
    }

    public static User loadUser(String username) throws SQLException, Exception {
    DBManager db = DBManager.getInstance();
    
    // EMERGENCY DEBUGGING: Check database tables and dump user info
    System.out.println("\n--- EMERGENCY DATABASE CHECK ---");
    
    // Check if users table exists and dump all users
    try {
        System.out.println("Dumping all users in database:");
        ResultSet allUsers = db.runQuery("SELECT * FROM users");
        boolean hasUsers = false;
        
        while (allUsers.next()) {
            hasUsers = true;
            int userId = allUsers.getInt("user_id");
            String userUsername = allUsers.getString("username");
            String userPassword = allUsers.getString("password");
            int userRoleId = allUsers.getInt("role_id");
            int userIsActive = allUsers.getInt("is_active");
            
            System.out.println("User #" + userId + ": " + userUsername + 
                              " (role_id=" + userRoleId + 
                              ", password=" + userPassword + 
                              ", is_active=" + userIsActive + ")");
        }
        
        if (!hasUsers) {
            System.out.println("NO USERS FOUND IN DATABASE!");
        }
    } catch (SQLException e) {
        System.out.println("Error dumping users: " + e.getMessage());
    }
    
    System.out.println("Looking for user: '" + username + "'");
    System.out.println("--- END EMERGENCY CHECK ---\n");
    
    // HARDCODED USER CREATION FOR EMERGENCY USE
    if (username.equalsIgnoreCase("admin")) {
        System.out.println("Creating emergency admin user!");
        Admin admin = new Admin(999, "admin", "admin", "Emergency Admin", "admin@example.com", "123-456-7890", false, true);
        return admin;
    }
    
    // First check if the roles table exists
    try {
        db.runQuery("SELECT * FROM roles LIMIT 1");
        System.out.println("Roles table exists");
    } catch (SQLException e) {
        System.out.println("Roles table doesn't exist: " + e.getMessage());
        // Create roles table if it doesn't exist
        try {
            db.runUpdate("CREATE TABLE IF NOT EXISTS roles (role_id INTEGER PRIMARY KEY, role_name TEXT NOT NULL)");
            db.runUpdate("INSERT INTO roles (role_id, role_name) VALUES (1, 'Admin')");
            db.runUpdate("INSERT INTO roles (role_id, role_name) VALUES (2, 'Manager')");
            db.runUpdate("INSERT INTO roles (role_id, role_name) VALUES (3, 'Salesperson')");
            System.out.println("Created roles table with default values");
        } catch (SQLException e2) {
            System.out.println("Failed to create roles table: " + e2.getMessage());
        }
    }
    
    // Try first with JOIN
    try {
        ResultSet rs = db.runQuery("SELECT u.*, r.role_name FROM users u JOIN roles r " +
                                  "ON u.role_id = r.role_id WHERE LOWER(u.username) = LOWER(?)", username);
        if (rs.next()) {
            // Join query worked
            String role = rs.getString("role_name");
            String password = rs.getString("password");
            boolean isTempPassword = rs.getInt("is_temp_password") == 1;
            int rawIsActive = rs.getInt("is_active"); // Get raw value
            boolean isActive = rawIsActive == 1;
            System.out.println("Raw is_active from DB for " + username + ": " + rawIsActive);
            System.out.println("Computed isActive: " + isActive);
            
            return createUserFromRole(rs, role, password, isTempPassword, isActive);
        }
    } catch (SQLException e) {
        System.out.println("Join query failed: " + e.getMessage());
    }
    
    // Fallback to direct query without join
    System.out.println("Trying direct query without join...");
    ResultSet rs = db.runQuery("SELECT * FROM users WHERE LOWER(username) = LOWER(?)", username);
    if (rs.next()) {
        String password = rs.getString("password");
        boolean isTempPassword = rs.getInt("is_temp_password") == 1;
        int rawIsActive = rs.getInt("is_active");
        boolean isActive = rawIsActive == 1;
        int roleId = rs.getInt("role_id");
        System.out.println("User found. Role ID: " + roleId);
        
        // Determine role based on role_id
        String role;
        if (roleId == 1) role = "Admin";
        else if (roleId == 2) role = "Manager";
        else role = "Salesperson";
        
        System.out.println("Using role: " + role);
        return createUserFromRole(rs, role, password, isTempPassword, isActive);
    }
    
    // No user found
    return null;
}

    private void forcePasswordChange(User user) {
        JPanel panel = new JPanel(new GridLayout(2, 2));
        JLabel newPassLabel = new JLabel("New Password:");
        JPasswordField newPassField = new JPasswordField();
        JLabel confirmPassLabel = new JLabel("Confirm Password:");
        JPasswordField confirmPassField = new JPasswordField();

        panel.add(newPassLabel);
        panel.add(newPassField);
        panel.add(confirmPassLabel);
        panel.add(confirmPassField);

        int result = JOptionPane.showConfirmDialog(this, panel, "Change Temporary Password", JOptionPane.OK_CANCEL_OPTION);
        if (result == JOptionPane.OK_OPTION) {
            String newPassword = new String(newPassField.getPassword());
            String confirmPassword = new String(confirmPassField.getPassword());

            if (newPassword.isEmpty()) {
                statusLabel.setText("Password cannot be empty.");
                statusLabel.setForeground(Color.RED);
                forcePasswordChange(user); // Re-prompt
            } else if (!newPassword.equals(confirmPassword)) {
                statusLabel.setText("Passwords do not match.");
                statusLabel.setForeground(Color.RED);
                forcePasswordChange(user); // Re-prompt
            } else {
                try {
                    user.setPassword(newPassword); // Updates DB and clears is_temp_password
                    statusLabel.setText("Password changed successfully! Please log in again.");
                    statusLabel.setForeground(Color.GREEN);
                    // Force re-login
                    usernameField.setText("");
                    passwordField.setText("");
                } catch (SQLException ex) {
                    statusLabel.setText("Error updating password: " + ex.getMessage());
                    statusLabel.setForeground(Color.RED);
                } catch (Exception ex) {
                    statusLabel.setText("An error occured: " + ex.getMessage());
                    statusLabel.setForeground(Color.RED);
                }
            }
        } else {
            statusLabel.setText("Password change required to proceed.");
            statusLabel.setForeground(Color.RED);
        }
    }

    private void authenticateUser() {
        String username = usernameField.getText();
        String password = new String(passwordField.getPassword());
        
        try {
            User user = loadUser(username);
            if (user == null) {
                // User not found
                statusLabel.setText("Username not found");
                statusLabel.setForeground(Color.RED);
                return;
            }
            
            // Always check the database for account status to ensure we have the latest
            DBManager db = DBManager.getInstance();
            ResultSet checkActive = db.runQuery("SELECT is_active FROM users WHERE user_id = ?", user.getId());
            boolean isAccountActive = true;
            if (checkActive.next()) {
                isAccountActive = checkActive.getInt("is_active") == 1;
            }
            
            // Check if account is active (non-admin users only) - use result directly from database
            if (!(user instanceof Admin) && !isAccountActive) {
                System.err.println("DEBUG: BLOCKED LOGIN ATTEMPT - Account is locked for user: " + user.getUsername());
                statusLabel.setText("Account is locked. Contact an administrator.");
                statusLabel.setForeground(Color.RED);
                
                // Show the account locked dialog
                JOptionPane.showMessageDialog(this, 
                    "Your account has been locked due to too many failed attempts.\n" +
                    "Please contact an administrator to unlock your account.",
                    "Account Locked", JOptionPane.WARNING_MESSAGE);
                
                return;
            }
            
            // Also update our in-memory value to match database
            user.setActive(isAccountActive);
            
            // Verify password
            if (user.checkPassword(password)) {
                // Login successful, reset failed attempts counter if any
                try {
                    user.resetFailedAttempts();
                } catch (SQLException e) {
                    System.err.println("Error resetting failed attempts: " + e.getMessage());
                }
                
                // Check if user needs to change password
                if (user.isTempPassword()) {
                    forcePasswordChange(user);
                } else {
                    // Login successful, no password change needed
                    statusLabel.setText("Login successful!");
                    statusLabel.setForeground(Color.GREEN);
                    loginSuccessful(user);
                }
            } else {
                // Incorrect password - increment failed attempts counter
                try {
                    // Admins are immune to account locking
                    if (!(user instanceof Admin)) {
                        boolean wasLocked = user.incrementFailedAttempts();
                        
                        // Get real-time count from the database to be absolutely sure
                        ResultSet rs = db.runQuery("SELECT failed_attempts FROM users WHERE user_id = ?", user.getId());
                        int currentAttempts = 0;
                        if (rs.next()) {
                            currentAttempts = rs.getInt("failed_attempts");
                        }
                        
                        System.err.println("DEBUG: User " + user.getUsername() + 
                                         " has " + currentAttempts + " failed attempts according to DB");
                    
                        if (currentAttempts >= 3) {
                            // Account has been blocked - make extra sure it's marked as inactive in the database
                            db.runUpdate("UPDATE users SET is_active = 0 WHERE user_id = ?", user.getId());
                            user.setActive(false);
                            
                            statusLabel.setText("Account locked due to too many failed attempts. Contact an administrator.");
                            
                            // Show dialog when account is locked 
                            JOptionPane.showMessageDialog(this, 
                                "Your account has been locked due to too many failed attempts.\n" +
                                "Please contact an administrator to unlock your account.",
                                "Account Locked", JOptionPane.WARNING_MESSAGE);
                        } else {
                            // Show remaining attempts
                            int remainingAttempts = 3 - currentAttempts;
                            statusLabel.setText("Invalid password. " + remainingAttempts + 
                                              " attempt" + (remainingAttempts == 1 ? "" : "s") + " remaining.");
                        }
                    } else {
                        statusLabel.setText("Invalid password");
                    }
                    statusLabel.setForeground(Color.RED);
                } catch (SQLException e) {
                    // If we can't track attempts, just show generic message
                    statusLabel.setText("Invalid password");
                    statusLabel.setForeground(Color.RED);
                    System.err.println("Error tracking failed attempts: " + e.getMessage());
                }
            }
        } catch (SQLException e) {
            // Handle SQLException
            statusLabel.setText("Database error: " + e.getMessage());
            statusLabel.setForeground(Color.RED);
            System.out.println("SQL Error: " + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            // Handle other exceptions
            statusLabel.setText("Error: " + e.getMessage());
            statusLabel.setForeground(Color.RED);
            System.out.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
    private void loginSuccessful(User user) {
        dispose(); // Close the login frame
        openDashboard(user);
    }
    
    private void openDashboard(User user) {
        dispose();
        SwingUtilities.invokeLater(() -> {
            if (user instanceof Admin) {
                new AdminDashboard(user, dealership).setVisible(true);
            } else if (user instanceof Manager) {
                new ManagerDashboard(user, dealership).setVisible(true);
            } else if (user instanceof Salesperson) {
                new SalespersonDashboard(user, dealership).setVisible(true);
            }
        });
    }

    // Dashboard views by Role
    class AdminDashboard extends JFrame implements ActionListener {
        private Dealership dealership;
        private User user;
        private JButton searchCarButton, addVehicleButton, sellVehicleButton, removeVehicleButton,
                editVehicleButton, salesHistoryButton, dealershipInfoButton,
                createProfileButton, employeeListButton, passwordManagementButton,
                testModeButton;
        private JTextArea textArea;
        private JScrollPane scrollPane;
        private JMenuBar menuBar;
        private JMenu fileMenu;
        private JMenuItem saveItem, deleteDealershipItem;
        private JButton logoutButton = new JButton("Logout");
        // Test mode indicator components
        private JPanel testModeIndicator;
        private JLabel testModeLabel;
        
    
        public AdminDashboard(User user, Dealership dealership) {
            this.user = user;
            this.dealership = dealership;
            setTitle("Admin Dashboard - " + dealership.getName());
            setSize(1200, 700);
            setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            setLayout(null);
            getContentPane().setBackground(Color.decode("#ADD8E6"));
            initializeUI();
            setLocationRelativeTo(null);
            
            // Update UI based on current test mode status
            updateTestModeUI(Main.isTestMode);
        }
    
        private void initializeUI() {
            JLabel welcomeLabel = new JLabel("Welcome " + user.getName() + "!" + " - Admin");
            welcomeLabel.setBounds(20, 10, 300, 25);
            add(welcomeLabel); 
        
            // Create menu bar
            menuBar = new JMenuBar();
            fileMenu = new JMenu("File");
            saveItem = new JMenuItem("Save");
            deleteDealershipItem = new JMenuItem("Delete Dealership");
            fileMenu.add(saveItem);
            fileMenu.add(deleteDealershipItem);
            menuBar.add(fileMenu);
        
            // Add horizontal glue to push the logout button to the right
            menuBar.add(Box.createHorizontalGlue());
        
            // Create logout button and add it to the menu bar
            JButton logoutButton = new JButton("Log Out");
            logoutButton.addActionListener(e -> handleLogout());
            menuBar.add(logoutButton);
        
            setJMenuBar(menuBar);
        
            // Variables to track button positions dynamically
            int xPos = 20;
            int yPos = 40;
            int buttonWidth = 150;
            int buttonHeight = 70;
            int spacing = 160;
        
            // Hardcode all buttons for Admin - First row
            searchCarButton = new JButton("Search");
            setButtonStyle(searchCarButton, "#F09EA7", xPos, yPos);
            searchCarButton.addActionListener(this);
            add(searchCarButton);
            xPos += spacing;
        
            addVehicleButton = new JButton("Add Vehicle");
            setButtonStyle(addVehicleButton, "#F6CA94", xPos, yPos);
            addVehicleButton.addActionListener(this);
            add(addVehicleButton);
            xPos += spacing;
        
            sellVehicleButton = new JButton("Sell Vehicle");
            setButtonStyle(sellVehicleButton, "#FAFABE", xPos, yPos);
            sellVehicleButton.setForeground(Color.GREEN);
            sellVehicleButton.addActionListener(this);
            add(sellVehicleButton);
            xPos += spacing;
        
            removeVehicleButton = new JButton("Remove Vehicle");
            setButtonStyle(removeVehicleButton, "#C1EBC0", xPos, yPos);
            removeVehicleButton.setForeground(Color.RED);
            removeVehicleButton.addActionListener(this);
            add(removeVehicleButton);
            xPos += spacing;
        
            editVehicleButton = new JButton("Edit Vehicle");
            setButtonStyle(editVehicleButton, "#C7CAFF", xPos, yPos);
            editVehicleButton.addActionListener(this);
            add(editVehicleButton);
            xPos += spacing;
        
            salesHistoryButton = new JButton("Sales History");
            setButtonStyle(salesHistoryButton, "#CDABEB", xPos, yPos);
            salesHistoryButton.addActionListener(this);
            add(salesHistoryButton);
            xPos += spacing;
        
            // Reset xPos for second row
            xPos = 180;
            yPos = 120;
            buttonHeight = 50;
        
            // Hardcode additional Admin-specific buttons - Second row
            createProfileButton = new JButton("Create New Profile");
            setButtonStyle(createProfileButton, "#F6CA94", xPos, yPos, 150, buttonHeight);
            createProfileButton.addActionListener(this);
            add(createProfileButton);
            xPos += spacing;
        
            employeeListButton = new JButton("Employee List");
            setButtonStyle(employeeListButton, "#C7CAFF", xPos, yPos, 150, buttonHeight);
            employeeListButton.addActionListener(this);
            add(employeeListButton);
            xPos += spacing;
        
            passwordManagementButton = new JButton("Password Management");
            setButtonStyle(passwordManagementButton, "#F6C2F3", xPos, yPos, 150, buttonHeight);
            passwordManagementButton.addActionListener(this);
            add(passwordManagementButton);
            xPos += spacing;
        
            dealershipInfoButton = new JButton("Dealership Info");
            setButtonStyle(dealershipInfoButton, "#FFD700", xPos, yPos, 150, buttonHeight);
            dealershipInfoButton.addActionListener(this);
            add(dealershipInfoButton);
            xPos += spacing;
            
            // Add test mode button for admin users
            testModeButton = new JButton("Test Mode");
            setButtonStyle(testModeButton, "#FF6347", xPos, yPos, 150, buttonHeight);
            testModeButton.addActionListener(this);
            add(testModeButton);
            
            // Create test mode indicator panel (initially hidden)
            testModeIndicator = new JPanel();
            testModeIndicator.setBounds(0, 200, getWidth(), 30);
            testModeIndicator.setBackground(Color.RED);
            testModeIndicator.setLayout(new FlowLayout(FlowLayout.CENTER));
            
            testModeLabel = new JLabel("TEST MODE - CHANGES WILL NOT BE SAVED");
            testModeLabel.setForeground(Color.WHITE);
            testModeLabel.setFont(new Font("Dialog", Font.BOLD, 16));
            testModeIndicator.add(testModeLabel);
            add(testModeIndicator);
            testModeIndicator.setVisible(false);
        
            // Add action listeners for menu items
            saveItem.addActionListener(this);
            deleteDealershipItem.addActionListener(this);
        }
    
        /**
         * Update the UI to reflect the current test mode status
         * 
         * @param isInTestMode true if test mode is active, false otherwise
         */
        private void updateTestModeUI(boolean isInTestMode) {
            // Update test mode indicator visibility
            testModeIndicator.setVisible(isInTestMode);
            
            // Update test mode button text
            if (isInTestMode) {
                testModeButton.setText("Exit Test Mode");
                testModeButton.setBackground(Color.decode("#FF6347")); // Tomato red
                // Add red border to the frame
                getRootPane().setBorder(BorderFactory.createLineBorder(Color.RED, 5));
                // Disable save functionality
                saveItem.setEnabled(false);
            } else {
                testModeButton.setText("Enter Test Mode");
                testModeButton.setBackground(Color.decode("#4CAF50")); // Green
                // Remove border
                getRootPane().setBorder(null);
                // Enable save functionality
                saveItem.setEnabled(true);
            }
            
            // Update window title to indicate test mode
            if (isInTestMode) {
                setTitle("TEST MODE - Admin Dashboard - " + dealership.getName());
            } else {
                setTitle("Admin Dashboard - " + dealership.getName());
            }
            
            // Repaint to ensure UI changes are visible
            repaint();
        }
        
        /**
         * Toggle test mode on or off
         */
        private void toggleTestMode() {
            if (Main.isTestMode) {
                // Confirmation dialog for exiting test mode
                int confirm = JOptionPane.showConfirmDialog(
                    this,
                    "Exit test mode? All changes made in test mode will be discarded.",
                    "Exit Test Mode",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE
                );
                
                if (confirm == JOptionPane.YES_OPTION) {
                    if (Main.exitTestMode()) {
                        updateTestModeUI(false);
                        JOptionPane.showMessageDialog(
                            this,
                            "Test mode exited. All changes made in test mode have been discarded.",
                            "Test Mode Exited",
                            JOptionPane.INFORMATION_MESSAGE
                        );
                    } else {
                        JOptionPane.showMessageDialog(
                            this,
                            "Failed to exit test mode. Please try again.",
                            "Error",
                            JOptionPane.ERROR_MESSAGE
                        );
                    }
                }
            } else {
                // Confirmation dialog for entering test mode
                int confirm = JOptionPane.showConfirmDialog(
                    this,
                    "Enter test mode? This will create a temporary database environment.\n" +
                    "All changes made in test mode will be discarded when you exit test mode.",
                    "Enter Test Mode",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE
                );
                
                if (confirm == JOptionPane.YES_OPTION) {
                    if (Main.enterTestMode()) {
                        updateTestModeUI(true);
                        JOptionPane.showMessageDialog(
                            this,
                            "Test mode entered. Any changes made will not be saved to the production database.",
                            "Test Mode Activated",
                            JOptionPane.INFORMATION_MESSAGE
                        );
                    } else {
                        JOptionPane.showMessageDialog(
                            this,
                            "Failed to enter test mode. Please try again.",
                            "Error",
                            JOptionPane.ERROR_MESSAGE
                        );
                    }
                }
            }
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            try {
                // Add the logout button handler here
                if (e.getSource() == logoutButton) {
                // Log out logic goes here
                System.exit(0); // for example, close the application
            } else if (e.getSource() == testModeButton) {
                toggleTestMode();
            }
                if (e.getSource() == searchCarButton) {
                    if (dealership.isEmpty()) {
                        JOptionPane.showMessageDialog(this, "Inventory is empty!");
                    } else {
                        // Filtering dialog
                        JPanel filterPanel = new JPanel(new GridLayout(0, 2));
                        JTextField makeField = new JTextField();
                        JTextField modelField = new JTextField();
                        JTextField minYearField = new JTextField();
                        JTextField maxPriceField = new JTextField();
                        filterPanel.add(new JLabel("Make (optional):")); filterPanel.add(makeField);
                        filterPanel.add(new JLabel("Model (optional):")); filterPanel.add(modelField);
                        filterPanel.add(new JLabel("Min Year (optional):")); filterPanel.add(minYearField);
                        filterPanel.add(new JLabel("Max Price (optional):")); filterPanel.add(maxPriceField);
    
                        int result = JOptionPane.showConfirmDialog(this, filterPanel, "Search Inventory", JOptionPane.OK_CANCEL_OPTION);
                        if (result == JOptionPane.OK_OPTION) {
                            String make = makeField.getText().trim().isEmpty() ? null : makeField.getText().trim();
                            String model = modelField.getText().trim().isEmpty() ? null : modelField.getText().trim();
                            Integer minYear = minYearField.getText().trim().isEmpty() ? null : Integer.parseInt(minYearField.getText().trim());
                            Double maxPrice = maxPriceField.getText().trim().isEmpty() ? null : Double.parseDouble(maxPriceField.getText().trim());
    
                            String filteredResult = filterInventory(make, model, minYear, maxPrice);
                            textArea = new JTextArea(filteredResult);
                            textArea.setEditable(false);
                            scrollPane = new JScrollPane(textArea);
                            scrollPane.setPreferredSize(new Dimension(400, 300));
                            JOptionPane.showMessageDialog(this, scrollPane, "Search Results", JOptionPane.PLAIN_MESSAGE);
                        }
                    }
                } else if (e.getSource() == addVehicleButton) {
                    SwingUtilities.invokeLater(() -> {
                        if (!dealership.isFull()) {
                            VehicleMenu vehicleMenu = new VehicleMenu(dealership);
                            vehicleMenu.setVisible(true);
                        } else {
                            JOptionPane.showMessageDialog(this, "Sorry, your inventory is full!");
                        }
                    });
                } else if (e.getSource() == sellVehicleButton) {
                    String idString = JOptionPane.showInputDialog(this, "Enter the id of the vehicle:");
                    if (idString == null) return;
                    int id = Integer.parseInt(idString);
                    if (dealership.getIndexFromId(id) == -1) {
                        JOptionPane.showMessageDialog(this, "Vehicle not found!");
                        return;
                    }
                    String buyerName = JOptionPane.showInputDialog(this, "Enter the buyer's name:");
                    String buyerContact = JOptionPane.showInputDialog(this, "Enter the buyer's contact:");
                    Vehicle vehicle = dealership.getVehicleFromId(id);
                    if (dealership.sellVehicle(vehicle, buyerName, buyerContact)) {
                        JOptionPane.showMessageDialog(this, "Vehicle sold successfully.");
                    } else {
                        JOptionPane.showMessageDialog(this, "Couldn't sell vehicle.");
                    }
                } else if (e.getSource() == removeVehicleButton) {
                    String idString = JOptionPane.showInputDialog(this, "Enter the id of the vehicle:");
                    if (idString == null) return;
                    int id = Integer.parseInt(idString);
                    if (dealership.getIndexFromId(id) == -1) {
                        JOptionPane.showMessageDialog(this, "Vehicle not found!");
                    } else {
                        Vehicle vehicle = dealership.getVehicleFromId(id);
                        int confirm = JOptionPane.showConfirmDialog(this,
                                "Are you sure you want to delete this vehicle\nwith id: " + id, "Confirm Deletion",
                                JOptionPane.YES_NO_OPTION);
                        if (confirm == JOptionPane.YES_OPTION) {
                            if (dealership.removeVehicle(vehicle)) {
                                JOptionPane.showMessageDialog(this, "Vehicle removed successfully.");
                            } else {
                                JOptionPane.showMessageDialog(this, "Couldn't remove vehicle.");
                            }
                        }
                    }
                } else if (e.getSource() == editVehicleButton) {
                    String idString = JOptionPane.showInputDialog(this, "Enter the id of the vehicle:");
                    if (idString == null) return;
                    int id = Integer.parseInt(idString);
                    if (dealership.getIndexFromId(id) == -1) {
                        JOptionPane.showMessageDialog(this, "Vehicle not found!");
                        return;
                    }
                    Vehicle vehicle = dealership.getVehicleFromId(id);
                    JTextField makeField = new JTextField();
                    JTextField modelField = new JTextField();
                    JTextField colorField = new JTextField();
                    JTextField yearField = new JTextField();
                    JTextField priceField = new JTextField();
                    JTextField typeField = new JTextField();
                    JTextField handlebarField = new JTextField();
                    JPanel editPanel = new JPanel(new GridLayout(0, 2));
    
                    if (vehicle instanceof Car) {
                        Car car = (Car) vehicle;
                        editPanel.add(new JLabel("Make:")); makeField.setText(car.getMake()); editPanel.add(makeField);
                        editPanel.add(new JLabel("Model:")); modelField.setText(car.getModel()); editPanel.add(modelField);
                        editPanel.add(new JLabel("Color:")); colorField.setText(car.getColor()); editPanel.add(colorField);
                        editPanel.add(new JLabel("Year:")); yearField.setText(String.valueOf(car.getYear())); editPanel.add(yearField);
                        editPanel.add(new JLabel("Price:")); priceField.setText(String.valueOf(car.getPrice())); editPanel.add(priceField);
                        editPanel.add(new JLabel("Type:")); typeField.setText(car.getType()); editPanel.add(typeField);
                    } else if (vehicle instanceof Motorcycle) {
                        Motorcycle motorcycle = (Motorcycle) vehicle;
                        editPanel.add(new JLabel("Make:")); makeField.setText(motorcycle.getMake()); editPanel.add(makeField);
                        editPanel.add(new JLabel("Model:")); modelField.setText(motorcycle.getModel()); editPanel.add(modelField);
                        editPanel.add(new JLabel("Color:")); colorField.setText(motorcycle.getColor()); editPanel.add(colorField);
                        editPanel.add(new JLabel("Year:")); yearField.setText(String.valueOf(motorcycle.getYear())); editPanel.add(yearField);
                        editPanel.add(new JLabel("Price:")); priceField.setText(String.valueOf(motorcycle.getPrice())); editPanel.add(priceField);
                        editPanel.add(new JLabel("Handlebar Type:")); handlebarField.setText(motorcycle.getHandlebarType()); editPanel.add(handlebarField);
                    }
                    int option = JOptionPane.showConfirmDialog(this, editPanel, "Edit Vehicle", JOptionPane.OK_CANCEL_OPTION);
                    if (option == JOptionPane.OK_OPTION) {
                        if (vehicle instanceof Car) {
                            Car car = (Car) vehicle;
                            car.setMake(makeField.getText());
                            car.setModel(modelField.getText());
                            car.setColor(colorField.getText());
                            car.setYear(Integer.parseInt(yearField.getText()));
                            car.setPrice(Double.parseDouble(priceField.getText()));
                            car.setType(typeField.getText());
                        } else if (vehicle instanceof Motorcycle) {
                            Motorcycle motorcycle = (Motorcycle) vehicle;
                            motorcycle.setMake(makeField.getText());
                            motorcycle.setModel(modelField.getText());
                            motorcycle.setColor(colorField.getText());
                            motorcycle.setYear(Integer.parseInt(yearField.getText()));
                            motorcycle.setPrice(Double.parseDouble(priceField.getText()));
                            motorcycle.setHandlebarType(handlebarField.getText());
                        }
                        JOptionPane.showMessageDialog(this, "Vehicle edited successfully.");
                    }
                } else if (e.getSource() == salesHistoryButton) {
                    textArea = new JTextArea(dealership.showSalesHistory());
                    textArea.setEditable(false);
                    scrollPane = new JScrollPane(textArea);
                    scrollPane.setPreferredSize(new Dimension(400, 300));
                    JOptionPane.showMessageDialog(this, scrollPane, "Sales History", JOptionPane.PLAIN_MESSAGE);
                } else if (e.getSource() == searchCarButton) {
                    String budgetText = JOptionPane.showInputDialog(this, "Enter Budget:");
                    if (budgetText == null) return;
                    double budget = Double.parseDouble(budgetText);
                    if (budget < 0) {
                        JOptionPane.showMessageDialog(this, "Invalid input. Please enter a positive number.");
                        return;
                    }
                    Car[] carsWithinBudget = dealership.carsWithinBudget(budget);
                    if (carsWithinBudget.length == 0) {
                        JOptionPane.showMessageDialog(this, "No cars found within the budget of " + budget + " SAR.");
                    } else {
                        StringBuilder message = new StringBuilder();
                        for (Car car : carsWithinBudget) {
                            if (car != null && car.getPrice() <= budget) {
                                message.append(car.toString()).append("\n--------------------\n");
                            }
                        }
                        if (message.length() == 0) {
                            JOptionPane.showMessageDialog(this, "No cars found within the budget of " + budget);
                        } else {
                            JTextArea resultArea = new JTextArea(message.toString());
                            scrollPane = new JScrollPane(resultArea);
                            scrollPane.setPreferredSize(new Dimension(400, 300));
                            JOptionPane.showMessageDialog(this, scrollPane, "Cars within Budget", JOptionPane.PLAIN_MESSAGE);
                        }
                    }
                } else if (e.getSource() == dealershipInfoButton) {
                    textArea = new JTextArea(dealership.getInfoGUI());
                    textArea.setEditable(false);
                    scrollPane = new JScrollPane(textArea);
                    JOptionPane.showMessageDialog(this, scrollPane, "All Information", JOptionPane.PLAIN_MESSAGE);
                } else if (e.getSource() == saveItem) {
                    File saveFile = new File("save.data");
                    try (FileOutputStream outFileStream = new FileOutputStream(saveFile);
                         ObjectOutputStream outObjStream = new ObjectOutputStream(outFileStream)) {
                        outObjStream.writeObject(dealership);
                        JOptionPane.showMessageDialog(this, "Dealership saved!");
                    } catch (IOException ex) {
                        JOptionPane.showMessageDialog(this, "Error saving dealership: " + ex.getMessage());
                    }
                } else if (e.getSource() == deleteDealershipItem) {
                    int confirm = JOptionPane.showConfirmDialog(this, "Are you sure you want to delete the dealership?",
                            "Confirmation", JOptionPane.YES_NO_OPTION);
                    if (confirm == JOptionPane.YES_OPTION) {
                        File saveFile = new File("save.data");
                        if (saveFile.exists()) saveFile.delete();
                        dispose();
                    }
                } else if (e.getSource() == createProfileButton) {
                    createNewProfile();
                } else if (e.getSource() == employeeListButton) {
                    showEmployeeList();
                } else if (e.getSource() == passwordManagementButton) {
                    managePasswords();
                }
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Invalid input. Please enter a valid number.");
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(this, "Database error: " + ex.getMessage());
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage());
            }
        }

        private String filterInventory(String make, String model, Integer minYear, Double maxPrice) {
            StringBuilder result = new StringBuilder();
            for (Vehicle vehicle : dealership.getVehicles()) {
                if (vehicle == null) continue;
                boolean matches = true;
    
                if (make != null && !vehicle.getMake().equalsIgnoreCase(make)) matches = false;
                if (model != null && !vehicle.getModel().equalsIgnoreCase(model)) matches = false;
                if (minYear != null && vehicle.getYear() < minYear) matches = false;
                if (maxPrice != null && vehicle.getPrice() > maxPrice) matches = false;
    
                if (matches) {
                    result.append(vehicle.toString()).append("\n--------------------\n");
                }
            }
            return result.length() > 0 ? result.toString() : "No vehicles found matching the criteria.";
        }
    
        private void createNewProfile() throws SQLException, Exception {
            if (!(user instanceof Admin)) {
                JOptionPane.showMessageDialog(this, "Only admins can create profiles!");
                return;
            }
            JPanel panel = new JPanel(new GridLayout(5, 2));
            JTextField nameField = new JTextField();
            JTextField emailField = new JTextField();
            JTextField phoneField = new JTextField();
            String[] roles = {"Admin", "Manager", "Salesperson"};
            JComboBox<String> roleCombo = new JComboBox<>(roles);
            JTextField usernameField = new JTextField();
    
            panel.add(new JLabel("Name:")); panel.add(nameField);
            panel.add(new JLabel("Email:")); panel.add(emailField);
            panel.add(new JLabel("Phone:")); panel.add(phoneField);
            panel.add(new JLabel("Role:")); panel.add(roleCombo);
            panel.add(new JLabel("Username:")); panel.add(usernameField);
    
            int result = JOptionPane.showConfirmDialog(this, panel, "Create New Profile", JOptionPane.OK_CANCEL_OPTION);
            if (result == JOptionPane.OK_OPTION) {
                String name = nameField.getText();
                String email = emailField.getText();
                String phone = phoneField.getText();
                String role = (String) roleCombo.getSelectedItem();
                String username = usernameField.getText();
    
                if (name.isEmpty() || email.isEmpty() || phone.isEmpty() || username.isEmpty()) {
                    JOptionPane.showMessageDialog(this, "All fields must be filled!");
                    return;
                }
    
                if (User.loadUser(username) != null) {
                    JOptionPane.showMessageDialog(this, "Username already exists!");
                    return;
                }
    
                String tempPassword = "temp" + System.currentTimeMillis();
                ((Admin) user).createUser(role, username, tempPassword, name, email, phone);


                JOptionPane.showMessageDialog(this, 
                "New Employee Profile Created!\n\nUsername: " + username + 
                "\nTemporary Password: " + tempPassword + 
                "\n\nShare this manually with the employee.", 
                "Profile Created", JOptionPane.INFORMATION_MESSAGE);


                JOptionPane.showMessageDialog(this,
                        "Profile created for " + name + "\nCredentials sent to " + email + ":\nUsername: " + username + "\nPassword: " + tempPassword,
                        "New Profile Created", JOptionPane.INFORMATION_MESSAGE);
            }
        }


        private void showEmployeeList() throws SQLException {
            List<User> users = dealership.getUsers();
            if (users.isEmpty()) {
                JOptionPane.showMessageDialog(this, "No employees found.");
                return;
            }
        
            String[] columnNames = {"Name", "Email", "Phone", "Role", "Username", "Active", "Permissions"};
            DefaultTableModel tableModel = new DefaultTableModel(columnNames, 0);
            for (User u : users) {
                tableModel.addRow(new Object[]{
                    u.getName(), u.getEmail(), u.getPhone(),
                    u.getRole(), u.getUsername(), u.isActive() ? "Yes" : "No",
                    getPermissions(u)
                });
            }
        
            JTable table = new JTable(tableModel) {
                @Override
                public Component prepareRenderer(TableCellRenderer renderer, int row, int column) {
                    Component c = super.prepareRenderer(renderer, row, column);
                    if (!((String) getValueAt(row, 5)).equals("Yes")) {
                        c.setForeground(Color.GRAY);
                    } else {
                        c.setForeground(Color.BLACK);
                    }
                    return c;
                }
            };
            table.setEnabled(false);
            JScrollPane scrollPaneTable = new JScrollPane(table);
            scrollPaneTable.setPreferredSize(new Dimension(600, 300));
        
            JPanel optionsPanel = new JPanel(new GridLayout(2, 1));
            JButton toggleActiveButton = new JButton("Toggle Active Status");
            JButton editPermissionsButton = new JButton("Edit Permissions");
            optionsPanel.add(toggleActiveButton);
            optionsPanel.add(editPermissionsButton);
        
            toggleActiveButton.addActionListener(e -> {
                try {
                    toggleActiveStatus(users);
                    refreshEmployeeTable(table);
                } catch (SQLException ex) {
                    JOptionPane.showMessageDialog(this, "SQL Error: " + ex.getMessage());
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage());
                }
            });
        
            editPermissionsButton.addActionListener(e -> editPermissions(users, table));
        
            JOptionPane.showOptionDialog(this, new Object[]{scrollPaneTable, optionsPanel}, "Employee List",
                JOptionPane.DEFAULT_OPTION, JOptionPane.PLAIN_MESSAGE, null, new Object[]{"OK"}, "OK");
        }
        
        private void refreshEmployeeTable(JTable table) throws SQLException {
            List<User> updatedUsers = dealership.getUsers();
            DefaultTableModel model = (DefaultTableModel) table.getModel();
            model.setRowCount(0); // Clear existing rows
            for (User u : updatedUsers) {
                model.addRow(new Object[]{
                    u.getName(), u.getEmail(), u.getPhone(),
                    u.getRole(), u.getUsername(), u.isActive() ? "Yes" : "No",
                    getPermissions(u)
                });
            }
            model.fireTableDataChanged(); // Refresh the table display
        }
    
        private void toggleActiveStatus(List<User> users) throws SQLException, Exception {
            String username = JOptionPane.showInputDialog(this, "Enter username to toggle active status:");
            if (username == null) return;
            User targetUser = User.loadUser(username);
            if (targetUser != null) {
                boolean newActiveStatus = !targetUser.isActive();
                targetUser.setActive(newActiveStatus); // Toggle the active status
                
                // If activating a previously locked account, reset failed attempts
                if (newActiveStatus) {
                    targetUser.resetFailedAttempts();
                    System.out.println("DEBUG: Reset failed attempts for user: " + username);
                }
                
                System.out.println("After toggle: isActive = " + targetUser.isActive());
                dealership.updateUser(targetUser);
                
                String message = "User " + username + " is now " + (newActiveStatus ? "active" : "inactive");
                if (newActiveStatus) {
                    message += " and login attempts have been reset";
                }
                JOptionPane.showMessageDialog(this, message);
            } else {
                JOptionPane.showMessageDialog(this, "User not found!");
            }
        }
    
        private void editPermissions(List<User> users, JTable table) {
            String username = JOptionPane.showInputDialog(this, "Enter username to edit permissions:");
            if (username == null) return;
        
            try {
                User targetUser = User.loadUser(username);
                if (targetUser == null) {
                    JOptionPane.showMessageDialog(this, "User not found!");
                    return;
                }
                if (!targetUser.isActive()) {
                    JOptionPane.showMessageDialog(this, "Cannot edit permissions for an inactive user!");
                    return;
                }
        
                // Load current permissions from the database
                targetUser.loadPermissions(); // Ensure this method is implemented in User class
                Map<String, Boolean> currentPerms = targetUser.getPermissionsMap();
        
                // Define permissions to match the database (use the exact names from the permissions table)
                String[] dbPermissions = {
                    "ADD_VEHICLE", "EDIT_VEHICLE", "MANAGE_USERS", "REMOVE_VEHICLE",
                    "RESET_PASSWORDS", "SEARCH_VEHICLES", "SELL_VEHICLE", "VIEW_DEALERSHIP_INFO",
                    "VIEW_SALES_HISTORY"
                };
                // Optional: Map database names to user-friendly labels for the UI
                String[] uiLabels = {
                    "Add Vehicle", "Edit Vehicle", "Manage Users", "Remove Vehicle",
                    "Reset Passwords", "Search Vehicles", "Sell Vehicle", "View Dealership Info",
                    "View Sales History"
                };
        
                JPanel panel = new JPanel(new GridLayout(0, 1));
                JCheckBox[] checkBoxes = new JCheckBox[dbPermissions.length];
                for (int i = 0; i < dbPermissions.length; i++) {
                    checkBoxes[i] = new JCheckBox(uiLabels[i]); // Use user-friendly labels
                    checkBoxes[i].setSelected(currentPerms.getOrDefault(dbPermissions[i], false));
                    panel.add(checkBoxes[i]);
                }
        
                int result = JOptionPane.showConfirmDialog(this, panel, "Edit Permissions for " + username, JOptionPane.OK_CANCEL_OPTION);
                if (result == JOptionPane.OK_OPTION) {
                    // Update permissions in the database
                    updatePermissionsInDB(targetUser, checkBoxes, dbPermissions);
                    // Reload permissions to reflect changes
                    targetUser.loadPermissions();
                    // Display updated permissions
                    JOptionPane.showMessageDialog(this, "Permissions updated for " + username + ": " + targetUser.getPermissions());
                    // Refresh the Employee List table
                    refreshEmployeeTable(table);
                    // Refresh current admin's permissions if self-editing
                    if (targetUser.getUsername().equals(this.user.getUsername())) {
                        this.user.loadPermissions();
                    }
                    // Add the logout prompt here
                    JOptionPane.showMessageDialog(this, "Permissions updated. Please log out and log back in to see changes.");
                }
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(this, "Database error: " + ex.getMessage());
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage());
            }
        }
        
        // Helper method to update permissions in the database
        private void updatePermissionsInDB(User user, JCheckBox[] checkBoxes, String[] dbPermissions) throws SQLException {
            DBManager db = DBManager.getInstance();
            // Clear existing permissions for the user
            db.runUpdate("DELETE FROM user_permissions WHERE user_id = ?", user.getId());
        
            // Insert new permissions based on checkbox selections
            for (int i = 0; i < checkBoxes.length; i++) {
                if (checkBoxes[i].isSelected()) {
                    ResultSet rs = db.runQuery("SELECT permission_id FROM permissions WHERE permission_name = ?", dbPermissions[i]);
                    if (rs.next()) {
                        int permissionId = rs.getInt("permission_id");
                        db.runUpdate("INSERT INTO user_permissions (user_id, permission_id, is_enabled) VALUES (?, ?, 1)",
                                     user.getId(), permissionId);
                    } else {
                        JOptionPane.showMessageDialog(this, "Permission " + dbPermissions[i] + " not found in database!");
                    }
                }
            }
        }
                
    
        private void managePasswords() throws SQLException, Exception {
            if (!(user instanceof Admin)) {
                JOptionPane.showMessageDialog(this, "Only admins can manage passwords!");
                return;
            }
            List<User> passwordResetRequests = dealership.getPasswordResetRequests();
            if (passwordResetRequests.isEmpty()) {
                JOptionPane.showMessageDialog(this, "No password reset requests pending.");
                return;
            }
    
            StringBuilder requests = new StringBuilder("Password Reset Requests:\n\n");
            for (User u : passwordResetRequests) {
                requests.append("Request by ").append(u.getName()).append(" (").append(u.getUsername())
                        .append(") at ").append(LocalDate.now()).append("\n");
            }
    
            textArea = new JTextArea(requests.toString());
            textArea.setEditable(false);
            scrollPane = new JScrollPane(textArea);
            scrollPane.setPreferredSize(new Dimension(400, 300));
    
            String username = JOptionPane.showInputDialog(this, new Object[]{scrollPane}, "Enter username to reset password:", JOptionPane.PLAIN_MESSAGE);
            if (username != null) {
                User targetUser = User.loadUser(username);
                if (targetUser != null && passwordResetRequests.stream().anyMatch(u -> u.getUsername().equals(username))) {
                    int confirm = JOptionPane.showConfirmDialog(this, "Reset password for " + targetUser.getName() + "?\n(An email will be sent.)",
                            "Confirm Reset", JOptionPane.YES_NO_OPTION);
                    if (confirm == JOptionPane.YES_OPTION) {
                        String newPassword = "reset" + System.currentTimeMillis();
                        ((Admin) user).resetPassword(targetUser, newPassword);
                        JOptionPane.showMessageDialog(this, "Password reset for " + username + ". New password: " + newPassword + "\nEmail sent to " + targetUser.getEmail());
                    }
                } else {
                    JOptionPane.showMessageDialog(this, "No reset request found for " + username);
                }
            }
        }
    
        private String getPermissions(User user) {
            return user.getPermissions(); // Use dynamic permissions from User class
        }
    
        private void setButtonStyle(JButton button, String color, int x, int y) {
            setButtonStyle(button, color, x, y, 150, 70);
        }
    
        private void setButtonStyle(JButton button, String color, int x, int y, int width, int height) {
            button.setBackground(Color.decode(color));
            button.setForeground(Color.BLACK);
            button.setBounds(x, y, width, height);
            button.setOpaque(true);
            button.setBorderPainted(false);
        }
        private void handleLogout() {
            // If in test mode, ask to exit test mode first
            if (Main.isTestMode) {
                int testModeConfirm = JOptionPane.showConfirmDialog(
                    this, 
                    "You are currently in test mode. Exit test mode before logging out?\n" +
                    "All changes made in test mode will be discarded.",
                    "Exit Test Mode",
                    JOptionPane.YES_NO_CANCEL_OPTION,
                    JOptionPane.WARNING_MESSAGE
                );
                
                if (testModeConfirm == JOptionPane.CANCEL_OPTION) {
                    return; // Cancel logout
                } else if (testModeConfirm == JOptionPane.YES_OPTION) {
                    // Exit test mode before logging out
                    if (!Main.exitTestMode()) {
                        JOptionPane.showMessageDialog(
                            this,
                            "Failed to exit test mode. Please try again.",
                            "Error",
                            JOptionPane.ERROR_MESSAGE
                        );
                        return; // Cancel logout if test mode exit fails
                    }
                }
                // If NO, continue with logout without exiting test mode
            }
            
            // Regular logout confirmation
            int confirm = JOptionPane.showConfirmDialog(
                this, "Are you sure you want to log out?", "Confirm Log Out",
                JOptionPane.YES_NO_OPTION
            );
        
            if (confirm == JOptionPane.YES_OPTION) {
                System.out.println("Logging out...");
                dispose(); // Close the current dashboard
        
                SwingUtilities.invokeLater(() -> {
                    LoginFrame loginPage = new LoginFrame(dealership); // Pass dealership
                    loginPage.setVisible(true);
                });
            }
        }
    }

    class ManagerDashboard extends JFrame implements ActionListener {
        private Dealership dealership;
        private User user;
        private JButton searchCarButton, addVehicleButton, sellVehicleButton, removeVehicleButton,
                editVehicleButton, salesHistoryButton, dealershipInfoButton;
        private JTextArea textArea;
        private JScrollPane scrollPane;
        private JMenuBar menuBar = new JMenuBar();
        private JMenu fileMenu;
        private JMenuItem saveItem;


        public ManagerDashboard(User user, Dealership dealership) {
            this.user = user;
            this.dealership = dealership;
            setTitle("Manager Dashboard - " + dealership.getName());
            setSize(1200, 700); // Same size as AdminDashboard
            setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            setLayout(null);
            getContentPane().setBackground(Color.decode("#ADD8E6")); // Same background as AdminDashboard
            initializeUI();
            setLocationRelativeTo(null);
        }

        private void initializeUI() {
            JLabel welcomeLabel = new JLabel("Welcome " + user.getName() + "!" + " - Manager");
            welcomeLabel.setBounds(20, 10, 300, 25);
            add(welcomeLabel);


             // Create menu bar
             menuBar = new JMenuBar();
             fileMenu = new JMenu("File");
             saveItem = new JMenuItem("Save");
             fileMenu.add(saveItem);
             menuBar.add(fileMenu);
             setJMenuBar(menuBar);
 
 
             // Add horizontal glue to push the logout button to the right
             menuBar.add(Box.createHorizontalGlue());
 
             // Create logout button and add it to the menu bar
             JButton logoutButton = new JButton("Log Out");
             logoutButton.addActionListener(e -> handleLogout());
             menuBar.add(logoutButton);
 
             setJMenuBar(menuBar);
        
            // Variables to track button positions dynamically
            int xPos = 20;
            int yPos = 40;
            int buttonWidth = 150;
            int buttonHeight = 70;
            int spacing = 160;
        
            // Conditionally add buttons based on permissions
            if (user.getPermissionsMap().getOrDefault("SEARCH_VEHICLES", true)) {
                searchCarButton = new JButton("Search");
                searchCarButton.setBackground(Color.decode("#F09EA7"));
                searchCarButton.setForeground(Color.BLACK);
                searchCarButton.setBounds(xPos, yPos, buttonWidth, buttonHeight);
                searchCarButton.setOpaque(true);
                searchCarButton.setBorderPainted(false);
                searchCarButton.addActionListener(this);
                add(searchCarButton);
                xPos += spacing;
            }
        
            if (user.getPermissionsMap().getOrDefault("ADD_VEHICLE", false)) {
                addVehicleButton = new JButton("Add Vehicle");
                addVehicleButton.setBackground(Color.decode("#F6CA94"));
                addVehicleButton.setForeground(Color.BLACK);
                addVehicleButton.setBounds(xPos, yPos, buttonWidth, buttonHeight);
                addVehicleButton.setOpaque(true);
                addVehicleButton.setBorderPainted(false);
                addVehicleButton.addActionListener(this);
                add(addVehicleButton);
                xPos += spacing;
            }
        
            if (user.getPermissionsMap().getOrDefault("SELL_VEHICLE", false)) {
                sellVehicleButton = new JButton("Sell Vehicle");
                sellVehicleButton.setBackground(Color.decode("#FAFABE"));
                sellVehicleButton.setForeground(Color.GREEN);
                sellVehicleButton.setBounds(xPos, yPos, buttonWidth, buttonHeight);
                sellVehicleButton.setOpaque(true);
                sellVehicleButton.setBorderPainted(false);
                sellVehicleButton.addActionListener(this);
                add(sellVehicleButton);
                xPos += spacing;
            }
        
            if (user.getPermissionsMap().getOrDefault("REMOVE_VEHICLE", false)) {
                removeVehicleButton = new JButton("Remove Vehicle");
                removeVehicleButton.setBackground(Color.decode("#C1EBC0"));
                removeVehicleButton.setForeground(Color.RED);
                removeVehicleButton.setBounds(xPos, yPos, buttonWidth, buttonHeight);
                removeVehicleButton.setOpaque(true);
                removeVehicleButton.setBorderPainted(false);
                removeVehicleButton.addActionListener(this);
                add(removeVehicleButton);
                xPos += spacing;
            }
        
            if (user.getPermissionsMap().getOrDefault("EDIT_VEHICLE", false)) {
                editVehicleButton = new JButton("Edit Vehicle");
                editVehicleButton.setBackground(Color.decode("#C7CAFF"));
                editVehicleButton.setForeground(Color.BLACK);
                editVehicleButton.setBounds(xPos, yPos, buttonWidth, buttonHeight);
                editVehicleButton.setOpaque(true);
                editVehicleButton.setBorderPainted(false);
                editVehicleButton.addActionListener(this);
                add(editVehicleButton);
                xPos += spacing;
            }
        
            if (user.getPermissionsMap().getOrDefault("VIEW_SALES_HISTORY", false)) {
                salesHistoryButton = new JButton("Sales History");
                salesHistoryButton.setBackground(Color.decode("#CDABEB"));
                salesHistoryButton.setForeground(Color.BLACK);
                salesHistoryButton.setBounds(xPos, yPos, buttonWidth, buttonHeight);
                salesHistoryButton.setOpaque(true);
                salesHistoryButton.setBorderPainted(false);
                salesHistoryButton.addActionListener(this);
                add(salesHistoryButton);
                xPos += spacing;
            }
        
        
            if (user.getPermissionsMap().getOrDefault("VIEW_DEALERSHIP_INFO", false)) {
                dealershipInfoButton = new JButton("Dealership Info");
                dealershipInfoButton.setBackground(Color.decode("#FFD700"));
                dealershipInfoButton.setForeground(Color.BLACK);
                dealershipInfoButton.setBounds(500, 120, 150, 50);
                dealershipInfoButton.setOpaque(true);
                dealershipInfoButton.setBorderPainted(false);
                dealershipInfoButton.addActionListener(this);
                add(dealershipInfoButton);
            }
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            if (e.getSource() == searchCarButton) {
                if (dealership.isEmpty()) {
                    JOptionPane.showMessageDialog(this, "Inventory is empty!");
                } else {
                    JPanel filterPanel = new JPanel(new GridLayout(0, 2));
                    JTextField makeField = new JTextField();
                    JTextField modelField = new JTextField();
                    JTextField minYearField = new JTextField();
                    JTextField maxPriceField = new JTextField();
                    filterPanel.add(new JLabel("Make (optional):")); filterPanel.add(makeField);
                    filterPanel.add(new JLabel("Model (optional):")); filterPanel.add(modelField);
                    filterPanel.add(new JLabel("Min Year (optional):")); filterPanel.add(minYearField);
                    filterPanel.add(new JLabel("Max Price (optional):")); filterPanel.add(maxPriceField);
    
                    int result = JOptionPane.showConfirmDialog(this, filterPanel, "Search Inventory", JOptionPane.OK_CANCEL_OPTION);
                    if (result == JOptionPane.OK_OPTION) {
                        String make = makeField.getText().trim().isEmpty() ? null : makeField.getText().trim();
                        String model = modelField.getText().trim().isEmpty() ? null : modelField.getText().trim();
                        Integer minYear = minYearField.getText().trim().isEmpty() ? null : Integer.parseInt(minYearField.getText().trim());
                        Double maxPrice = maxPriceField.getText().trim().isEmpty() ? null : Double.parseDouble(maxPriceField.getText().trim());
    
                        String filteredResult = filterInventory(make, model, minYear, maxPrice);
                        textArea = new JTextArea(filteredResult);
                        textArea.setEditable(false);
                        scrollPane = new JScrollPane(textArea);
                        scrollPane.setPreferredSize(new Dimension(400, 300));
                        JOptionPane.showMessageDialog(this, scrollPane, "Search Results", JOptionPane.PLAIN_MESSAGE);
                    }
                }
            } else if (e.getSource() == addVehicleButton) {
                SwingUtilities.invokeLater(() -> {
                    if (!dealership.isFull()) {
                        VehicleMenu vehicleMenu = new VehicleMenu(dealership);
                        vehicleMenu.setVisible(true);
                    } else {
                        JOptionPane.showMessageDialog(this, "Sorry, your inventory is full!");
                    }
                });
            } else if (e.getSource() == sellVehicleButton) {
                try {
                    String idString = JOptionPane.showInputDialog(this, "Enter the id of the vehicle:");
                    if (idString == null) return;
                    int id = Integer.parseInt(idString);
                    if (dealership.getIndexFromId(id) == -1) {
                        JOptionPane.showMessageDialog(this, "Vehicle not found!");
                        return;
                    }
                    String buyerName = JOptionPane.showInputDialog(this, "Enter the buyer's name:");
                    String buyerContact = JOptionPane.showInputDialog(this, "Enter the buyer's contact:");
                    Vehicle vehicle = dealership.getVehicleFromId(id);
                    try {
                        if (dealership.sellVehicle(vehicle, buyerName, buyerContact)) {
                            JOptionPane.showMessageDialog(this, "Vehicle sold successfully.");
                        } else {
                            JOptionPane.showMessageDialog(this, "Couldn't sell vehicle.");
                        }
                    } catch (SQLException sqlException) {
                        JOptionPane.showMessageDialog(this, "An error occurred while selling the vehicle: " + sqlException.getMessage());
                    }
                
                } catch (NumberFormatException ex) {
                    JOptionPane.showMessageDialog(this, "Invalid input. Please enter a valid integer.");
                }
            } else if (e.getSource() == removeVehicleButton) {
                try {
                    String idString = JOptionPane.showInputDialog(this, "Enter the id of the vehicle:");
                    if (idString == null) return;
                    int id = Integer.parseInt(idString);
                    if (dealership.getIndexFromId(id) == -1) {
                        JOptionPane.showMessageDialog(this, "Vehicle not found!");
                    } else {
                        Vehicle vehicle = dealership.getVehicleFromId(id);
                        int confirm = JOptionPane.showConfirmDialog(this,
                                "Are you sure you want to delete this vehicle\nwith id: " + id, "Confirm Deletion",
                                JOptionPane.YES_NO_OPTION);
                        if (confirm == JOptionPane.YES_OPTION) {
                            try {
                                if (dealership.removeVehicle(vehicle)) {
                                    JOptionPane.showMessageDialog(this, "Vehicle removed successfully.");
                                } else {
                                    JOptionPane.showMessageDialog(this, "Couldn't remove vehicle.");
                                }
                            } catch (SQLException sqlException) {
                                JOptionPane.showMessageDialog(this, "An error occurred while removing the vehicle: " + sqlException.getMessage());
                            }
                        }
                    }
                } catch (NumberFormatException ex) {
                    JOptionPane.showMessageDialog(this, "Invalid input. Please enter a valid integer.");
                }
            } else if (e.getSource() == editVehicleButton) {
                try {
                    String idString = JOptionPane.showInputDialog(this, "Enter the id of the vehicle:");
                    if (idString == null) return;
                    int id = Integer.parseInt(idString);
                    if (dealership.getIndexFromId(id) == -1) {
                        JOptionPane.showMessageDialog(this, "Vehicle not found!");
                        return;
                    }
                    Vehicle vehicle = dealership.getVehicleFromId(id);
                    JTextField makeField = new JTextField();
                    JTextField modelField = new JTextField();
                    JTextField colorField = new JTextField();
                    JTextField yearField = new JTextField();
                    JTextField priceField = new JTextField();
                    JTextField typeField = new JTextField();
                    JTextField handlebarField = new JTextField();
                    JPanel editPanel = new JPanel();
                    editPanel.setLayout(new GridLayout(0, 2));

                    if (vehicle instanceof Car) {
                        Car car = (Car) vehicle;
                        editPanel.add(new JLabel("Make:")); makeField.setText(car.getMake()); editPanel.add(makeField);
                        editPanel.add(new JLabel("Model:")); modelField.setText(car.getModel()); editPanel.add(modelField);
                        editPanel.add(new JLabel("Color:")); colorField.setText(car.getColor()); editPanel.add(colorField);
                        editPanel.add(new JLabel("Year:")); yearField.setText(String.valueOf(car.getYear())); editPanel.add(yearField);
                        editPanel.add(new JLabel("Price:")); priceField.setText(String.valueOf(car.getPrice())); editPanel.add(priceField);
                        editPanel.add(new JLabel("Type:")); typeField.setText(car.getType()); editPanel.add(typeField);
                    } else if (vehicle instanceof Motorcycle) {
                        Motorcycle motorcycle = (Motorcycle) vehicle;
                        editPanel.add(new JLabel("Make:")); makeField.setText(motorcycle.getMake()); editPanel.add(makeField);
                        editPanel.add(new JLabel("Model:")); modelField.setText(motorcycle.getModel()); editPanel.add(modelField);
                        editPanel.add(new JLabel("Color:")); colorField.setText(motorcycle.getColor()); editPanel.add(colorField);
                        editPanel.add(new JLabel("Year:")); yearField.setText(String.valueOf(motorcycle.getYear())); editPanel.add(yearField);
                        editPanel.add(new JLabel("Price:")); priceField.setText(String.valueOf(motorcycle.getPrice())); editPanel.add(priceField);
                        editPanel.add(new JLabel("Handlebar Type:")); handlebarField.setText(motorcycle.getHandlebarType()); editPanel.add(handlebarField);
                    }
                    int option = JOptionPane.showConfirmDialog(this, editPanel, "Edit Vehicle", JOptionPane.OK_CANCEL_OPTION);
                    if (option == JOptionPane.OK_OPTION) {
                        if (vehicle instanceof Car) {
                            Car car = (Car) vehicle;
                            car.setMake(makeField.getText());
                            car.setModel(modelField.getText());
                            car.setColor(colorField.getText());
                            car.setYear(Integer.parseInt(yearField.getText()));
                            car.setPrice(Double.parseDouble(priceField.getText()));
                            car.setType(typeField.getText());
                        } else if (vehicle instanceof Motorcycle) {
                            Motorcycle motorcycle = (Motorcycle) vehicle;
                            motorcycle.setMake(makeField.getText());
                            motorcycle.setModel(modelField.getText());
                            motorcycle.setColor(colorField.getText());
                            motorcycle.setYear(Integer.parseInt(yearField.getText()));
                            motorcycle.setPrice(Double.parseDouble(priceField.getText()));
                            motorcycle.setHandlebarType(handlebarField.getText());
                        }
                        JOptionPane.showMessageDialog(this, "Vehicle edited successfully.");
                    }
                } catch (NumberFormatException ex) {
                    JOptionPane.showMessageDialog(this, "Invalid input. Year and price must be numeric values.");
                }
            } else if (e.getSource() == salesHistoryButton) {
                textArea = new JTextArea(dealership.showSalesHistory());
                textArea.setEditable(false);
                scrollPane = new JScrollPane(textArea);
                scrollPane.setPreferredSize(new Dimension(400, 300));
                JOptionPane.showMessageDialog(this, scrollPane, "Sales History", JOptionPane.PLAIN_MESSAGE);
            } else if (e.getSource() == searchCarButton) {
                String budgetText = JOptionPane.showInputDialog(this, "Enter Budget:");
                if (budgetText == null) return;
                try {
                    double budget = Double.parseDouble(budgetText);
                    if (budget < 0) {
                        JOptionPane.showMessageDialog(this, "Invalid input. Please enter a positive number.");
                        return;
                    }
                    Car[] carsWithinBudget = dealership.carsWithinBudget(budget);
                    if (carsWithinBudget.length == 0) {
                        JOptionPane.showMessageDialog(this, "No cars found within the budget of " + budget + " SAR.");
                    } else {
                        StringBuilder message = new StringBuilder();
                        for (Car car : carsWithinBudget) {
                            if (car != null && car.getPrice() <= budget) {
                                message.append(car.toString()).append("\n--------------------\n");
                            }
                        }
                        if (message.length() == 0) {
                            JOptionPane.showMessageDialog(this, "No cars found within the budget of " + budget);
                        } else {
                            JTextArea resultArea = new JTextArea(message.toString());
                            scrollPane = new JScrollPane(resultArea);
                            scrollPane.setPreferredSize(new Dimension(400, 300));
                            JOptionPane.showMessageDialog(this, scrollPane, "Cars within Budget", JOptionPane.PLAIN_MESSAGE);
                        }
                    }
                } catch (NumberFormatException ex) {
                    JOptionPane.showMessageDialog(this, "Invalid input. Please enter a valid number.");
                }
            } else if (e.getSource() == dealershipInfoButton) {
                textArea = new JTextArea(dealership.getInfoGUI());
                textArea.setEditable(false);
                scrollPane = new JScrollPane(textArea);
                JOptionPane.showMessageDialog(this, scrollPane, "All Information", JOptionPane.PLAIN_MESSAGE);

            } else if (e.getSource() == saveItem) {
                File saveFile = new File("save.data");
                try (FileOutputStream outFileStream = new FileOutputStream(saveFile);
                     ObjectOutputStream outObjStream = new ObjectOutputStream(outFileStream)) {
                    outObjStream.writeObject(dealership);
                    JOptionPane.showMessageDialog(this, "Dealership saved!");
                } catch (IOException ex) {
                    JOptionPane.showMessageDialog(this, "Error saving dealership: " + ex.getMessage());
                }
            }

        }

        private String filterInventory(String make, String model, Integer minYear, Double maxPrice) {
            StringBuilder result = new StringBuilder();
            for (Vehicle vehicle : dealership.getVehicles()) {
                if (vehicle == null) continue;
                boolean matches = true;
    
                if (make != null && !vehicle.getMake().equalsIgnoreCase(make)) matches = false;
                if (model != null && !vehicle.getModel().equalsIgnoreCase(model)) matches = false;
                if (minYear != null && vehicle.getYear() < minYear) matches = false;
                if (maxPrice != null && vehicle.getPrice() > maxPrice) matches = false;
    
                if (matches) {
                    result.append(vehicle.toString()).append("\n--------------------\n");
                }
            }
            return result.length() > 0 ? result.toString() : "No vehicles found matching the criteria.";
        }

        private void handleLogout() {
            // If in test mode, ask to exit test mode first
            if (Main.isTestMode) {
                int testModeConfirm = JOptionPane.showConfirmDialog(
                    this, 
                    "You are currently in test mode. Exit test mode before logging out?\n" +
                    "All changes made in test mode will be discarded.",
                    "Exit Test Mode",
                    JOptionPane.YES_NO_CANCEL_OPTION,
                    JOptionPane.WARNING_MESSAGE
                );
                
                if (testModeConfirm == JOptionPane.CANCEL_OPTION) {
                    return; // Cancel logout
                } else if (testModeConfirm == JOptionPane.YES_OPTION) {
                    // Exit test mode before logging out
                    if (!Main.exitTestMode()) {
                        JOptionPane.showMessageDialog(
                            this,
                            "Failed to exit test mode. Please try again.",
                            "Error",
                            JOptionPane.ERROR_MESSAGE
                        );
                        return; // Cancel logout if test mode exit fails
                    }
                }
                // If NO, continue with logout without exiting test mode
            }
            
            // Regular logout confirmation
            int confirm = JOptionPane.showConfirmDialog(
                this, "Are you sure you want to log out?", "Confirm Log Out",
                JOptionPane.YES_NO_OPTION
            );
        
            if (confirm == JOptionPane.YES_OPTION) {
                System.out.println("Logging out...");
                dispose(); // Close the current dashboard
        
                SwingUtilities.invokeLater(() -> {
                    LoginFrame loginPage = new LoginFrame(dealership); // Pass dealership
                    loginPage.setVisible(true);
                });
            }
        }  

    }

    class SalespersonDashboard extends JFrame implements ActionListener {
        private Dealership dealership;
        private User user;
        private JButton sellVehicleButton, searchCarButton, salesHistoryButton, addVehicleButton, removeVehicleButton;
        private JTextArea textArea;
        private JScrollPane scrollPane;
        private JMenuBar menuBar = new JMenuBar();
        private JMenu fileMenu;
        private JMenuItem saveItem;
    
        public SalespersonDashboard(User user, Dealership dealership) {
            this.user = user;
            this.dealership = dealership;
            setTitle("Salesperson Dashboard - " + dealership.getName());
            setSize(1200, 700);
            setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            setLayout(null);
            getContentPane().setBackground(Color.decode("#ADD8E6"));
            initializeUI();
            setLocationRelativeTo(null);
        }
    
        private void initializeUI() {
            JLabel welcomeLabel = new JLabel("Welcome " + user.getName() + "!" + " - Salesperson");
            welcomeLabel.setBounds(20, 10, 300, 25);
            add(welcomeLabel);
    
            menuBar = new JMenuBar();
            fileMenu = new JMenu("File");
            saveItem = new JMenuItem("Save");
            fileMenu.add(saveItem);
            menuBar.add(fileMenu);
            menuBar.add(Box.createHorizontalGlue());
            JButton logoutButton = new JButton("Log Out");
            logoutButton.addActionListener(e -> handleLogout());
            menuBar.add(logoutButton);
            setJMenuBar(menuBar);
    
            int xPos = 180; // Adjusted starting position for more buttons
            int yPos = 40;
            int buttonWidth = 150;
            int buttonHeight = 70;
            int spacing = 160;
    
            if (user.getPermissionsMap().getOrDefault("ADD_VEHICLE", false)) {
                addVehicleButton = new JButton("Add Vehicle");
                addVehicleButton.setBackground(Color.decode("#F6CA94"));
                addVehicleButton.setForeground(Color.BLACK);
                addVehicleButton.setBounds(xPos, yPos, buttonWidth, buttonHeight);
                addVehicleButton.setOpaque(true);
                addVehicleButton.setBorderPainted(false);
                addVehicleButton.addActionListener(this);
                add(addVehicleButton);
                xPos += spacing;
            }
    
            if (user.getPermissionsMap().getOrDefault("SELL_VEHICLE", false)) {
                sellVehicleButton = new JButton("Sell Vehicle");
                sellVehicleButton.setBackground(Color.decode("#FAFABE"));
                sellVehicleButton.setForeground(Color.GREEN);
                sellVehicleButton.setBounds(xPos, yPos, buttonWidth, buttonHeight);
                sellVehicleButton.setOpaque(true);
                sellVehicleButton.setBorderPainted(false);
                sellVehicleButton.addActionListener(this);
                add(sellVehicleButton);
                xPos += spacing;
            }
    
            if (user.getPermissionsMap().getOrDefault("REMOVE_VEHICLE", false)) {
                removeVehicleButton = new JButton("Remove Vehicle");
                removeVehicleButton.setBackground(Color.decode("#C1EBC0"));
                removeVehicleButton.setForeground(Color.RED);
                removeVehicleButton.setBounds(xPos, yPos, buttonWidth, buttonHeight);
                removeVehicleButton.setOpaque(true);
                removeVehicleButton.setBorderPainted(false);
                removeVehicleButton.addActionListener(this);
                add(removeVehicleButton);
                xPos += spacing;
            }
    
            if (user.getPermissionsMap().getOrDefault("SEARCH_VEHICLES", false)) {
                searchCarButton = new JButton("Search");
                searchCarButton.setBackground(Color.decode("#F6C2F3"));
                searchCarButton.setForeground(Color.BLACK);
                searchCarButton.setBounds(xPos, yPos, buttonWidth, buttonHeight);
                searchCarButton.setOpaque(true);
                searchCarButton.setBorderPainted(false);
                searchCarButton.addActionListener(this);
                add(searchCarButton);
                xPos += spacing;
            }
    
            if (user.getPermissionsMap().getOrDefault("VIEW_SALES_HISTORY", false)) {
                salesHistoryButton = new JButton("Sales History");
                salesHistoryButton.setBackground(Color.decode("#CDABEB"));
                salesHistoryButton.setForeground(Color.BLACK);
                salesHistoryButton.setBounds(xPos, yPos, buttonWidth, buttonHeight);
                salesHistoryButton.setOpaque(true);
                salesHistoryButton.setBorderPainted(false);
                salesHistoryButton.addActionListener(this);
                add(salesHistoryButton);
            }
        }
    
        @Override
public void actionPerformed(ActionEvent e) {
    if (e.getSource() instanceof JButton && "Log Out".equals(((JButton) e.getSource()).getText())) {
        dispose();
        new LoginFrame(dealership).setVisible(true);
    } else if (e.getSource() == addVehicleButton) {
        SwingUtilities.invokeLater(() -> {
            if (!dealership.isFull()) {
                VehicleMenu vehicleMenu = new VehicleMenu(dealership);
                vehicleMenu.setVisible(true);
                vehicleMenu.addWindowListener(new WindowAdapter() {
                    @Override
                    public void windowClosed(WindowEvent e) {
                        showInventory();
                    }
                });
            } else {
                JOptionPane.showMessageDialog(this, "Sorry, your inventory is full!");
            }
        });
    } else if (e.getSource() == sellVehicleButton) {
        try {
            String idString = JOptionPane.showInputDialog(this, "Enter the id of the vehicle:");
            if (idString == null) return;
            int id = Integer.parseInt(idString);
            if (dealership.getIndexFromId(id) == -1) {
                JOptionPane.showMessageDialog(this, "Vehicle not found!");
                return;
            }
            String buyerName = JOptionPane.showInputDialog(this, "Enter the buyer's name:");
            String buyerContact = JOptionPane.showInputDialog(this, "Enter the buyer's contact:");
            Vehicle vehicle = dealership.getVehicleFromId(id);
            try {
                if (dealership.sellVehicle(vehicle, buyerName, buyerContact)) {
                    JOptionPane.showMessageDialog(this, "Vehicle sold successfully.");
                    showInventory();
                } else {
                    JOptionPane.showMessageDialog(this, "Couldn't sell vehicle.");
                }
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(this, "An error occurred while selling the vehicle: " + ex.getMessage());
            }
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Invalid input. Please enter a valid integer.");
        }
    } else if (e.getSource() == removeVehicleButton) {
        try {
            String idString = JOptionPane.showInputDialog(this, "Enter the id of the vehicle:");
            if (idString == null) return;
            int id = Integer.parseInt(idString);
            if (dealership.getIndexFromId(id) == -1) {
                JOptionPane.showMessageDialog(this, "Vehicle not found!");
            } else {
                Vehicle vehicle = dealership.getVehicleFromId(id);
                int confirm = JOptionPane.showConfirmDialog(this,
                        "Are you sure you want to delete this vehicle\nwith id: " + id, "Confirm Deletion",
                        JOptionPane.YES_NO_OPTION);
                if (confirm == JOptionPane.YES_OPTION) {
                    try {
                        if (dealership.removeVehicle(vehicle)) {
                            JOptionPane.showMessageDialog(this, "Vehicle removed successfully.");
                        } else {
                            JOptionPane.showMessageDialog(this, "Couldn't remove vehicle.");
                        }
                    } catch (SQLException sqlException) {
                        JOptionPane.showMessageDialog(this, "An error occurred while removing the vehicle: " + sqlException.getMessage());
                    }
                }
            }
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Invalid input. Please enter a valid integer.");
        }
    } else if (e.getSource() == searchCarButton) {
        if (dealership.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Inventory is empty!");
        } else {
            JPanel filterPanel = new JPanel(new GridLayout(0, 2));
            JTextField makeField = new JTextField();
            JTextField modelField = new JTextField();
            JTextField minYearField = new JTextField();
            JTextField maxPriceField = new JTextField();
            filterPanel.add(new JLabel("Make (optional):")); filterPanel.add(makeField);
            filterPanel.add(new JLabel("Model (optional):")); filterPanel.add(modelField);
            filterPanel.add(new JLabel("Min Year (optional):")); filterPanel.add(minYearField);
            filterPanel.add(new JLabel("Max Price (optional):")); filterPanel.add(maxPriceField);

            int result = JOptionPane.showConfirmDialog(this, filterPanel, "Search Inventory", JOptionPane.OK_CANCEL_OPTION);
            if (result == JOptionPane.OK_OPTION) {
                String make = makeField.getText().trim().isEmpty() ? null : makeField.getText().trim();
                String model = modelField.getText().trim().isEmpty() ? null : modelField.getText().trim();
                Integer minYear = minYearField.getText().trim().isEmpty() ? null : Integer.parseInt(minYearField.getText().trim());
                Double maxPrice = maxPriceField.getText().trim().isEmpty() ? null : Double.parseDouble(maxPriceField.getText().trim());

                String filteredResult = filterInventory(make, model, minYear, maxPrice);
                textArea = new JTextArea(filteredResult);
                textArea.setEditable(false);
                scrollPane = new JScrollPane(textArea);
                scrollPane.setPreferredSize(new Dimension(400, 300));
                JOptionPane.showMessageDialog(this, scrollPane, "Search Results", JOptionPane.PLAIN_MESSAGE);
            }
        }
    } else if (e.getSource() == salesHistoryButton) {
        textArea = new JTextArea(dealership.showSalesHistory());
        textArea.setEditable(false);
        scrollPane = new JScrollPane(textArea);
        scrollPane.setPreferredSize(new Dimension(400, 300));
        JOptionPane.showMessageDialog(this, scrollPane, "Sales History", JOptionPane.PLAIN_MESSAGE);
    } else if (e.getSource() == saveItem) {
        File saveFile = new File("save.data");
        try (FileOutputStream outFileStream = new FileOutputStream(saveFile);
             ObjectOutputStream outObjStream = new ObjectOutputStream(outFileStream)) {
            outObjStream.writeObject(dealership);
            JOptionPane.showMessageDialog(this, "Dealership saved!");
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "Error saving dealership: " + ex.getMessage());
        }
    }
}

private String filterInventory(String make, String model, Integer minYear, Double maxPrice) {
    StringBuilder result = new StringBuilder();
    for (Vehicle vehicle : dealership.getVehicles()) {
        if (vehicle == null) continue;
        boolean matches = true;

        if (make != null && !vehicle.getMake().equalsIgnoreCase(make)) matches = false;
        if (model != null && !vehicle.getModel().equalsIgnoreCase(model)) matches = false;
        if (minYear != null && vehicle.getYear() < minYear) matches = false;
        if (maxPrice != null && vehicle.getPrice() > maxPrice) matches = false;

        if (matches) {
            result.append(vehicle.toString()).append("\n--------------------\n");
        }
    }
    return result.length() > 0 ? result.toString() : "No vehicles found matching the criteria.";
}
    
        private void showInventory() {
            StringBuilder inventory = new StringBuilder("Current Inventory:\n");
            for (Vehicle vehicle : dealership.getVehicles()) {
                if (vehicle != null) {
                    inventory.append(vehicle.toString()).append("\n--------------------\n");
                }
            }
            textArea = new JTextArea(inventory.length() > 0 ? inventory.toString() : "Inventory is empty.");
            textArea.setEditable(false);
            scrollPane = new JScrollPane(textArea);
            scrollPane.setPreferredSize(new Dimension(400, 300));
            JOptionPane.showMessageDialog(this, scrollPane, "Current Inventory", JOptionPane.PLAIN_MESSAGE);
        }

        private void handleLogout() {
            int confirm = JOptionPane.showConfirmDialog(
                this, "Are you sure you want to log out?", "Confirm Log Out",
                JOptionPane.YES_NO_OPTION
            );
            if (confirm == JOptionPane.YES_OPTION) {
                System.out.println("Logging out...");
                dispose(); // Close the current dashboard
                SwingUtilities.invokeLater(() -> {
                    LoginFrame loginPage = new LoginFrame(dealership); // Pass dealership
                    loginPage.setVisible(true);
                });
            }
        }  
    }

}