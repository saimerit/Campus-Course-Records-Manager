# **Campus Course & Records Manager (CCRM)**

## **1\. Introduction**

Welcome to the **Campus Course & Records Manager (CCRM)**, a robust, console-driven Java application engineered to simplify the complexities of academic administration. CCRM is a comprehensive solution for managing students, courses, enrollments, and academic records within an educational institution. Developed entirely with Java SE and backed by an Oracle database, this project provides a powerful yet intuitive command-line interface (CLI) for administrators.

CCRM empowers administrators with a suite of tools to manage the academic lifecycle seamlessly:

* **Student Administration**: Effortlessly add, list, and update student information and academic standing.  
* **Course Coordination**: Create, list, and modify courses, and assign instructors with ease.  
* **Enrollment Oversight**: Manage student course registrations, including the enforcement of academic regulations like credit limits.  
* **Academic Records**: Input student grades and generate detailed academic transcripts.  
* **Data Management**: Facilitate data import and export using the CSV format and create timestamped backups of all application data.

This project is a testament to a solid foundation in core Java principles, object-oriented programming (OOP), modern I/O with NIO.2, and JDBC for database interaction, all while applying fundamental design patterns.

## **2\. Getting Started**

To compile and execute the CCRM application, ensure your system meets the following prerequisites:

* **Java Development Kit (JDK)**: Version 11 or a more recent release.  
* **Oracle Database**: The application is pre-configured to connect to an Oracle database.

### **Execution Instructions:**

1. **Obtain the Source Code**:
   ```bash
   git clone https://github.com/saimerit/Campus-Course-Records-Manager.git
   ```
3. Compile the Application:  
   Navigate to the project's root directory and execute the following command. This will compile all source files and place the generated .class files into the bin directory, ensuring the Oracle JDBC driver is included in the classpath.
   ```bash
   javac \-d bin \-cp "lib/ojdbc17.jar" src/edu/ccrm/cli/\*.java src/edu/ccrm/config/\*.java src/edu/ccrm/domain/\*.java src/edu/ccrm/exception/\*.java src/edu/ccrm/io/\*.java src/edu/ccrm/service/\*.java src/edu/ccrm/util/\*.java
   ```
4. **Configure the Database**:  
   * Confirm that your Oracle database instance is active.  
   * Refer to the "Database Configuration" section for instructions on creating the necessary user. The application will automatically handle table creation upon its initial launch.  
5. Launch the Application:  
   With the compilation complete, run the application using this command, making sure to include the bin folder and the JDBC driver in the classpath.
   ```bash
   java \-cp "bin;lib/ojdbc17.jar" edu.ccrm.cli.Main
   ```
   Upon successful execution, the application will initialize, and the main menu will be presented in your console.

## **3\. Database Configuration**

An Oracle database is required for this application.

### **User Creation**

1. **Establish a connection** to your database with a user account that has administrative privileges (e.g., SYSTEM).  
2. **Create the ccrm\_user**: Run the following SQL statements:
   ```SQL
   CREATE USER ccrm\_user IDENTIFIED BY ccrm\_pass;  
   GRANT CONNECT, RESOURCE, DBA TO ccrm\_user;
   ```
   This will create a new user named ccrm\_user with the password ccrm\_pass.

### **Schema Initialization**

The application automates the creation of the database schema. When you first run the Main.java application, it will detect the absence of the required tables and execute the database\_setup.sql script to create them.

## **4\. The Evolution of Java**

* **1995**: Sun Microsystems officially announces the Java programming language.  
* **1996**: The first version of the Java Development Kit (JDK 1.0) is released.  
* **2004**: The release of Java SE 5.0 (originally versioned 1.5) introduces significant enhancements such as generics, annotations, and autoboxing.  
* **2014**: Java SE 8 is launched, bringing about major changes with the introduction of Lambda expressions and the Stream API.  
* **2018**: Oracle implements a new six-month release cycle, beginning with Java SE 10\.  
* **2021**: Java SE 17 is released, becoming the latest version with Long-Term Support (LTS).

