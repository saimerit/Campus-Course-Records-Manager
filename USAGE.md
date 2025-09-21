# **CCRM Application Usage Guide**

This guide provides detailed instructions on how to use the various features of the **Campus Course & Records Manager (CCRM)** application.

## **1\. Main Menu**

When you start the application, you will be presented with the main menu. To navigate, enter the number corresponding to the desired option.

Welcome to Campus Course & Records Manager (CCRM)

\--- Main Menu \---  
1\. Manage Students  
2\. Manage Instructors  
3\. Manage Courses  
4\. Manage Enrollments & Grades  
5\. File Operations  
6\. Delete Database & Exit  
7\. Exit

## **2\. Student Management**

Select option 1 from the main menu to access the **Student Management** menu.

\--- Student Management \---  
1\. Add New Student  
2\. List All Students  
3\. View Student Profile & Transcript  
4\. Update Student Status  
5\. Back to Main Menu

* **Add New Student**: Prompts you to enter the student's ID, registration number, first name, last name, and email.  
* **List All Students**: Displays a table with the ID, registration number, name, email, and status of all students.  
* **View Student Profile & Transcript**: Enter a student's ID to see their complete profile and academic transcript, including enrolled courses and grades.  
* **Update Student Status**: Enter the student's ID and a new status (e.g., ACTIVE, INACTIVE, GRADUATED) to update their status.

## **3\. Instructor Management**

Select option 2 from the main menu to access the **Instructor Management** menu.

\--- Instructor Management \---  
1\. Add New Instructor  
2\. List All Instructors  
3\. Back to Main Menu

* **Add New Instructor**: Prompts for the instructor's ID, first name, last name, email, and department.  
* **List All Instructors**: Displays all instructors with their ID, name, email, and department.

## **4\. Course Management**

Select option 3 from the main menu to access the **Course Management** menu.

\--- Course Management \---  
1\. Add New Course  
2\. List All Courses  
3\. Search & Filter Courses  
4\. Assign Instructor to Course  
5\. Back to Main Menu

* **Add New Course**: Prompts for the course code, title, credits, department, and semester.  
* **List All Courses**: Displays all courses with their details.  
* **Search & Filter Courses**: Provides options to filter courses by department, semester, or instructor ID.  
* **Assign Instructor to Course**: Enter a course code and instructor ID to assign an instructor to a course.

## **5\. Enrollment & Grades**

Select option 4 from the main menu to manage **Enrollments and Grades**.

\--- Enrollment & Grading \---  
1\. Enroll Student in Course  
2\. Unenroll Student from Course  
3\. Record Student's Grade  
4\. Back to Main Menu

* **Enroll a student in a course**: You will be asked for the student's ID and the course code. The system will check for prerequisites and credit limits before enrolling.  
* **Unenroll a student from a course**: Enter the student's ID and the course code to remove them from a course.  
* **Record a student's grade**: Enter the course code and then enter the grade for each enrolled student (e.g., A, B, C, D, F).

## **6\. File Operations**

Select option 5 from the main menu to perform **File Operations**.

\--- File Operations \---  
1\. Import Data from CSV files  
2\. Export Data to CSV files  
3\. Create a Backup  
4\. Show Backup Directory Size  
5\. Back to Main Menu

* **Import data from CSV files**: You can import courses, students, instructors, and enrollments from CSV files.  
* **Export data to CSV files**: This will export the current state of all students, courses, instructors, and enrollments into CSV files in the app-data directory.  
* **Create a system backup**: This creates a timestamped zip archive of the exported CSV files in the backups directory.  
* **Show Backup Directory Size**: Displays the total size of the backups directory.

## **7\. Test Data CSV Format**

The application uses CSV files for importing and exporting data. The expected format for each file is as follows:

### **students.csv**
```csv
id,regNo,firstName,lastName,email,status  
1,2023001,John,Doe,john.doe@example.com,ACTIVE  
2,2023002,Jane,Smith,jane.smith@example.com,ACTIVE
```
### **courses.csv**
```csv
code,title,credits,department,semester,instructorId  
CS101,Introduction to Computer Science,3,Computer Science,FALL,101  
MATH201,Calculus II,4,Mathematics,SPRING,102
```
### **instructors.csv**
```csv
id,firstName,lastName,email,department  
101,Alan,Turing,alan.turing@example.com,Computer Science  
102,Ada,Lovelace,ada.lovelace@example.com,Mathematics
```
### **enrollments.csv**
```csv
studentId,courseCode,grade  
1,CS101,A  
2,MATH201,B  
```