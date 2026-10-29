CREATE TABLE departments (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    code VARCHAR(30) NOT NULL,
    name VARCHAR(120) NOT NULL,
    description VARCHAR(500) NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    CONSTRAINT uk_departments_code UNIQUE (code),
    CONSTRAINT uk_departments_name UNIQUE (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE users (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    department_id BIGINT UNSIGNED NULL,
    full_name VARCHAR(120) NOT NULL,
    email VARCHAR(254) NOT NULL,
    phone VARCHAR(30) NULL,
    password_hash VARCHAR(255) NOT NULL,
    role ENUM('CITIZEN', 'OFFICER', 'ADMIN') NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    CONSTRAINT uk_users_email UNIQUE (email),
    CONSTRAINT fk_users_department
        FOREIGN KEY (department_id) REFERENCES departments (id)
        ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE INDEX idx_users_department_id ON users (department_id);
CREATE INDEX idx_users_role ON users (role);

CREATE TABLE grievances (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    tracking_id VARCHAR(40) NOT NULL,
    citizen_id BIGINT UNSIGNED NOT NULL,
    department_id BIGINT UNSIGNED NULL,
    assigned_officer_id BIGINT UNSIGNED NULL,
    category ENUM('POTHOLE', 'GARBAGE', 'WATER_SUPPLY', 'DRAINAGE', 'STREETLIGHT', 'OTHER') NOT NULL,
    title VARCHAR(160) NOT NULL,
    description TEXT NOT NULL,
    address VARCHAR(500) NOT NULL,
    latitude DECIMAL(10, 8) NULL,
    longitude DECIMAL(11, 8) NULL,
    priority ENUM('LOW', 'MEDIUM', 'HIGH', 'CRITICAL') NOT NULL DEFAULT 'MEDIUM',
    status ENUM('SUBMITTED', 'ACKNOWLEDGED', 'ASSIGNED', 'IN_PROGRESS', 'RESOLVED', 'CLOSED', 'REJECTED') NOT NULL DEFAULT 'SUBMITTED',
    resolution_notes TEXT NULL,
    resolved_at TIMESTAMP(6) NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    CONSTRAINT uk_grievances_tracking_id UNIQUE (tracking_id),
    CONSTRAINT fk_grievances_citizen
        FOREIGN KEY (citizen_id) REFERENCES users (id)
        ON DELETE RESTRICT,
    CONSTRAINT fk_grievances_department
        FOREIGN KEY (department_id) REFERENCES departments (id)
        ON DELETE SET NULL,
    CONSTRAINT fk_grievances_assigned_officer
        FOREIGN KEY (assigned_officer_id) REFERENCES users (id)
        ON DELETE SET NULL,
    CONSTRAINT chk_grievances_latitude
        CHECK (latitude IS NULL OR latitude BETWEEN -90 AND 90),
    CONSTRAINT chk_grievances_longitude
        CHECK (longitude IS NULL OR longitude BETWEEN -180 AND 180)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE INDEX idx_grievances_citizen_id ON grievances (citizen_id);
CREATE INDEX idx_grievances_department_status ON grievances (department_id, status);
CREATE INDEX idx_grievances_assigned_officer_status ON grievances (assigned_officer_id, status);
CREATE INDEX idx_grievances_status_created_at ON grievances (status, created_at);

CREATE TABLE grievance_status_history (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    grievance_id BIGINT UNSIGNED NOT NULL,
    previous_status ENUM('SUBMITTED', 'ACKNOWLEDGED', 'ASSIGNED', 'IN_PROGRESS', 'RESOLVED', 'CLOSED', 'REJECTED') NULL,
    new_status ENUM('SUBMITTED', 'ACKNOWLEDGED', 'ASSIGNED', 'IN_PROGRESS', 'RESOLVED', 'CLOSED', 'REJECTED') NOT NULL,
    changed_by_user_id BIGINT UNSIGNED NOT NULL,
    note VARCHAR(1000) NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    CONSTRAINT fk_status_history_grievance
        FOREIGN KEY (grievance_id) REFERENCES grievances (id)
        ON DELETE CASCADE,
    CONSTRAINT fk_status_history_changed_by
        FOREIGN KEY (changed_by_user_id) REFERENCES users (id)
        ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE INDEX idx_status_history_grievance_created_at
    ON grievance_status_history (grievance_id, created_at);
