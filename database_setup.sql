-- Drop tables first to ensure a clean setup
DROP TABLE ENROLLMENTS;
DROP TABLE COURSES;
DROP TABLE STUDENTS;
DROP TABLE INSTRUCTORS;

-- Create the Instructors table
CREATE TABLE INSTRUCTORS (
    id NUMBER PRIMARY KEY,
    first_name VARCHAR2(50),
    last_name VARCHAR2(50),
    email VARCHAR2(100),
    department VARCHAR2(100)
);

-- Create the Students table
CREATE TABLE STUDENTS (
    id NUMBER PRIMARY KEY,
    reg_no VARCHAR2(20) UNIQUE,
    first_name VARCHAR2(50),
    last_name VARCHAR2(50),
    email VARCHAR2(100),
    status VARCHAR2(20),
    registration_date DATE
);

-- Create the Courses table
CREATE TABLE COURSES (
    code VARCHAR2(10) PRIMARY KEY,
    title VARCHAR2(100),
    credits NUMBER,
    department VARCHAR2(100),
    instructor_id NUMBER,
    semester VARCHAR2(20),
    CONSTRAINT fk_instructor FOREIGN KEY (instructor_id) REFERENCES INSTRUCTORS(id)
);

-- Create the Enrollments table
CREATE TABLE ENROLLMENTS (
    student_id NUMBER,
    course_code VARCHAR2(10),
    grade VARCHAR2(2),
    PRIMARY KEY (student_id, course_code),
    CONSTRAINT fk_student FOREIGN KEY (student_id) REFERENCES STUDENTS(id),
    CONSTRAINT fk_course FOREIGN KEY (course_code) REFERENCES COURSES(code)
);