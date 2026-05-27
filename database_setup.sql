DROP TABLE PROBATION_STUDENTS;
DROP TABLE PROBATION_REPORTS;
DROP TABLE ENROLLMENTS;
DROP TABLE COURSES;
DROP TABLE STUDENTS;
DROP TABLE INSTRUCTORS;

CREATE TABLE INSTRUCTORS (
    FiD VARCHAR2(20) PRIMARY KEY,
    first_name VARCHAR2(50),
    last_name VARCHAR2(50),
    email VARCHAR2(100),
    department VARCHAR2(100),
    dob DATE,
    phone VARCHAR2(20),
    cabin_no VARCHAR2(20)
);

CREATE TABLE STUDENTS (
    id NUMBER,
    reg_no VARCHAR2(20) PRIMARY KEY,
    first_name VARCHAR2(50),
    last_name VARCHAR2(50),
    email VARCHAR2(100),
    status VARCHAR2(20),
    registration_date DATE,
    dob DATE,
    phone VARCHAR2(20),
    cgpa NUMBER(3,2),
    probation_count NUMBER DEFAULT 0
);

CREATE TABLE COURSES (
    code VARCHAR2(10) PRIMARY KEY,
    title VARCHAR2(100),
    credits NUMBER,
    department VARCHAR2(100),
    instructor_id VARCHAR2(20),
    semester VARCHAR2(20),
    classroom_no VARCHAR2(20),
    CONSTRAINT fk_instructor FOREIGN KEY (instructor_id) REFERENCES INSTRUCTORS(FiD)
);

CREATE TABLE ENROLLMENTS (
    student_reg_no VARCHAR2(20),
    course_code VARCHAR2(10),
    grade VARCHAR2(2),
    enrollment_year NUMBER(4),
    enrollment_semester VARCHAR2(20),
    grand_total_marks NUMBER(5,2) DEFAULT 0,
    PRIMARY KEY (student_reg_no, course_code),
    CONSTRAINT fk_student FOREIGN KEY (student_reg_no) REFERENCES STUDENTS(reg_no),
    CONSTRAINT fk_course FOREIGN KEY (course_code) REFERENCES COURSES(code)
);

CREATE TABLE PROBATION_REPORTS (
    probation_id VARCHAR2(20) PRIMARY KEY,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    reason VARCHAR2(1000) NOT NULL
);

CREATE TABLE PROBATION_STUDENTS (
    probation_id VARCHAR2(20) NOT NULL,
    student_reg_no VARCHAR2(20) NOT NULL,
    PRIMARY KEY (probation_id, student_reg_no),
    CONSTRAINT fk_probation_report FOREIGN KEY (probation_id) REFERENCES PROBATION_REPORTS(probation_id) ON DELETE CASCADE,
    CONSTRAINT fk_probation_student_reg FOREIGN KEY (student_reg_no) REFERENCES STUDENTS(reg_no)
);