## **5\. A Comparison of Java Editions: ME, SE, and EE**

| Aspect | Java ME (Micro Edition) | Java SE (Standard Edition) | Java EE (Enterprise Edition) |
| :---- | :---- | :---- | :---- |
| **Primary Application** | Designed for mobile devices, embedded systems, and other resource-limited environments. | The foundation for desktop applications, servers, and console-based programs. | Tailored for large-scale, distributed, and web-centric enterprise applications. |
| **APIs** | Includes a subset of Java SE APIs, supplemented with libraries for mobile development. | Comprises the core Java platform, including the JVM, standard libraries, and development tools. | An extension of Java SE, offering additional APIs for enterprise functionalities like servlets and JSPs. |
| **Intended Audience** | Developers creating applications for mobile phones and embedded devices. | Java developers working on a wide range of general-purpose applications. | Developers focused on building robust, enterprise-grade applications. |

## **6\. Understanding the Java Ecosystem: JDK, JRE, and JVM**

* **JVM (Java Virtual Machine)**: The JVM is an abstract computing machine that provides the runtime environment for executing Java bytecode. It is platform-specific, allowing Java programs to be "write once, run anywhere."  
* **JRE (Java Runtime Environment)**: The JRE is a software package that includes the JVM, along with the necessary libraries and other components required to *run* Java applications.  
* **JDK (Java Development Kit)**: The JDK is a superset of the JRE, providing all the tools necessary to *develop* Java applications, such as the compiler (javac), a debugger, and other development utilities.

## **7\. Integrated Development Environment (IDE) Setup**

Although the application is designed for command-line execution, you can also configure it within an IDE for a more streamlined development experience.

### **Eclipse IDE**

1. **Import the Project**: Navigate to File \> Import \> General \> Existing Projects into Workspace.  
2. **Incorporate the JDBC Driver**: Right-click on the project, then go to Build Path \> Configure Build Path. In the Libraries tab, select Classpath, click Add JARs..., and include the lib/ojdbc17.jar file.

### **Visual Studio Code**

1. **Install the Necessary Extension**: Install the "Extension Pack for Java" from Microsoft.  
2. **Open the Project**: Go to File \> Open Folder... and select the root directory of the project.  
3. **Add the JDBC Driver**: In the "JAVA PROJECTS" explorer, locate "Referenced Libraries," click the \+ icon, and add the lib/ojdbc17.jar file.

## **8\. Project-Syllabus Mapping**

| Syllabus Concept | Demonstrated in File/Class/Method |
| :---- | :---- |
| **Encapsulation** | Student.java, Course.java (private fields with public accessors) |
| **Inheritance** | Person.java (serves as an abstract base class for Student and Instructor) |
| **Polymorphism** | TranscriptService.java (utilizes Person references) |
| **Abstraction** | Person.java (an abstract class with abstract methods) |
| **Interfaces** | Searchable.java (a functional interface for search operations) |
| **NIO.2 and Streams** | ImportExportService.java, BackupService.java (for file I/O) |
| **Lambda Expressions** | CourseService.java (employed for filtering courses with predicates) |
| **Singleton Design Pattern** | AppConfig.java (ensures a single instance of the configuration) |
| **Builder Design Pattern** | Course.java (a nested Builder class for object creation) |
| **Custom Exceptions** | DuplicateEnrollmentException.java, MaxCreditLimitExceededException.java |

## **9\. Activating Assertions**

To enable assertions during command-line execution, use the \-ea (or \-enableassertions) flag:
```bash
java \-ea \-cp "bin;lib/ojdbc17.jar" edu.ccrm.cli.Main
```
Within an IDE, add \-ea to the "VM arguments" section of your run configuration.

## **10\. Credits**

This project was originally created by Sai Ardhendu Kalivarapu as part of the "Programming in Java" course on the Vityarthi portal.
