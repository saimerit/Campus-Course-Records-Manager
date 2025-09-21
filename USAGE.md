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
5\. Update Student Details  
6\. Back to Main Menu

* **Add New Student**: Prompts you to enter the student's ID, registration number, first name, last name, and email.  
* **List All Students**: Displays a table with the ID, registration number, name, email, and status of all students.  
* **View Student Profile & Transcript**: Enter a student's registration number to see their complete profile and academic transcript.  
* **Update Student Status**: Enter the student's registration number and a new status (e.g., ACTIVE, INACTIVE, GRADUATED).  
* **Update Student Details**: Allows you to modify the name and email for an existing student.

## **3\. Instructor Management**

Select option 2 from the main menu to access the **Instructor Management** menu.

\--- Instructor Management \---  
1\. Add New Instructor  
2\. List All Instructors  
3\. Update Instructor Details  
4\. Back to Main Menu

* **Add New Instructor**: Prompts for the instructor's Faculty ID (FiD), first name, last name, email, and department.  
* **List All Instructors**: Displays all instructors with their FiD, name, email, and department.  
* **Update Instructor Details**: Allows modification of an instructor's details based on their FiD.

## **4\. Course Management**

Select option 3 from the main menu to access the **Course Management** menu.

\--- Course Management \---  
1\. Add New Course  
2\. List All Courses  
3\. Search & Filter Courses  
4\. Assign Instructor to Course  
5\. Back to Main Menu

* **Add New Course**: Prompts for the course code, title, credits, department, and semester.  
* **List All Courses**: Displays all available courses with their details.  
* **Search & Filter Courses**: Provides options to filter courses by department, semester, or instructor FiD.  
* **Assign Instructor to Course**: Enter a course code and instructor FiD to link an instructor to a course.

## **5\. Enrollment & Grades**

Select option 4 from the main menu to manage **Enrollments and Grades**.

\--- Enrollment & Grading \---  
1\. Enroll Student in Course  
2\. Unenroll Student from Course  
3\. Record Student's Grade  
4\. View All Enrollments by Student  
5\. Back to Main Menu

* **Enroll a student in a course**: You will be asked for the student's registration number and the course code.  
* **Unenroll a student from a course**: Enter the student's registration number and the course code to remove them.  
* **Record a student's grade**: Enter a course code, and then enter a grade for each enrolled student (S, A, B, C, D, E, or F).  
* **View All Enrollments by Student**: Iterates through all students and displays their currently enrolled courses.

## **6\. File Operations**

Select option 5 from the main menu to perform **File Operations**.

\--- File Operations \---  
1\. Import Data from CSV files  
2\. Export Data to CSV files  
3\. Create a Backup  
4\. Show Backup Directory Size  
5\. Back to Main Menu

* **Import data from CSV files**: You can import data for courses, students, instructors, and enrollments from the CSV files located in the test-data directory.  
* **Export data to CSV files**: Exports the current state of all records into CSV files in the app-data directory.  
* **Create a system backup**: Creates a timestamped .zip archive of the app-data directory into the backups folder.  
* **Show Backup Directory Size**: Displays the total size of the backups directory.

## **7\. Data CSV Format**

The application uses CSV files for importing and exporting data. The expected format for each file is as follows:

### **students.csv**
```CSV
id,reg_no,first_name,last_name,email,status,registration_date  
1,S001,Alice,Smith,alice.smith@email.com,ACTIVE,2023-01-15
```
### **instructors.csv**
```CSV
FiD,firstName,lastName,email,department  
101,John,Smith,john.smith@university.edu,Computer Science
```
### **courses.csv**
```CSV
code,title,credits,department,instructor_id,semester  
CS101,Introduction to Computer Science,3,Computer Science,101,FALL
```
### **enrollments.csv**
```CSV
student_reg_no,course_code,grade  
S001,CS101,A  
```