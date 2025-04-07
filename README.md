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

## Project Structure

- **Core Classes**
  - `Vehicle`: Abstract base class for all vehicles
  - `Car`: Extends Vehicle with car-specific attributes
  - `Motorcycle`: Extends Vehicle with motorcycle-specific attributes
  - `User`: Abstract base class for all user types
  - `Admin`, `Manager`, `Salesperson`: User role implementations
  - `Dealership`: Core business logic for dealership operations
  - `Sale`: Transaction record for vehicle sales

- **UI Components**
  - `Frame`: Main application window
  - `LoginFrame`: Authentication interface
  - `FirstLaunchPage`: Initial setup wizard
  - `VehicleMenu`: Vehicle management interface
  
- **Persistence Layer**
  - `DBManager`: SQLite database connection management
  - `DealershipLayer`: Data access for dealership operations

## Installation

### Prerequisites
- Java Development Kit (JDK) 17 or higher recommended
- SQLite JDBC driver (included in the `libs` directory)

### Setup
1. Clone the repository
2. Ensure the SQLite JDBC driver is in the classpath
3. Compile the application:
   ```bash
   javac -cp libs/sqlite-jdbc-3.49.1.0.jar -d bin src/carDealership/*.java src/persistance/*.java
   ```
4. Run the application:
   ```bash
   java -cp bin:libs/sqlite-jdbc-3.49.1.0.jar carDealership.Main
   ```
   On Windows use:
   ```bash
   java -cp bin;libs/sqlite-jdbc-3.49.1.0.jar carDealership.Main
   ```

## Team Members

Code contributions from:

- Nazim Chaib Cherif-Baza (nbazc)
- [Andrea Delgado Anderson](https://github.com/andremagda)
- [Ronika Patel](https://github.com/RonikaP)
- [Michael Persico](https://github.com/M-PERSIC)

With input from:

- Grace Pan
- Bao Tran Nguyen

Based on the work of previous members:

- [Mohanad Hafez](https://github.com/mohanad-hafez)
- [Faris Al Zahrani](https://github.com/nxrzs)
- [Hisham Saydawi](https://github.com/xAGS1)

## Possible Improvements

- Enhance user interface design for better user experience.
- Implement user authentication and authorization for secure access.
- Utilize a linked list data structure instead of an array for the inventory to enable dynamic resizing without specifying a fixed size.
