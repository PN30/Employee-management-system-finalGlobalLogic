-- Scalar function ---------------------------------------------------------
DELIMITER $$
CREATE FUNCTION fn_get_employee_full_name(p_employee_id BIGINT)
RETURNS VARCHAR(161)
READS SQL DATA
BEGIN
    DECLARE v_full_name VARCHAR(161);
    SELECT CONCAT(first_name, ' ', last_name) INTO v_full_name
    FROM employee
    WHERE id = p_employee_id;
    RETURN v_full_name;
END$$
DELIMITER ;

-- View --------------------------------------------------------------------
CREATE VIEW vw_employee_details AS
SELECT e.id                            AS employee_id,
       fn_get_employee_full_name(e.id) AS full_name,
       e.email                         AS email,
       e.job_title                     AS job_title,
       e.salary                        AS salary,
       e.hire_date                     AS hire_date,
       e.active                        AS active,
       d.id                            AS department_id,
       d.name                          AS department_name,
       (SELECT COUNT(*) FROM employee_project ep WHERE ep.employee_id = e.id) AS project_count
FROM employee e
JOIN department d ON d.id = e.department_id;

-- Stored procedures -------------------------------------------------------
DELIMITER $$
CREATE PROCEDURE sp_get_employees_by_department(IN p_department_id BIGINT)
BEGIN
    SELECT * FROM vw_employee_details
    WHERE department_id = p_department_id
    ORDER BY full_name;
END$$

CREATE PROCEDURE sp_get_active_employees()
BEGIN
    SELECT * FROM vw_employee_details
    WHERE active = TRUE
    ORDER BY full_name;
END$$
DELIMITER ;
