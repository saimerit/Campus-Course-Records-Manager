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

To compile and run the CCRM application from the command line, you will need to have the following installed on your system:

* **Java Development Kit (JDK)**: Version 11 or higher.  
* **Oracle Database**: The application is configured to connect to an Oracle database.

### **Steps to Run the Application:**

1. **Clone the Repository**:  
   git clone https://github.com/saimerit/Campus-Course-Records-Manager.git

2. Compile the Application:  
   From the root directory of the project, run the following command. This compiles all source files and places the .class files in the bin directory, including the Oracle JDBC driver in the classpath.  
   ```bash
   javac -d bin -cp "lib/ojdbc17.jar" src/edu/ccrm/cli/*.java src/edu/ccrm/config/*.java src/edu/ccrm/domain/*.java src/edu/ccrm/exception/*.java src/edu/ccrm/io/*.java src/edu/ccrm/service/*.java src/edu/ccrm/util/*.java
    ```
3. **Set up the Database**:  
   * Ensure your Oracle database is running.  
   * Follow the instructions in the "Database Setup" section below to create the necessary user. The application will create the tables automatically on first run.  
4. Run the Application:  
   Once compiled, run the application with this command, ensuring the bin folder and the JDBC driver are on the classpath.  
   ```bash
   java -cp "bin;lib/ojdbc17.jar" edu.ccrm.cli.Main
    ```
The application will then start, and you will see the main menu displayed in the console.

## **3\. Database Setup**

This application requires an Oracle database.

### **Creating the CCRM User**

1. **Connect to your database** as a user with administrative privileges (e.g., SYSTEM).  
2. **Create the ccrm\_user**: Execute the following SQL commands:  
    ```SQL
   CREATE USER ccrm_user IDENTIFIED BY ccrm_pass;  
   GRANT CONNECT, RESOURCE, DBA TO ccrm_user;
    ```
   This creates a user named ccrm_user with the password ccrm_pass.

### **Initializing the Schema**

The application handles schema creation automatically. The first time you run the Main.java application, it will detect that the tables are missing and execute the database\_setup.sql script to create them.

## **4\. Evolution of Java**

* **1995**: Java is publicly announced by Sun Microsystems.  
* **1996**: JDK 1.0 is released.  
* **2004**: Java SE 5.0 (initially numbered 1.5) is released, introducing major features like generics, annotations, and autoboxing.  
* **2014**: Java SE 8 is released, with the most significant changes being the introduction of Lambda expressions and the Stream API.  
* **2018**: Oracle introduces a new 6-month release cadence, starting with Java SE 10\.  
* **2021**: Java SE 17 is released as the latest Long-Term Support (LTS) version.

## **5\. Java ME vs. SE vs. EE**

| Feature | Java ME (Micro Edition) | Java SE (Standard Edition) | Java EE (Enterprise Edition) |
| :---- | :---- | :---- | :---- |
| **Primary Use** | Mobile devices, embedded systems, and other resource-constrained devices. | Desktop applications, servers, and console applications. | Large-scale, distributed, and web-based enterprise applications. |
| **APIs** | A subset of Java SE APIs, with additional libraries for mobile development. | The core Java platform, including the JVM, core libraries, and development tools. | A superset of Java SE, with additional APIs for enterprise features like servlets and JSPs. |
| **Target Audience** | Developers for mobile phones and embedded devices. | General-purpose Java developers. | Enterprise application developers. |

## **6\. JDK vs. JRE vs. JVM**

* **JVM (Java Virtual Machine)**: An abstract machine that provides a runtime environment in which Java bytecode can be executed. It is platform-dependent.  
* **JRE (Java Runtime Environment)**: A software package that contains the JVM, necessary libraries, and other components to *run* Java applications.  
* **JDK (Java Development Kit)**: A superset of the JRE that includes everything needed to *develop* Java applications, including the compiler (javac), debugger, and other tools.

## **7\. IDE Setup**

While the application is designed to be run from the command line, you can also set it up in an IDE for development.

### **Eclipse IDE Setup**

1. **Import Project**: File \> Import \> General \> Existing Projects into Workspace.  
2. **Add JDBC Driver**: Right-click the project \> Build Path \> Configure Build Path. Go to the Libraries tab, select Classpath, click Add JARs..., and add the lib/ojdbc17.jar file.

### **Visual Studio Code Setup**

1. **Install Extension**: Install the "Extension Pack for Java" from Microsoft.  
2. **Open Project**: Go to File \> Open Folder... and select the project's root directory.  
3. **Add JDBC Driver**: In the "JAVA PROJECTS" explorer view, find "Referenced Libraries," click the \+ icon, and add the lib/ojdbc17.jar file.

## **8\. Mapping of Syllabus Topics to Project Files**

| Syllabus Topic | File/Class/Method Where Demonstrated |
| :---- | :---- |
| **Encapsulation** | Student.java, Course.java (private fields with public getters/setters) |
| **Inheritance** | Person.java (abstract base class for Student and Instructor) |
| **Polymorphism** | TranscriptService.java (using Person references) |
| **Abstraction** | Person.java (abstract class with abstract methods) |
| **Interfaces** | Searchable.java (functional interface for searching) |
| **NIO.2 and Streams** | ImportExportService.java, BackupService.java (for file I/O) |
| **Lambda Expressions** | CourseService.java (used for filtering courses with predicates) |
| **Singleton Design Pattern** | AppConfig.java (ensuring a single configuration instance) |
| **Builder Design Pattern** | Course.java (nested Builder class for object construction) |
| **Custom Exceptions** | DuplicateEnrollmentException.java, MaxCreditLimitExceededException.java |

## **9\. Enabling Assertions**

To enable assertions when running from the command line, use the \-ea (or \-enableassertions) flag:
```bash
java -ea -cp "bin;lib/ojdbc17.jar" edu.ccrm.cli.Main
```
In an IDE, add \-ea to the "VM arguments" in your run configuration.

## **10\. Acknowledgements**

* This project was created as part of the "Programming in Java" course on the Vityarthi portal.  
* All code is original and written by Sai Ardhendu Kalivarapu.
