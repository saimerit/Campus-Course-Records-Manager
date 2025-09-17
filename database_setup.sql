-- Drop tables first to ensure a clean setup
DROP TABLE enrollments;
DROP TABLE courses;
DROP TABLE students;
DROP TABLE instructors;

-- Create the Instructors table
CREATE TABLE instructors (
    id NUMBER PRIMARY KEY,
    first_name VARCHAR2(50),
    last_name VARCHAR2(50),
    email VARCHAR2(100),
    department VARCHAR2(100)
);

-- Create the Students table
CREATE TABLE students (
    id NUMBER PRIMARY KEY,
    reg_no VARCHAR2(20) UNIQUE,
    first_name VARCHAR2(50),
    last_name VARCHAR2(50),
    email VARCHAR2(100),
    status VARCHAR2(20)
);

-- Create the Courses table
CREATE TABLE courses (
    code VARCHAR2(10) PRIMARY KEY,
    title VARCHAR2(100),
    credits NUMBER,
    department VARCHAR2(100),
    instructor_id NUMBER,
    semester VARCHAR2(20),
    CONSTRAINT fk_instructor FOREIGN KEY (instructor_id) REFERENCES instructors(id)
);

-- Create the Enrollments table
CREATE TABLE enrollments (
    student_id NUMBER,
    course_code VARCHAR2(10),
    grade VARCHAR2(2),
    PRIMARY KEY (student_id, course_code),
    CONSTRAINT fk_student FOREIGN KEY (student_id) REFERENCES students(id),
    CONSTRAINT fk_course FOREIGN KEY (course_code) REFERENCES courses(code)
);