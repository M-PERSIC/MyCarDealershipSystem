package persistance;

import java.io.*;
import java.nio.file.*;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Database manager for SQLite operations
 *
 * @author Ronika Patel (40156217)
 * @author Nazim Chaib Cherif-Baza (40017992)
 * @author Andrea Delgado Anderson (40315869)
 * @author Grace Pan (40302283)
 * @author Bao Tran Nguyen (40257379)
 * @author Michael Persico (40090861)
 * @since 1.8
 */
public class DBManager {

	private static DBManager m_dbManager;
	private String m_dbPath;
	private Connection m_connection;
	
	// Flag to track if we're in test mode
	private boolean isTestMode = false;
	
	// Test connection for in-memory database
	private Connection m_testConnection;

	/**
	 * Private constructor for the DBManager class
	 * Creates a database connection and initializes the database if needed
	 *
	 * @throws SQLException if a database access error occurs
	 */
	private DBManager() throws SQLException {
		// Use the local database file instead of home directory
		m_dbPath = "dealership.sqlite3";
		
		// Print the absolute path for debugging
		System.out.println("Database path: " + new File(m_dbPath).getAbsolutePath());

		initDB();
	}

	/**
	 * Execute an SQL insert statement with the provided parameters
	 *
	 * @param query - the SQL insert statement to execute
	 * @param params - variable number of parameters to replace placeholders in the query
	 * @throws SQLException if a database access error occurs
	 */
	public void runInsert(String query, Object... params) throws SQLException {
		System.out.println("Will run insert query: " + query + (isTestMode ? " [TEST MODE]" : ""));
		// Use the appropriate connection based on test mode status
		Connection conn = this.Connection();
		var stmt = conn.prepareStatement(query);
		for (int i = 0; i < params.length; i++) {
			stmt.setObject(i + 1, params[i]);
		}
		stmt.execute();
		conn.commit();
	}

	/**
	 * Execute an SQL query statement and return the result set
	 *
	 * @param query - the SQL query statement to execute
	 * @param params - variable number of parameters to replace placeholders in the query
	 * @return the ResultSet containing the query results
	 * @throws SQLException if a database access error occurs
	 */
	public ResultSet runQuery(String query, Object... params) throws SQLException {
		System.out.println("Will run query: " + query);
		// Debug parameter information
		System.out.println("Number of parameters: " + params.length);
		for (int i = 0; i < params.length; i++) {
			System.out.println("Parameter " + (i+1) + ": " + (params[i] == null ? "null" : "'" + params[i].toString() + "'"));
		}
		
		// Use the appropriate connection based on test mode status
		Connection conn = this.Connection();
		var stmt = conn.prepareStatement(query);
		for (int i = 0; i < params.length; i++) {
			stmt.setObject(i + 1, params[i]);
		}
		
		// Execute the query and debug results
		ResultSet rs = stmt.executeQuery();
		System.out.println("Query executed successfully");
		
		// Debug if the ResultSet has any rows
		if (rs.isBeforeFirst()) {
			System.out.println("Query returned at least one row");
		} else {
			System.out.println("Query returned no rows");
		}
		
		return rs;
	}




  public static String getSalespersonPerformanceReport() {
    StringBuilder report = new StringBuilder();
    report.append("Salesperson Performance (Last 12 Months)\n\n");

    String sql = """
        SELECT
            u.name AS salesperson_name,
            COUNT(s.sale_id) AS vehicles_sold,
            SUM(v.price) AS total_revenue,
            AVG(v.price) AS avg_price
        FROM Sales s
        JOIN users u ON s.user_id = u.user_id
        JOIN Vehicle v ON s.vehicle_id = v.vehicle_id
        WHERE s.sale_date >= date('now', '-12 months')
        GROUP BY s.user_id
        ORDER BY total_revenue DESC;
    """;

    try {
        ResultSet rs = getInstance().runQuery(sql);
        while (rs.next()) {
            String name = rs.getString("salesperson_name");
            int sold = rs.getInt("vehicles_sold");
            double revenue = rs.getDouble("total_revenue");
            double avg = rs.getDouble("avg_price");

            report.append(name).append("\n")
                  .append("---------------------\n")
                  .append("Total Vehicles Sold: ").append(sold).append("\n")
                  .append(String.format("Total Revenue: $%,.2f\n", revenue))
                  .append(String.format("Average Sale: $%,.2f\n", avg))
                  .append("\n");
        }
        rs.close();
    } catch (SQLException e) {
        e.printStackTrace();
        report.append("Error generating report.");
    }

    return report.toString();
  }

