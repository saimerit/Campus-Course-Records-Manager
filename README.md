# **Campus Course & Records Manager (CCRM)**

## **1\. Project Overview**

The **Campus Course & Records Manager (CCRM)** is a comprehensive, console-based Java application designed to streamline the management of students, courses, enrollments, and academic records for an educational institution. This project is built entirely in Java SE and connects to an Oracle database for data persistence.

The CCRM application provides a user-friendly command-line interface (CLI) for administrators to perform a wide range of tasks, including:

* **Student Management**: Add, list, update student records and academic status.  
* **Course Management**: Create, list, update courses, and assign instructors.  
* **Enrollment Management**: Enroll and unenroll students in courses, with enforcement of academic rules like credit limits.  
* **Grading and Transcripts**: Record student grades and generate academic transcripts.  
* **File Operations**: Import and export data in CSV format, and create timestamped backups of all application data.

This project showcases a deep understanding of core Java principles, object-oriented programming (OOP), modern I/O with NIO.2, JDBC for database connectivity, and the application of fundamental design patterns.

## **2\. How to Run**

To compile and run the CCRM application, you will need to have the following installed on your system:

* **Java Development Kit (JDK)**: Version 11 or higher.  
* **Oracle Database**: The application is configured to connect to an Oracle database (like Oracle 10g XE or later).

### **Steps to Run the Application from the Command Line:**

1. **Clone the Repository**:  
   git clone \[https://github.com/saimerit/Campus-Course-Records-Manager.git\](https://github.com/saimerit/Campus-Course-Records-Manager.git)  
   cd Campus-Course-Records-Manager

2. Compile the Application:  
   From the root directory of the project, run the following command to compile all the Java source files:  
   javac \-d bin \-cp "lib/ojdbc17.jar" src/edu/ccrm/cli/\*.java src/edu/ccrm/config/\*.java src/edu/ccrm/domain/\*.java src/edu/ccrm/exception/\*.java src/edu/ccrm/io/\*.java src/edu/ccrm/service/\*.java src/edu/ccrm/util/\*.java

3. **Set up the Database**:  
   * Ensure your Oracle database is running.  
   * Follow the instructions in the "Database Setup" section below to create the necessary user and tables.  
4. Run the Application:  
   Once the code is compiled, run the application with this command:  
   java \-cp "bin;lib/ojdbc17.jar" edu.ccrm.cli.Main

The application will then start, and you will see the main menu displayed in the console.

## **3\. Database Setup**

This application requires an Oracle database. Follow these steps to set it up:

### **Creating the CCRM User**

1. **Connect to your database** as a user with administrative privileges (e.g., SYSTEM).  
2. **Create the ccrm\_user** by executing the following SQL commands:  
   CREATE USER ccrm\_user IDENTIFIED BY ccrm\_pass;  
   GRANT CONNECT, RESOURCE, DBA TO ccrm\_user;

   This creates a user named ccrm\_user with the password ccrm\_pass.

### **Initializing the Schema**

The application can initialize the database schema for you. The first time you run the Main.java application, it will detect that the tables are missing and execute the database\_setup.sql script to create the STUDENTS, INSTRUCTORS, COURSES, and ENROLLMENTS tables.

## **4\. Project Structure and Key Technologies**

| Category | File/Class/Technology Used |
| :---- | :---- |
| **Core Logic & Domain** | Student.java, Course.java, Instructor.java, Enrollment.java |
| **Database Connectivity** | java.sql (JDBC), ojdbc17.jar, DatabaseManager.java |
| **Service Layer** | StudentService.java, CourseService.java, etc. (Handles business logic) |
| **Command-Line Interface** | Main.java (Handles user input and displays menus) |
| **File I/O & Backups** | java.nio.file, java.util.zip, ImportExportService.java, BackupService.java |
| **Configuration** | AppConfig.java (Singleton pattern for managing file paths) |
| **Exception Handling** | Custom exceptions like RecordNotFoundException.java |
| **Design Patterns** | Singleton (AppConfig), Builder (Course.java) |

## **5\. Acknowledgements**

* This project was created as part of the "Programming in Java" course on the VitYarthi portal.  
* All code is original and written by Sai Ardhendu Kalivarapu.