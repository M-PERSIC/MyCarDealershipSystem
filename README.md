# Car Dealership Management System

This project is a fork of [Car Dealership System](https://github.com/mohanad-hafez/car-dealership-system.git), adapted and enhanced for educational purposes.

A comprehensive Java application for managing vehicle inventory, sales, and user accounts in a car dealership.

## Project Overview

The Car Dealership Management System is a Java-based application that provides a complete solution for dealership operations. It features a user-friendly GUI, robust data persistence using SQLite, and a multi-user role-based access control system for secure dealership management.

## Features

- **User Authentication & Authorization**
  - Role-based access control (Admin, Manager, Salesperson)
  - Account lockout protection for system security
  - Password management and reset functionality
  - Test mode for safe system exploration
  
- **Inventory Management**
  - Add, edit, and remove vehicles (cars and motorcycles)
  - Display complete inventory with filtering options
  - Search vehicles by make, model, year, and price range
  
- **Sales Processing**
  - Record and track vehicle sales transactions
  - Maintain comprehensive sales history
  - Customer information management
  
- **Administration Tools**
  - User account management
  - Dealership information configuration
  - Test mode for risk-free feature testing

## Installation

### Prerequisites
- Java Development Kit (JDK) 17 or higher recommended
- SQLite JDBC driver (included in the `libs` directory)

### Setup on Linux
1. Clone the repository
   ```bash
   git clone https://github.com/RonikaP/MyCarDealershipSystem.git
   cd MyCarDealershipSystem
   ```

2. Ensure the SQLite JDBC driver is in the classpath
   ```bash
   # The driver should already be in the libs directory
   ls -la libs/
   ```

3. Create bin directory if it doesn't exist
   ```bash
   mkdir -p bin
   ```

4. Compile the application
   ```bash
   javac -cp libs/sqlite-jdbc-3.49.1.0.jar -d bin src/carDealership/*.java src/persistance/*.java
   ```

5. Run the application
   ```bash
   java -cp bin:libs/sqlite-jdbc-3.49.1.0.jar carDealership.Main
   ```

### Setup on Windows
1. Open Command Prompt or PowerShell in the project root directory

2. Create bin directory if it doesn't exist
   ```
   mkdir bin
   ```

3. Compile the application
   ```
   javac -cp ".;libs\sqlite-jdbc-3.49.1.0.jar" -d bin src\carDealership\*.java src\persistance\*.java
   ```

4. Run the application
   ```
   java -cp "bin;libs\sqlite-jdbc-3.49.1.0.jar" carDealership.Main
   ```

5. If the application shows the Dealership Setup screen instead of the login screen,
   you need to run the setup script to create a dealership:
   ```
   sqlite3 dealership.sqlite3 < setup_dealership.sql
   ```
   Then restart the application.

## Team Members

- Nazim Chaib Cherif-Baza (nbazc)
- Andrea Delgado Anderson
- Ronika Patel
- Grace Pan
- Bao Tran Nguyen
- Michael Persico

Based on the work of previous members:

- [Mohanad Hafez](https://github.com/mohanad-hafez)
- [Faris Al Zahrani](https://github.com/nxrzs)
- [Hisham Saydawi](https://github.com/xAGS1)