  public static String getModelSalesReport() {
    StringBuilder report = new StringBuilder();
    report.append("Model Sales (Last 12 Months)\n\n");

    String sql = """
        SELECT
            v.make || ' ' || v.model AS full_model_name,
            COUNT(s.sale_id) AS units_sold
        FROM Sales s
        JOIN Vehicle v ON s.vehicle_id = v.vehicle_id
        WHERE s.sale_date >= date('now', '-12 months')
        GROUP BY v.make, v.model
        ORDER BY units_sold DESC;
    """;

    try {
        ResultSet rs = getInstance().runQuery(sql);
        while (rs.next()) {
            String model = rs.getString("full_model_name");
            int count = rs.getInt("units_sold");

            report.append(model).append("\n")
                  .append("---------------------\n")
                  .append("Units Sold: ").append(count).append("\n\n");
        }
        rs.close();
    } catch (SQLException e) {
        e.printStackTrace();
        report.append("Error generating model sales report.");
    }

    return report.toString();
  }


	/**
	 * Execute an SQL update statement with the provided parameters
	 *
	 * @param query - the SQL update statement to execute
	 * @param params - variable number of parameters to replace placeholders in the query
	 * @throws SQLException if a database access error occurs
	 */
	public void runUpdate(String query, Object... params) throws SQLException {
		System.out.println("Will run update query: " + query + (isTestMode ? " [TEST MODE]" : ""));
		// Use the appropriate connection based on test mode status
		Connection conn = this.Connection();
		var stmt = conn.prepareStatement(query);
		for (int i = 0; i < params.length; i++) {
			stmt.setObject(i + 1, params[i]);
		}
		stmt.execute();
		conn.commit();
	}

	/**
	 * Initialize the database connection and create tables if the database doesn't exist
	 *
	 * @throws SQLException if a database access error occurs
	 */
	private void initDB() throws SQLException {
		var dbFile = new File(m_dbPath);
		var mustCreateTables = !dbFile.exists();

		var url = "jdbc:sqlite:" + m_dbPath;
		try {
			m_connection = DriverManager.getConnection(url);
			System.out.println("Connection to SQLite has been established.");
			m_connection.setAutoCommit(false);
		} catch (SQLException e) {
			System.out.println(e.getMessage());
		}

		if (!mustCreateTables) {
			System.out.println("DB file " + m_dbPath + " already exists. Not creating the database.");
		} else {
			System.out.println("Creating the DB file " + m_dbPath + " and the tables.");
			createTables();
		}
	}

