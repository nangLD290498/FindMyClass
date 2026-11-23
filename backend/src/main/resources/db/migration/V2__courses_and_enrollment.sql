CREATE TABLE courses (
 id BIGINT AUTO_INCREMENT PRIMARY KEY,
 teacher_id BIGINT NOT NULL,
 title VARCHAR(160) NOT NULL,
 description VARCHAR(4000) NOT NULL,
 subject VARCHAR(100) NOT NULL,
 level_name VARCHAR(100) NOT NULL,
 area VARCHAR(160) NOT NULL,
 location VARCHAR(500) NOT NULL,
 delivery_mode VARCHAR(16) NOT NULL,
 tuition DECIMAL(14,2) NOT NULL,
 capacity INT NOT NULL,
 start_date DATE NOT NULL,
 end_date DATE NOT NULL,
 status VARCHAR(16) NOT NULL,
 created_at DATETIME(6) NOT NULL,
 CONSTRAINT fk_course_teacher FOREIGN KEY (teacher_id) REFERENCES app_users(id),
 CONSTRAINT ck_course_capacity CHECK (capacity > 0),
 CONSTRAINT ck_course_tuition CHECK (tuition >= 0),
 CONSTRAINT ck_course_dates CHECK (end_date >= start_date),
 CONSTRAINT ck_course_mode CHECK (delivery_mode IN ('ONLINE','OFFLINE')),
 CONSTRAINT ck_course_status CHECK (status IN ('DRAFT','PUBLISHED','HIDDEN','ARCHIVED'))
) ENGINE=InnoDB DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_bin;
CREATE INDEX ix_courses_teacher ON courses(teacher_id);
CREATE INDEX ix_courses_public ON courses(status, created_at);
CREATE TABLE course_meetings (
 id BIGINT AUTO_INCREMENT PRIMARY KEY,
 course_id BIGINT NOT NULL,
 weekday INT NOT NULL,
 start_time VARCHAR(5) NOT NULL,
 end_time VARCHAR(5) NOT NULL,
 CONSTRAINT fk_meeting_course FOREIGN KEY (course_id) REFERENCES courses(id),
 CONSTRAINT ck_meeting_day CHECK (weekday BETWEEN 1 AND 7)
) ENGINE=InnoDB DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_bin;
CREATE INDEX ix_meetings_course ON course_meetings(course_id);
CREATE TABLE enrollments (
 id BIGINT AUTO_INCREMENT PRIMARY KEY,
 course_id BIGINT NOT NULL,
 student_id BIGINT NOT NULL,
 phone VARCHAR(30) NOT NULL,
 message VARCHAR(1000),
 source VARCHAR(80) NOT NULL,
 status VARCHAR(16) NOT NULL,
 created_at DATETIME(6) NOT NULL,
 CONSTRAINT fk_enrollment_course FOREIGN KEY (course_id) REFERENCES courses(id),
 CONSTRAINT fk_enrollment_student FOREIGN KEY (student_id) REFERENCES app_users(id),
 CONSTRAINT uq_enrollment_student UNIQUE(course_id, student_id),
 CONSTRAINT ck_enrollment_status CHECK (status IN ('NEW','CONTACTED','CONFIRMED','ATTENDING','CANCELLED','REJECTED'))
) ENGINE=InnoDB DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_bin;
CREATE INDEX ix_enrollment_student ON enrollments(student_id);
CREATE INDEX ix_enrollment_course ON enrollments(course_id, status);
CREATE TABLE enrollment_history (
 id BIGINT AUTO_INCREMENT PRIMARY KEY,
 enrollment_id BIGINT NOT NULL,
 actor_id BIGINT NOT NULL,
 status VARCHAR(16) NOT NULL,
 note VARCHAR(2000),
 created_at DATETIME(6) NOT NULL,
 CONSTRAINT fk_history_enrollment FOREIGN KEY (enrollment_id) REFERENCES enrollments(id),
 CONSTRAINT fk_history_actor FOREIGN KEY (actor_id) REFERENCES app_users(id)
) ENGINE=InnoDB DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_bin;
CREATE INDEX ix_history_enrollment ON enrollment_history(enrollment_id);
CREATE TABLE page_views (
 id BIGINT AUTO_INCREMENT PRIMARY KEY,
 teacher_id BIGINT NOT NULL,
 course_id BIGINT,
 source VARCHAR(80) NOT NULL,
 created_at DATETIME(6) NOT NULL,
 CONSTRAINT fk_view_teacher FOREIGN KEY (teacher_id) REFERENCES app_users(id),
 CONSTRAINT fk_view_course FOREIGN KEY (course_id) REFERENCES courses(id)
) ENGINE=InnoDB DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_bin;
CREATE INDEX ix_views_teacher ON page_views(teacher_id, created_at);
