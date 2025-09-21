# Campus Course & Records Manager (CCRM)

## 1. Project Overview

The **Campus Course & Records Manager (CCRM)** is a comprehensive, console-based Java application designed to streamline the management of students, courses, enrollments, and academic records for an educational institution. This project is built entirely in Java SE and is intended to run locally in a console environment.

The CCRM application provides a user-friendly command-line interface (CLI) for administrators to perform a wide range of tasks, including:
* **Student Management**: Add, list, update, and deactivate student records.
* **Course Management**: Create, list, update, and deactivate courses.
* **Enrollment Management**: Enroll and unenroll students in courses, with enforcement of academic rules.
* **Grading and Transcripts**: Record student marks, calculate GPAs, and generate academic transcripts.
* **File Operations**: Import and export data in CSV format, and create timestamped backups of all application data.

This project showcases a deep understanding of core Java principles, object-oriented programming (OOP), modern I/O with NIO.2, and the application of fundamental design patterns.

***

## 2. How to Run

To run the CCRM application, you will need to have the following installed on your system:

* **Java Development Kit (JDK)**: Version 11 or higher.
* **Eclipse IDE for Java Developers**: The project is configured to be built and run using Eclipse.
* **Oracle 10g XE Database**: The application uses an Oracle database to store its data.

### Steps to Run the Application:

1.  **Clone the Repository**:
    ```bash
    git clone https://github.com/saimerit/Campus-Course-Records-Manager.git
    ```

2.  **Open in Eclipse IDE**:
    * Launch Eclipse IDE.
    * Go to `File > Import`.
    * Select `General > Existing Projects into Workspace`.
    * Browse to the directory where you cloned the repository and click `Finish`.

3.  **Set up the Database**:
    * Follow the instructions in the "Database Setup (Oracle 10g XE)" section below to install and configure the database.

4.  **Run the Main Class**:
    * In the "Package Explorer" in Eclipse, navigate to the `src` folder.
    * Find the `Main.java` class in the `edu.ccrm.cli` package.
    * Right-click on `Main.java` and select `Run As > Java Application`.

The application will then start, and you will see the main menu displayed in the console.

***

## 3. Database Setup (Oracle 10g XE)

This application requires an Oracle 10g XE database to be running. Follow these steps to set it up:

### Installing Oracle 10g XE on Windows

1.  **Download Oracle 10g XE**: Since Oracle 10g XE is an older version, you will need to find a reliable source to download the installer. You may need to create a free Oracle account to access the download.
2.  **Run the Installer**: Double-click the downloaded executable to start the installation process.
3.  **Follow the Wizard**:
    * Accept the license agreement.
    * Choose an installation directory (e.g., `C:\oraclexe`).
    * When prompted, set a password for the `SYS` and `SYSTEM` database administrative accounts. **Remember this password!**
    * Complete the installation.

### Creating the CCRM User

1.  **Open the SQL Command Line**:
    * Go to `Start > All Programs > Oracle Database 10g Express Edition > Run SQL Command Line`.

2.  **Connect as the SYSTEM user**:
    ```sql
    connect SYSTEM
    ```
    * When prompted, enter the password you set during the installation.

3.  **Create the `ccrm_user`**:
    * Execute the following commands to create the user and grant the necessary permissions:
    ```sql
    CREATE USER ccrm_user IDENTIFIED BY ccrm_pass;
    ```
    ```sql
    GRANT CONNECT, RESOURCE, DBA TO ccrm_user;
    ```
    * This creates a user named `ccrm_user` with the password `ccrm_pass` and gives it full administrative privileges.

4.  **Exit the SQL Command Line**:
    ```sql
    exit
    ```

***

## 4. Evolution of Java

* **1995**: Java is publicly announced by Sun Microsystems.
* **1996**: JDK 1.0 is released.
* **2004**: Java SE 5.0 (initially numbered 1.5) is released, introducing major features like generics, annotations, and autoboxing.
* **2014**: Java SE 8 is released, with the most significant changes being the introduction of Lambda expressions and the Stream API.
* **2018**: Oracle introduces a new 6-month release cadence, starting with Java SE 10.
* **2021**: Java SE 17 is released as the latest Long-Term Support (LTS) version.

***

## 5. Java ME vs. SE vs. EE

| Feature           | Java ME (Micro Edition)                               | Java SE (Standard Edition)                            | Java EE (Enterprise Edition)                             |
| ----------------- | ----------------------------------------------------- | ----------------------------------------------------- | -------------------------------------------------------- |
| **Primary Use** | Mobile devices, embedded systems, and other resource-constrained devices. | Desktop applications, servers, and embedded systems.    | Large-scale, distributed, and web-based enterprise applications. |
| **APIs** | A subset of Java SE APIs, with additional libraries for mobile and embedded development. | The core Java platform, including the JVM, core libraries, and development tools. | A superset of Java SE, with additional APIs for enterprise features like servlets, JSPs, and EJBs. |
| **Target Audience** | Developers creating applications for mobile phones and embedded devices. | General-purpose Java developers.                       | Enterprise application developers building robust and scalable systems. |