	/**
	 * Create the database tables structure
	 * Sets up dealerships, users, roles, vehicles, and sales tables
	 *
	 * @throws SQLException if a database access error occurs
	 */
	private void createTables() throws SQLException {
		System.out.println("Creating the dealerships table");
		var dealershipSQL = "CREATE TABLE IF NOT EXISTS dealerships (id INTEGER PRIMARY KEY AUTOINCREMENT,"
				+ " name text NOT NULL, location text NOT NULL, capacity INTEGER);";

		var stmt = m_connection.createStatement();
		stmt.execute(dealershipSQL);


		// Modified users table schema
		var userSQL = "CREATE TABLE IF NOT EXISTS users (user_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
              "username TEXT UNIQUE NOT NULL, password TEXT NOT NULL, role_id INTEGER, " +
              "name TEXT NOT NULL, email TEXT, phone TEXT, is_active BOOLEAN DEFAULT TRUE, " +
              "join_date DATE DEFAULT CURRENT_DATE, FOREIGN KEY (role_id) REFERENCES roles(role_id));";

		stmt.execute(userSQL);

		// Modified roles table schema
		var roleSQL = "CREATE TABLE IF NOT EXISTS roles (role_id INTEGER PRIMARY KEY AUTOINCREMENT,"
		+ " role_name text NOT NULL);";

		stmt.execute(roleSQL);

		var addAdminRoleSQL = "INSERT INTO roles (role_name) VALUES ('Admin');";
		stmt.execute(addAdminRoleSQL);

		var addManagerRoleSQL = "INSERT INTO roles (role_name) VALUES ('Manager');";
		stmt.execute(addManagerRoleSQL);

		var addSalesPersonRoleSQL = "INSERT INTO roles (role_name) VALUES ('Salesperson');";
		stmt.execute(addSalesPersonRoleSQL);


		// Added Vehicles and Sales tables
		System.out.println("Creating the Vehicle table");
		stmt.execute("CREATE TABLE IF NOT EXISTS Vehicle (" +
					"vehicle_id INTEGER PRIMARY KEY AUTOINCREMENT, make TEXT NOT NULL, model TEXT NOT NULL, " +
					"color TEXT, year INTEGER, price REAL NOT NULL, type TEXT, handlebar_type TEXT, " +
					"car_type TEXT, is_sold BOOLEAN DEFAULT FALSE, dealerships_id INTEGER, " +
					"FOREIGN KEY (dealerships_id) REFERENCES dealerships(id))");
		System.out.println("Creating the Sales table");
		stmt.execute("CREATE TABLE IF NOT EXISTS Sales (" +
					"sale_id INTEGER PRIMARY KEY AUTOINCREMENT, vehicle_id INTEGER NOT NULL, " +
					"user_id INTEGER NOT NULL, buyer_name TEXT, buyer_contact TEXT, " +
					"sale_date DATETIME DEFAULT CURRENT_TIMESTAMP, " +
					"FOREIGN KEY (vehicle_id) REFERENCES Vehicle(vehicle_id), " +
					"FOREIGN KEY (user_id) REFERENCES users(user_id))");
		System.out.println("Creating the password_reset_requests table");
		stmt.execute("CREATE TABLE IF NOT EXISTS password_reset_requests (" +
					"id INTEGER PRIMARY KEY AUTOINCREMENT, " +
					"username TEXT NOT NULL, " +
					"request_date TEXT NOT NULL)");	
		m_connection.commit();
	}

	/**
	 * Get the singleton instance of the DBManager
	 * Creates a new instance if one doesn't exist
	 *
	 * @return the singleton DBManager instance
	 * @throws SQLException if a database access error occurs
	 */
	public static DBManager getInstance() throws SQLException {
		if (m_dbManager == null) {
			m_dbManager = new DBManager();
		}
		return m_dbManager;
	}

	/**
	 * Get the database connection
	 * Returns test connection if in test mode, otherwise returns regular connection
	 *
	 * @return the Connection object for the database
	 */
	public Connection Connection() throws SQLException {
		return isTestMode ? m_testConnection : m_connection;
	}
	
	/**
	 * Check if the system is currently in test mode
	 *
	 * @return true if in test mode, false otherwise
	 */
	public boolean isInTestMode() {
		return isTestMode;
	}

	/**
	 * Enter test mode with an in-memory database
	 * Creates a temporary database with the same schema as the real database
	 * and populates it with sample test data
	 *
	 * @throws SQLException if a database access error occurs
	 */
	public void enterTestMode() throws SQLException {
		if (isTestMode) {
			System.out.println("Already in test mode");
			return; // Already in test mode
		}
		
		System.out.println("Entering test mode with in-memory database");
		
		// Create in-memory database
		m_testConnection = DriverManager.getConnection("jdbc:sqlite::memory:");
		m_testConnection.setAutoCommit(false);
		
		// Copy schema from real database to in-memory database
		copySchemaToTestDB();
		
		// Generate sample test data
		generateTestData();
		
		isTestMode = true;
		System.out.println("Test mode activated successfully");
	}
	
	/**
	 * Exit test mode and discard all changes made in the test database
	 *
	 * @throws SQLException if a database access error occurs
	 */
	public void exitTestMode() throws SQLException {
		if (!isTestMode) {
			System.out.println("Not in test mode");
			return; // Not in test mode
		}
		
		System.out.println("Exiting test mode");
		
		// Close test connection (this will discard the in-memory database)
		if (m_testConnection != null && !m_testConnection.isClosed()) {
			m_testConnection.close();
			m_testConnection = null;
		}
		
		isTestMode = false;
		System.out.println("Test mode deactivated, all test data discarded");
	}
	
