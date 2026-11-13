INSERT INTO department (name, description) VALUES
    ('Engineering', 'Builds and maintains our products'),
    ('Human Resources', 'People, culture and hiring'),
    ('Marketing', 'Brand, content and growth'),
    ('Finance', 'Accounts, payroll and planning');

INSERT INTO employee (first_name, last_name, email, job_title, salary, hire_date, active, department_id) VALUES
    ('Aarav',   'Sharma', 'aarav.sharma@example.com',  'Senior Software Engineer', 2400000.00, '2021-06-14', TRUE,  (SELECT id FROM department WHERE name = 'Engineering')),
    ('Ishita',  'Reddy',  'ishita.reddy@example.com',  'Software Engineer',        1500000.00, '2023-01-09', TRUE,  (SELECT id FROM department WHERE name = 'Engineering')),
    ('Vikram',  'Nair',   'vikram.nair@example.com',   'QA Engineer',              1100000.00, '2022-08-22', TRUE,  (SELECT id FROM department WHERE name = 'Engineering')),
    ('Meera',   'Iyer',   'meera.iyer@example.com',    'HR Manager',               1800000.00, '2020-03-02', TRUE,  (SELECT id FROM department WHERE name = 'Human Resources')),
    ('Rohan',   'Gupta',  'rohan.gupta@example.com',   'Talent Partner',            900000.00, '2024-02-19', TRUE,  (SELECT id FROM department WHERE name = 'Human Resources')),
    ('Ananya',  'Das',    'ananya.das@example.com',    'Marketing Lead',           1700000.00, '2021-11-01', TRUE,  (SELECT id FROM department WHERE name = 'Marketing')),
    ('Karthik', 'Menon',  'karthik.menon@example.com', 'Content Strategist',        850000.00, '2023-07-17', FALSE, (SELECT id FROM department WHERE name = 'Marketing')),
    ('Sneha',   'Patel',  'sneha.patel@example.com',   'Financial Analyst',        1200000.00, '2022-04-25', TRUE,  (SELECT id FROM department WHERE name = 'Finance')),
    ('Aditya',  'Verma',  'aditya.verma@example.com',  'Accounts Executive',        700000.00, '2024-05-06', TRUE,  (SELECT id FROM department WHERE name = 'Finance'));

INSERT INTO project (name, description, start_date, end_date) VALUES
    ('Apollo Platform',  'Next-generation customer platform',     '2024-01-15', '2025-06-30'),
    ('Talent Portal',    'Internal hiring and onboarding portal', '2024-03-01', '2024-12-15'),
    ('Brand Refresh',    'Company-wide rebranding campaign',      '2024-05-10', '2024-10-31'),
    ('Finance Insights', 'Self-service reporting dashboards',     '2024-07-01', NULL);

INSERT INTO employee_project (employee_id, project_id)
SELECT e.id, p.id
FROM employee e
JOIN project p ON (e.email, p.name) IN (
    ('aarav.sharma@example.com',  'Apollo Platform'),
    ('ishita.reddy@example.com',  'Apollo Platform'),
    ('vikram.nair@example.com',   'Apollo Platform'),
    ('ishita.reddy@example.com',  'Finance Insights'),
    ('meera.iyer@example.com',    'Talent Portal'),
    ('rohan.gupta@example.com',   'Talent Portal'),
    ('aarav.sharma@example.com',  'Talent Portal'),
    ('ananya.das@example.com',    'Brand Refresh'),
    ('karthik.menon@example.com', 'Brand Refresh'),
    ('sneha.patel@example.com',   'Finance Insights'),
    ('aditya.verma@example.com',  'Finance Insights')
);
