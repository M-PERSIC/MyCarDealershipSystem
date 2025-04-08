# Windows Setup and Compatibility Fix

## Database Path Fix (New in this branch)

This branch contains a fix for an issue where the SQLite database file (`dealership.sqlite3`) could not be found on Windows systems, requiring a new database to be created instead of using the existing one.

### Problem

On Windows, using a relative path like `"dealership.sqlite3"` can sometimes fail to be resolved consistently, depending on how the Java application is launched. This caused the application to behave differently on Windows versus Linux.

### Solution

The fix modifies the `DBManager` class to use an absolute path that is constructed at runtime:

```java
// Get the application's root directory
File currentDir = new File(System.getProperty("user.dir"));
m_dbPath = new File(currentDir, "dealership.sqlite3").getAbsolutePath();
```

This approach is cross-platform because:

1. It uses `System.getProperty("user.dir")` to get the current working directory as an absolute path
2. It uses `File.getAbsolutePath()` which handles platform-specific path separators (`\` on Windows, `/` on Linux)
3. It uses Java's `File` class to properly join paths in a platform-independent way

The code also includes a fallback mechanism to use the original relative path approach if there's an error determining the absolute path.

### Testing

This fix should be tested on both Windows and Linux systems to ensure that:

1. The database file is correctly found on both platforms
2. The application launches directly to the login page on both platforms
3. No new database is created unnecessarily

## Original Windows Setup Instructions

If you're still experiencing issues with the login screen not appearing on Windows, follow these steps:

### Option 1: Run the SQL script to add a dealership

1. Navigate to your project directory in Command Prompt:
   ```
   cd C:\path\to\MyCarDealershipSystem
   ```

2. Run the following command to add a default dealership to your database:
   ```
   sqlite3 dealership.sqlite3 < setup_dealership.sql
   ```

3. Compile and run the application:
   ```
   javac -cp ".;libs\sqlite-jdbc-3.49.1.0.jar" src\carDealership\*.java src\persistance\*.java
   java -cp ".;libs\sqlite-jdbc-3.49.1.0.jar" carDealership.Main
   ```

### Option 2: Create a dealership through the UI

Alternatively, if you see the "Dealership Setup" screen:

1. Enter valid dealership information:
   - Dealership Name: Any name you prefer
   - Location: Any location
   - Inventory Capacity: A number between 1-100

2. Click "Go" to create the dealership

3. You should now see the login screen where you can log in with one of these accounts:
   - Username: Ronika, Password: 123 (Admin)
   - Username: Max, Password: 456 (Manager)
   - Username: Jessica, Password: 789 (Salesperson)

## Issue explanation

The problem occurs because:
1. The application expects to find a dealership record in the database
2. If no dealership record exists, it shows the setup screen
3. After creating a dealership, it should show the login screen

The fixes we've made ensure that:
1. You can easily add a dealership through the SQL script
2. After creating a dealership, the app will correctly show the login screen