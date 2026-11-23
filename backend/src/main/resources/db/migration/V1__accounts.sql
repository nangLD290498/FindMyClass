CREATE TABLE app_users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    google_subject VARCHAR(255) NOT NULL,
    email VARCHAR(320) NOT NULL,
    display_name VARCHAR(255) NOT NULL,
    role VARCHAR(16),
    CONSTRAINT uq_users_google_subject UNIQUE (google_subject),
    CONSTRAINT ck_users_role CHECK (role IN ('TEACHER', 'STUDENT'))
) ENGINE=InnoDB DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_bin;
CREATE TABLE teacher_profiles (
    user_id BIGINT PRIMARY KEY,
    bio VARCHAR(2000) NOT NULL,
    subjects VARCHAR(500) NOT NULL,
    CONSTRAINT fk_teacher_user FOREIGN KEY (user_id) REFERENCES app_users(id)
) ENGINE=InnoDB DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_bin;