***

## 6. JDK vs. JRE vs. JVM

* **JVM (Java Virtual Machine)**: An abstract machine that provides a runtime environment in which Java bytecode can be executed. It is platform-dependent and is responsible for converting bytecode into machine code.
* **JRE (Java Runtime Environment)**: A software package that contains the JVM, along with the necessary libraries and other components to run Java applications.
* **JDK (Java Development Kit)**: A superset of the JRE that includes everything needed to develop Java applications, including the compiler (`javac`), debugger, and other development tools.

***

## 7. Windows Installation and Eclipse Setup

### Installing JDK on Windows

1.  **Download the JDK**: Go to the official Oracle Java SE Downloads page and download the appropriate installer for your version of Windows.
2.  **Run the Installer**: Double-click the downloaded executable to start the installation process. Follow the on-screen instructions.
3.  **Set Environment Variables**:
    * Open the "System Properties" window and click on "Environment Variables".
    * Under "System variables", click "New" and add a new variable named `JAVA_HOME` with the path to your JDK installation directory (e.g., `C:\Program Files\Java\jdk-17`).
    * Find the `Path` variable in the "System variables" list, select it, and click "Edit". Add `%JAVA_HOME%\bin` to the list of paths.

### Setting Up a Project in Eclipse

1.  **Create a New Java Project**:
    * In Eclipse, go to `File > New > Java Project`.
    * Enter a project name (e.g., "CCRM") and select the appropriate JRE version.
    * Click `Finish`.
2.  **Create Packages**:
    * Right-click on the `src` folder in your project and select `New > Package`.
    * Create the necessary packages for the project (e.g., `edu.ccrm.cli`, `edu.ccrm.domain`, `edu.ccrm.service`)[cite: 49].
3.  **Create Classes**:
    * Right-click on a package and select `New > Class` to create a new Java class.

### Setting Up a Project in Visual Studio Code

1.  **Install the Java Extension Pack**:
    * Open VS Code. Go to the Extensions view (Ctrl+Shift+X).
    * Search for "Extension Pack for Java" from Microsoft and install it.
2.  **Open the Project Folder**:
    * Go to `File > Open Folder...` and select the root directory where you cloned the project.
3.  **Configure the JDK**:
    * The extension should automatically detect your installed JDK. If not, you can configure it by opening the Command Palette (Ctrl+Shift+P), typing "Java: Configure Java Runtime", and selecting your JDK path.
4.  **Add the Oracle JDBC Driver**:
    * In the "JAVA PROJECTS" explorer view, find the "Referenced Libraries" section.
    * Click the `+` icon and navigate to the `lib` folder in the project.
    * Select the `ojdbc17.jar` file to add it to the project's classpath.
5.  **Run the Application**:
    * Open the `Main.java` file in the editor.
    * Click the "Run" button that appears above the `main` method.

***

## 8. Mapping of Syllabus Topics to Project Files

| Syllabus Topic                  | File/Class/Method Where Demonstrated                                 |
| ------------------------------- | -------------------------------------------------------------------- |
| **Encapsulation** | `Student.java`, `Course.java` (private fields with getters/setters) |
| **Inheritance** | `Person.java` (abstract base class for `Student` and `Instructor`)   |
| **Polymorphism** | `TranscriptService.java` (using `Person` references to handle `Student` and `Instructor` objects) |
| **Abstraction** | `Person.java` (abstract class with abstract methods)                 |
| **Interfaces** | `Searchable.java` (interface for searching courses)                  |
| **NIO.2 and Streams** | `ImportExportService.java` (for reading and writing CSV files)       |
| **Lambda Expressions** | `CourseService.java` (used for sorting and filtering courses)        |
| **Singleton Design Pattern** | `AppConfig.java` (ensuring a single instance of the application configuration) |
| **Builder Design Pattern** | `Course.java` (nested `Builder` class for constructing `Course` objects) |
| **Custom Exceptions** | `DuplicateEnrollmentException.java`, `MaxCreditLimitExceededException.java` |

***

## 9. Enabling Assertions

To enable assertions when running the application from the command line, use the `-ea` (or `-enableassertions`) flag:

```bash
java -ea -cp bin edu.ccrm.cli.Main
```
In Eclipse, you can enable assertions by going to Run > Run Configurations, selecting your application's run configuration, and adding -ea to the "VM arguments" text box in the "Arguments" tab.

## 10. Acknowledgements
* This project was created as part of the "Programming in Java" course on VitYarthi portal.

* All code is original and written by Sai Ardhendu Kalivarapu.