	/**
	 * Copy schema from the real database to the test in-memory database
	 * 
	 * @throws SQLException if a database access error occurs
	 */
	private void copySchemaToTestDB() throws SQLException {
		System.out.println("Copying database schema to test database");
		
		// Get the schema from the main database
		ResultSet tables = m_connection.getMetaData().getTables(null, null, null, new String[]{"TABLE"});
		
		while (tables.next()) {
			String tableName = tables.getString("TABLE_NAME");
			
			// Skip system tables
			if (tableName.startsWith("sqlite_")) {
				continue;
			}
			
			System.out.println("Copying table structure: " + tableName);
			
			// Get the CREATE TABLE statement
			ResultSet rs = m_connection.createStatement().executeQuery(
					"SELECT sql FROM sqlite_master WHERE type='table' AND name='" + tableName + "'");
			
			if (rs.next()) {
				String createTableSQL = rs.getString("sql");
				if (createTableSQL != null) {
					// Create the table in the test database
					m_testConnection.createStatement().execute(createTableSQL);
				}
			}
		}
		
		m_testConnection.commit();
		System.out.println("Schema copied successfully");
	}
	
	/**
	 * Generate test data for the in-memory database
	 * 
	 * @throws SQLException if a database access error occurs
	 */
	private void generateTestData() throws SQLException {
		System.out.println("Generating test data");
		
		// Create test admin
		m_testConnection.createStatement().execute(
				"INSERT INTO users (username, password, role_id, name, email, phone, is_active, is_temp_password) " +
				"VALUES ('testadmin', 'test123', 1, 'Test Admin', 'testadmin@example.com', '555-000-0000', 1, 0)");
		
		// Create test manager
		m_testConnection.createStatement().execute(
				"INSERT INTO users (username, password, role_id, name, email, phone, is_active, is_temp_password) " +
				"VALUES ('testmanager', 'test123', 2, 'Test Manager', 'testmanager@example.com', '555-000-0001', 1, 0)");
		
		// Create test salesperson
		m_testConnection.createStatement().execute(
				"INSERT INTO users (username, password, role_id, name, email, phone, is_active, is_temp_password) " +
				"VALUES ('testsales', 'test123', 3, 'Test Salesperson', 'testsales@example.com', '555-000-0002', 1, 0)");
		
		// Create test dealership
		m_testConnection.createStatement().execute(
				"INSERT INTO dealerships (name, location, capacity) VALUES ('Test Dealership', 'Test Location', 50)");
		
		// Create sample vehicles (cars and motorcycles)
		// Cars
		m_testConnection.createStatement().execute(
				"INSERT INTO Vehicle (make, model, color, year, price, car_type, dealerships_id) " +
				"VALUES ('Honda', 'Civic', 'Red', 2022, 25000, 'Sedan', 1)");
		m_testConnection.createStatement().execute(
				"INSERT INTO Vehicle (make, model, color, year, price, car_type, dealerships_id) " +
				"VALUES ('Toyota', 'Camry', 'Blue', 2021, 30000, 'Sedan', 1)");
		m_testConnection.createStatement().execute(
				"INSERT INTO Vehicle (make, model, color, year, price, car_type, dealerships_id) " +
				"VALUES ('Ford', 'F-150', 'Black', 2023, 45000, 'Truck', 1)");
		
		// Motorcycles
		m_testConnection.createStatement().execute(
				"INSERT INTO Vehicle (make, model, color, year, price, handlebar_type, dealerships_id) " +
				"VALUES ('Harley-Davidson', 'Street 750', 'Black', 2022, 8000, 'Cruiser', 1)");
		m_testConnection.createStatement().execute(
				"INSERT INTO Vehicle (make, model, color, year, price, handlebar_type, dealerships_id) " +
				"VALUES ('Yamaha', 'YZF R1', 'Blue', 2023, 12000, 'Sport', 1)");
		
		// Create sample sales
		m_testConnection.createStatement().execute(
				"INSERT INTO Sales (vehicle_id, user_id, buyer_name, buyer_contact, sale_date) " +
				"VALUES (2, 3, 'John Doe', 'john@example.com', '2023-01-15 14:30:00')");
		
		m_testConnection.commit();
		System.out.println("Test data generation complete");
	}
}