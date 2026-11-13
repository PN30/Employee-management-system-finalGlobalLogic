CREATE TABLE department (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    name        VARCHAR(100) NOT NULL,
    description VARCHAR(255) NULL,
    CONSTRAINT pk_department PRIMARY KEY (id),
    CONSTRAINT uk_department_name UNIQUE (name)
) ENGINE = InnoDB;

CREATE TABLE employee (
    id            BIGINT        NOT NULL AUTO_INCREMENT,
    first_name    VARCHAR(80)   NOT NULL,
    last_name     VARCHAR(80)   NOT NULL,
    email         VARCHAR(150)  NOT NULL,
    job_title     VARCHAR(100)  NULL,
    salary        DECIMAL(12,2) NULL,
    hire_date     DATE          NOT NULL,
    active        BOOLEAN       NOT NULL DEFAULT TRUE,
    department_id BIGINT        NOT NULL,
    CONSTRAINT pk_employee PRIMARY KEY (id),
    CONSTRAINT uk_employee_email UNIQUE (email),
    CONSTRAINT fk_employee_department FOREIGN KEY (department_id) REFERENCES department (id),
    CONSTRAINT chk_employee_salary CHECK (salary IS NULL OR salary >= 0)
) ENGINE = InnoDB;

CREATE INDEX idx_employee_department ON employee (department_id);
CREATE INDEX idx_employee_active ON employee (active);

CREATE TABLE project (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    name        VARCHAR(120) NOT NULL,
    description VARCHAR(500) NULL,
    start_date  DATE         NULL,
    end_date    DATE         NULL,
    CONSTRAINT pk_project PRIMARY KEY (id),
    CONSTRAINT uk_project_name UNIQUE (name),
    CONSTRAINT chk_project_dates CHECK (start_date IS NULL OR end_date IS NULL OR end_date >= start_date)
) ENGINE = InnoDB;

CREATE TABLE employee_project (
    employee_id BIGINT NOT NULL,
    project_id  BIGINT NOT NULL,
    CONSTRAINT pk_employee_project PRIMARY KEY (employee_id, project_id),
    CONSTRAINT fk_ep_employee FOREIGN KEY (employee_id) REFERENCES employee (id) ON DELETE CASCADE,
    CONSTRAINT fk_ep_project  FOREIGN KEY (project_id)  REFERENCES project (id)  ON DELETE CASCADE
) ENGINE = InnoDB;
