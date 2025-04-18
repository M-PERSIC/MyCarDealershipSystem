package carDealership;

import persistance.DBManager;
import persistance.DealershipLayer;

import java.io.IOException;
import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

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
public class Dealership implements Serializable {
    private static final long serialVersionUID = 1L;
    private String name;
    private String location;
    private int nv;
    private int ns;
    private Vehicle[] inventory;
    private Sale[] sales;
    private int nextId;
    private transient DealershipLayer m_dealershipLayer;

    /**
     * Constructor for the Dealership class
     *
     * @param name        - the name of the dealership
     * @param location    - the location of the dealership
     * @param maxInventory - the maximum inventory capacity
     * @throws SQLException if a database access error occurs
     */
    public Dealership(String name, String location, int maxInventory) throws SQLException {
        this.name = name;
        this.location = location;
        inventory = new Vehicle[maxInventory];
        sales = new Sale[maxInventory * 2];
        nv = 0;
        ns = 0;
        nextId = 1; // Changed from 0 to match repository's logic for IDs starting at 1
        m_dealershipLayer = new DealershipLayer(name, location, maxInventory);
    }

    /**
     * Custom deserialization method to reinitialize transient fields
     *
     * @param in - the object input stream for deserialization
     * @throws IOException if an I/O error occurs
     * @throws ClassNotFoundException if the class of a serialized object cannot be found
     */
    private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        try {
            if (name != null && location != null && inventory != null) {
                m_dealershipLayer = new DealershipLayer(name, location, inventory.length);
            }
        } catch (SQLException e) {
            throw new IOException("Failed to reinitialize DealershipLayer during deserialization", e);
        }
    }

    /**
     * Getter method for the dealership name
     *
     * @return the dealership name
     */
    public String getName() {
        return this.name;
    }

    /**
     * Add a vehicle to the dealership inventory
     *
     * @param vehicle - the vehicle to be added
     * @return true if the vehicle was successfully added, false otherwise
     * @throws SQLException if a database access error occurs
     */
    public boolean addVehicle(Vehicle vehicle) throws SQLException {
        if (nv == inventory.length) {
            return false;
        }

        // Don't assign ID yet - we'll get it from the database
        int vehicleIndex = nv;
        
        // Add to in-memory inventory
        if (vehicle instanceof Car) {
            inventory[nv++] = new Car((Car) vehicle);
        } else if (vehicle instanceof Motorcycle) {
            inventory[nv++] = new Motorcycle((Motorcycle) vehicle);
        }

        try {
            // Persist to database without specifying vehicle_id
            DBManager db = DBManager.getInstance();
            String query = "INSERT INTO Vehicle (make, model, color, year, price, " +
                    (vehicle instanceof Car ? "car_type" : "handlebar_type") +
                    ", dealerships_id) VALUES (?, ?, ?, ?, ?, ?, ?)";
            Object[] params = {
                vehicle.getMake(), vehicle.getModel(), vehicle.getColor(),
                vehicle.getYear(), vehicle.getPrice(),
                vehicle instanceof Car ? ((Car) vehicle).getType() : ((Motorcycle) vehicle).getHandlebarType(),
                m_dealershipLayer.getDealershipId()
            };
            
            // Execute the insert
            db.runInsert(query, params);
            
            // Get the auto-generated ID from the database
            ResultSet rs = db.runQuery("SELECT last_insert_rowid()");
            if (rs.next()) {
                int generatedId = rs.getInt(1);
                
                // Update both the original vehicle and the inventory copy with the database ID
                vehicle.setId(generatedId);
                inventory[vehicleIndex].setId(generatedId);
                
                // Update nextId to be greater than any assigned ID to avoid future conflicts
                if (generatedId >= nextId) {
                    nextId = generatedId + 1;
                }
            }
            return true;
        } catch (SQLException e) {
            // If database operation fails, remove the vehicle from inventory
            if (vehicleIndex < nv) {
                // Shift all elements after the failed vehicle one position back
                for (int i = vehicleIndex; i < nv - 1; i++) {
                    inventory[i] = inventory[i + 1];
                }
                // Clear the last element and decrement count
                inventory[nv - 1] = null;
                nv--;
            }
            // Re-throw the exception for proper handling upstream
            throw e;
        }
        // No need for additional return statement - the try block returns true if successful
    }

    /**
     * Remove a vehicle from the dealership inventory
     *
     * @param vehicle - the vehicle to be removed
     * @return true if the vehicle was successfully removed, false otherwise
     * @throws SQLException if a database access error occurs
     */
    public boolean removeVehicle(Vehicle vehicle) throws SQLException {
        if (vehicle == null) {
            return false;
        }
        int index = getIndexFromId(vehicle.id);
        if (index == -1) {
            return false;
        }

        // Remove from database
        DBManager db = DBManager.getInstance();
        db.runUpdate("DELETE FROM Vehicle WHERE vehicle_id = ?", vehicle.getId());

        // Shift in-memory inventory
        for (int i = index; i < nv - 1; i++) {
            inventory[i] = inventory[i + 1];
        }
        inventory[--nv] = null; // Clear last slot
        return true;
    }

    /**
     * Sell a vehicle from the dealership inventory
     *
     * @param vehicle      - the vehicle to be sold
     * @param buyerName    - the name of the buyer
     * @param buyerContact - the contact information of the buyer
     * @return true if the vehicle was successfully sold, false otherwise
     * @throws SQLException if a database access error occurs
     */
    public boolean sellVehicle(Vehicle vehicle, String buyerName, String buyerContact) throws SQLException {
        if (!removeVehicle(vehicle)) {
            return false;
        }

        // Add to sales in memory
        sales[ns++] = new Sale(vehicle, buyerName, buyerContact, LocalDate.now());

        // Persist sale to database
        DBManager db = DBManager.getInstance();
        db.runInsert("INSERT INTO Sales (vehicle_id, user_id, buyer_name, buyer_contact) VALUES (?, ?, ?, ?)",
                vehicle.getId(), 1, buyerName, buyerContact); // user_id=1 as placeholder

        return true;
    }

    /**
     * Find the inventory index of a vehicle with the specified ID
     * 
     * @param id - the vehicle ID to search for
     * @return the index of the vehicle in the inventory, or -1 if not found
     */
    public int getIndexFromId(int id) {
        for (int i = 0; i < nv; i++) {
            if (inventory[i] != null && inventory[i].id == id) {
                return i;
            }
        }
        return -1;
    }
    
    /**
     * Refreshes the dealership data when test mode is toggled
     * Call this when entering or exiting test mode
     * 
     * @throws SQLException if a database access error occurs
     */
    public void refreshOnTestModeChange() throws SQLException {
        // Clear the inventory and reload from the new database connection
        reloadInventoryFromDatabase();
    }

    /**
     * Getter method for the dealership inventory
     * Loads vehicles from the database to ensure we're showing data from the current connection (test or real)
     *
     * @return array of vehicles in the inventory
     */
    public Vehicle[] getVehicles() {
        // No need to reload every time - the inventory is already maintained by add/remove/sell
        // operations through the correct database connection
        return inventory;
    }
    
    /**
     * Reloads the inventory from the current database connection
     * Ensures we're showing the correct vehicles based on test/normal mode
     * 
     * @throws SQLException if a database access error occurs
     */
    private void reloadInventoryFromDatabase() throws SQLException {
        // Clear existing inventory
        for (int i = 0; i < inventory.length; i++) {
            inventory[i] = null;
        }
        nv = 0;
        
        // Reload from current database connection
        DBManager db = DBManager.getInstance();
        ResultSet rs = db.runQuery("SELECT * FROM Vehicle WHERE dealerships_id = ? AND is_sold = 0", 
                                 m_dealershipLayer.getDealershipId());
        
        while (rs.next() && nv < inventory.length) {
            int id = rs.getInt("vehicle_id");
            String make = rs.getString("make");
            String model = rs.getString("model");
            String color = rs.getString("color");
            int year = rs.getInt("year");
            double price = rs.getDouble("price");
            
            // Determine if it's a car or motorcycle based on which specific field is not null
            String carType = rs.getString("car_type");
            String handlebarType = rs.getString("handlebar_type");
            
            if (carType != null) {
                // It's a car
                Car car = new Car(make, model, color, year, price, carType);
                car.setId(id);
                inventory[nv++] = car;
            } else if (handlebarType != null) {
                // It's a motorcycle
                Motorcycle motorcycle = new Motorcycle(make, model, color, year, price, handlebarType);
                motorcycle.setId(id);
                inventory[nv++] = motorcycle;
            }
            
            // Update nextId to be greater than any loaded ID
            if (id >= nextId) {
                nextId = id + 1;
            }
        }
    }

    /**
     * Find a vehicle by its ID
     *
     * @param id - the vehicle ID to search for
     * @return the vehicle with the specified ID, or null if not found
     */
    public Vehicle getVehicleFromId(int id) {
        for (int i = 0; i < nv; i++) {
            if (inventory[i] != null && inventory[i].id == id) {
                return inventory[i];
            }
        }
        return null;
    }

    /**
     * Count vehicles within a specified budget
     *
     * @param budget - the maximum price for vehicles
     * @return the number of vehicles within the budget
     */
    public int carBudget(double budget) {
        int total = 0; // declare the variable "total" here
        // method implementation...
        return total; // return the total number of cars within the budget
    }

    /**
     * Find cars within a specified budget
     *
     * @param budget - the maximum price for cars
     * @return array of cars within the specified budget
     */
    public Car[] carsWithinBudget(double budget) {
        List<Car> cars = new ArrayList<>();
        for (Vehicle vehicle : inventory) {
            if (vehicle instanceof Car && vehicle.getPrice() <= budget) {
                cars.add((Car) vehicle);
            }
        }
        return cars.toArray(new Car[0]);
    }

    /**
     * Generate a formatted string of the dealership's sales history
     * Loads sales directly from the database to ensure we're showing data from the current connection (test or real)
     *
     * @return formatted string containing the sales history
     */
    public String showSalesHistory() {
        StringBuilder string = new StringBuilder("Sales History:\n");
        
        try {
            // Load sales from the database to ensure we're using the right connection
            DBManager db = DBManager.getInstance();
            ResultSet rs = db.runQuery(
                "SELECT s.*, v.make, v.model, v.color, v.year, v.price, v.car_type, v.handlebar_type " +
                "FROM Sales s JOIN Vehicle v ON s.vehicle_id = v.vehicle_id " +
                "WHERE v.dealerships_id = ?",
                m_dealershipLayer.getDealershipId()
            );
            
            boolean hasSales = false;
            
            while (rs.next()) {
                hasSales = true;
                int vehicleId = rs.getInt("vehicle_id");
                String make = rs.getString("make");
                String model = rs.getString("model");
                String color = rs.getString("color");
                int year = rs.getInt("year");
                double price = rs.getDouble("price");
                String carType = rs.getString("car_type");
                String handlebarType = rs.getString("handlebar_type");
                
                Vehicle vehicle;
                if (carType != null) {
                    vehicle = new Car(make, model, color, year, price, carType);
                } else {
                    vehicle = new Motorcycle(make, model, color, year, price, handlebarType);
                }
                vehicle.setId(vehicleId);
                
                String buyerName = rs.getString("buyer_name");
                String buyerContact = rs.getString("buyer_contact");
                String saleDate = rs.getString("sale_date");
                
                string.append("-------------------\n")
                      .append(vehicle.toString()).append("\n")
                      .append("Buyer Name: ").append(buyerName).append("\n")
                      .append("Buyer Contact: ").append(buyerContact).append("\n")
                      .append("Sale Date: ").append(saleDate).append("\n");
            }
            
            if (!hasSales) {
                return "No sales recorded.";
            }
            
        } catch (SQLException e) {
            System.err.println("Error loading sales history: " + e.getMessage());
            e.printStackTrace();
            
            // Fallback to in-memory sales if DB access fails
            if (ns == 0) {
                return "No sales recorded.";
            }
            for (int i = 0; i < ns; i++) {
                string.append("-------------------\n")
                      .append(sales[i].getVehicle().toString()).append("\n")
                      .append("Buyer Name: ").append(sales[i].getBuyerName()).append("\n")
                      .append("Buyer Contact: ").append(sales[i].getBuyerContact()).append("\n")
                      .append("Sale Date: ").append(sales[i].getSaleDate()).append("\n");
            }
        }
        
        return string.append("-------------------\n").toString();
    }

    /**
     * Generate formatted information about the dealership for GUI display
     *
     * @return formatted string containing dealership information
     */
    public String getInfoGUI() {
        return "Dealership name: [" + name + "]\nLocation: [" + location + "]\nInventory Size: [" + inventory.length
                + "]\n\nAvailable space: " + (inventory.length - nv) + "\nTotal Cars: " + getTotalCars()
                + "\nTotal Motorcycles: " + getTotalMotorcycles() + "\n\nTotal sales profit: " + salesProfit()
                + "\nTotal vehicles sold: " + ns;
    }

    /**
     * Check if the dealership inventory is full
     *
     * @return true if the inventory is full, false otherwise
     */
    public boolean isFull() { return nv == inventory.length; }
    
    /**
     * Check if the dealership inventory is empty
     *
     * @return true if the inventory is empty, false otherwise
     */
    public boolean isEmpty() { return nv == 0; }
    
    /**
     * Count the total number of cars in the inventory
     *
     * @return the number of cars in the inventory
     */
    public int getTotalCars() {
        int total = 0;
        for (int i = 0; i < nv; i++) if (inventory[i] instanceof Car) total++;
        return total;
    }
    
    /**
     * Count the total number of motorcycles in the inventory
     *
     * @return the number of motorcycles in the inventory
     */
    public int getTotalMotorcycles() {
        int total = 0;
        for (int i = 0; i < nv; i++) if (inventory[i] instanceof Motorcycle) total++;
        return total;
    }

    /**
     * Search for cars of a specific type
     *
     * @param type - the car type to search for
     * @return array of cars matching the specified type
     */
    public Car[] searchCar(String type) {
        // Implement the logic to search for cars by type
        // For example:
        Car[] cars = new Car[nv];
        int count = 0;
        for (int i = 0; i < nv; i++) {
            if (inventory[i] instanceof Car && ((Car) inventory[i]).getType().equals(type)) {
                cars[count++] = (Car) inventory[i];
            }
        }
        Car[] result = new Car[count];
        System.arraycopy(cars, 0, result, 0, count);
        return result;
    }

    /**
     * Calculate the total profit from all sales
     *
     * @return the total sales profit
     */
    private int salesProfit() {
        int total = 0;
        for (int i = 0; i < ns; i++) total += sales[i].getVehicle().getPrice();
        return total;
    }

    /**
     * Retrieve all users from the database
     *
     * @return list of all users
     * @throws SQLException if a database access error occurs
     */
    public List<User> getUsers() throws SQLException {
        List<User> users = new ArrayList<>();
        DBManager db = DBManager.getInstance();
        ResultSet rs = db.runQuery("SELECT u.*, r.role_name FROM users u JOIN roles r ON u.role_id = r.role_id");
        while (rs.next()) {
            String role = rs.getString("role_name");
            String password = rs.getString("password");
            boolean isTempPassword = rs.getInt("is_temp_password") == 1;
            boolean isActive = rs.getInt("is_active") == 1;

            switch (role) {
                case "Admin":
                    users.add(new Admin(rs.getInt("user_id"), rs.getString("username"), password,
                            rs.getString("name"), rs.getString("email"), rs.getString("phone"), isTempPassword, isActive));
                    break;
                case "Manager":
                    users.add(new Manager(rs.getInt("user_id"), rs.getString("username"), password,
                            rs.getString("name"), rs.getString("email"), rs.getString("phone"), isTempPassword, isActive));
                    break;
                case "Salesperson":
                    users.add(new Salesperson(rs.getInt("user_id"), rs.getString("username"), password,
                            rs.getString("name"), rs.getString("email"), rs.getString("phone"), isTempPassword, isActive));
                    break;
            }
        }
        return users;
    }

    /**
     * Update a user's information in the database
     *
     * @param user - the user to be updated
     * @throws SQLException if a database access error occurs
     */
    public void updateUser(User user) throws SQLException {
        DBManager db = DBManager.getInstance();
        String query = "UPDATE users SET password = ?, name = ?, email = ?, phone = ?, is_active = ? WHERE user_id = ?";
        db.runUpdate(query, user.password, user.name, user.email, user.phone, user.isActive ? 1 : 0, user.getId());
    }

    public void addPasswordResetRequest(User user) throws SQLException {
                DBManager db = DBManager.getInstance();
                db.runInsert("INSERT INTO password_reset_requests (username, request_date) VALUES (?, ?)",
                            user.getUsername(), LocalDateTime.now().toString());
            }

    /**
     * Retrieve users who have requested a password reset
     *
     * @return list of users with password reset requests
     * @throws SQLException if a database access error occurs
     */
    public List<User> getPasswordResetRequests() throws SQLException, Exception {
			DBManager db = DBManager.getInstance();
			List<User> requests = new ArrayList<>();
			PreparedStatement stmt = null;
			ResultSet rs = null;
			try {
				stmt = db.Connection().prepareStatement("SELECT username FROM password_reset_requests");
				rs = stmt.executeQuery();
				while (rs.next()) {
					String username = rs.getString("username");
					User user = User.loadUser(username);
					if (user != null) {
						requests.add(user);
					}
				}
			} finally {
				if (rs != null) {
					rs.close();
				}
				if (stmt != null) {
					stmt.close();
				}
			}
			return requests;
		}
}
