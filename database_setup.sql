-- Drop tables first to ensure a clean setup
DROP TABLE ENROLLMENTS;
DROP TABLE COURSES;
DROP TABLE STUDENTS;
DROP TABLE INSTRUCTORS;

-- Create the Instructors table
CREATE TABLE INSTRUCTORS (
    FiD VARCHAR2(20) PRIMARY KEY,
    first_name VARCHAR2(50),
    last_name VARCHAR2(50),
    email VARCHAR2(100),
    department VARCHAR2(100)
);

-- Create the Students table
CREATE TABLE STUDENTS (
    id NUMBER,
    reg_no VARCHAR2(20) PRIMARY KEY,
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
    instructor_id VARCHAR2(20),
    semester VARCHAR2(20),
    CONSTRAINT fk_instructor FOREIGN KEY (instructor_id) REFERENCES INSTRUCTORS(FiD)
);

-- Create the Enrollments table
CREATE TABLE ENROLLMENTS (
    student_reg_no VARCHAR2(20),
    course_code VARCHAR2(10),
    grade VARCHAR2(2),
    PRIMARY KEY (student_reg_no, course_code),
    CONSTRAINT fk_student FOREIGN KEY (student_reg_no) REFERENCES STUDENTS(reg_no),
    CONSTRAINT fk_course FOREIGN KEY (course_code) REFERENCES COURSES(code)
);