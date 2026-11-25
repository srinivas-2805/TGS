-- ============================================================================
-- TELANGANA GRAMMAR SCHOOL (TGS) - CORE REFERENCE DATA (FLYWAY V2)
-- ============================================================================

INSERT INTO roles (name, description) VALUES
('SUPER_ADMIN', 'System-wide super administrator with full privileges'),
('PRINCIPAL', 'School Principal with academic and administrative oversight'),
('ADMIN', 'School Administrator managing admissions, users, and operations'),
('TEACHER', 'Teaching faculty managing classes, attendance, marks, and homework'),
('ACCOUNTANT', 'Finance officer managing fee structures, receipts, and financial reports'),
('PARENT', 'Parent/Guardian accessing linked children records'),
('STUDENT', 'Enrolled student accessing personal academic portal'),
('LIBRARIAN', 'Library administrator managing books and circulation');

INSERT INTO academic_years (name, start_date, end_date, is_current, status) VALUES
('2025-2026', '2025-06-02', '2026-04-30', FALSE, 'ARCHIVED'),
('2026-2027', '2026-06-01', '2027-04-30', TRUE, 'ACTIVE');

INSERT INTO classes (name, level_order, category, is_activity_graded) VALUES
('LKG', 1, 'PRE_PRIMARY', TRUE),
('UKG', 2, 'PRE_PRIMARY', TRUE),
('1st Class', 3, 'PRIMARY', FALSE),
('2nd Class', 4, 'PRIMARY', FALSE),
('3rd Class', 5, 'PRIMARY', FALSE),
('4th Class', 6, 'PRIMARY', FALSE),
('5th Class', 7, 'PRIMARY', FALSE),
('6th Class', 8, 'HIGH_SCHOOL', FALSE),
('7th Class', 9, 'HIGH_SCHOOL', FALSE),
('8th Class', 10, 'HIGH_SCHOOL', FALSE),
('9th Class', 11, 'HIGH_SCHOOL', FALSE),
('10th Class', 12, 'HIGH_SCHOOL', FALSE);

INSERT INTO subjects (name, code, description, is_activity_based) VALUES
('English', 'ENG', 'English Language and Literature', FALSE),
('Telugu', 'TEL', 'Telugu First/Second Language', FALSE),
('Hindi', 'HIN', 'Hindi Language', FALSE),
('Mathematics', 'MAT', 'Mathematics and Quantitative Reasoning', FALSE),
('Environmental Studies', 'EVS', 'Primary Environmental Studies', FALSE),
('General Science', 'SCI', 'Science (Physics, Chemistry, Biology)', FALSE),
('Social Studies', 'SOC', 'History, Civics, and Geography', FALSE),
('Computer Science', 'CMP', 'Foundational Computing and Digital Literacy', FALSE),
('Early Literacy & Rhymes', 'ELR', 'Pre-primary phonics, storytelling, and rhymes', TRUE),
('Number Play & Shapes', 'NPS', 'Pre-primary numeracy and logical play', TRUE),
('Art, Craft & Motor Skills', 'ACM', 'Creative expression and fine motor activities', TRUE);

-- Grading rules (10-point Telangana/CBSE inspired scale)
INSERT INTO grading_rules (academic_year_id, grade_name, min_percentage, max_percentage, grade_point, description, is_passing) VALUES
(NULL, 'A+', 90.00, 100.00, 10.00, 'Outstanding', TRUE),
(NULL, 'A', 80.00, 89.99, 9.00, 'Excellent', TRUE),
(NULL, 'B+', 70.00, 79.99, 8.00, 'Very Good', TRUE),
(NULL, 'B', 60.00, 69.99, 7.00, 'Good', TRUE),
(NULL, 'C', 50.00, 59.99, 6.00, 'Above Average', TRUE),
(NULL, 'D', 35.00, 49.99, 5.00, 'Pass', TRUE),
(NULL, 'F', 0.00, 34.99, 0.00, 'Needs Improvement', FALSE);

-- Editable Public Website CMS Content (No fabricated claims, clear editable sections)
INSERT INTO website_content (section_key, title, subtitle, body_content, metadata_json, is_published) VALUES
('HOME_HERO', 'Nurturing Knowledge, Character & Future Leaders', 'Telangana Grammar School • Kalluru, Khammam, Telangana', 'Providing structured, value-based school education from LKG to 10th Class in Kalluru, Khammam. Our academic program balances foundational literacy, mathematics, languages, sciences, and co-curricular growth.', '{"ctaPrimary":"Admission Enquiry","ctaSecondary":"Explore Academics"}', TRUE),
('ABOUT_US', 'About Telangana Grammar School', 'Building Strong Academic Foundations in Kalluru', 'Telangana Grammar School is dedicated to holistic school education from Pre-Primary (LKG & UKG) through 10th Class in Kalluru, Khammam district, Telangana. School history, affiliation details, and institutional highlights can be updated directly by the school administration through the Portal CMS.', '{"location":"Kalluru, Khammam, Telangana, India","grades":"LKG to 10th Class"}', TRUE),
('PRINCIPAL_MESSAGE', 'Principal''s Desk', 'Message from the School Principal', '[Placeholder: School administration can update the Principal''s name, photograph, and official welcome message from the Admin Website Content Manager.] Our mission is to foster curiosity, discipline, and compassionate citizenship in every student.', '{"principalName":"[To be updated by Admin]","designation":"Principal, Telangana Grammar School"}', TRUE),
('VISION_MISSION', 'Vision & Mission', 'Guiding Principles of Our Institution', 'Vision: To empower every child in Kalluru and surrounding communities with clarity of thought, strong moral character, and academic excellence.\n\nMission: To deliver structured classroom teaching, continuous formative feedback, parent partnership, and safe campus facilities across LKG to 10th Class.', '{}', TRUE),
('TEACHING_METHODOLOGY', 'Teaching Methodology', 'Activity-Based Pre-Primary to Concept-Driven High School', 'In LKG and UKG, learning is activity-based with storytelling, rhymes, motor-skill play, and descriptive progress feedback. Primary classes (1st to 5th) focus on bilingual fluency, numeracy, and inquiry. High school classes (6th to 10th) emphasize analytical science, mathematics, languages, and board readiness.', '{}', TRUE),
('FACILITIES', 'Campus Facilities', 'Learning Spaces Designed for Growth', 'Administrators can publish verified campus facilities here. Standard ERP modules support Library Management, School Transport Routes, Classroom Timetables, and Digital Parent Communication.', '{"items":[{"name":"Library & Reading Room","detail":"Cataloged books with borrowing tracking"},{"name":"Classrooms (LKG–X)","detail":"Structured sections with assigned class teachers"},{"name":"School Transport","detail":"Route-managed buses with pickup/drop schedules"},{"name":"Academic Support","detail":"Homework, study materials, and periodic assessments"}]}', TRUE),
('WHY_CHOOSE_US', 'Why Choose Telangana Grammar School', 'Focused on Clarity, Discipline & Parent Partnership', 'Structured curriculum from LKG to 10th Class, transparent attendance and examination tracking, dedicated parent portal for multi-child families, and continuous teacher feedback.', '{}', TRUE),
('ACHIEVEMENTS', 'School Achievements & Milestones', 'Student & Institutional Recognition', '[Editable Placeholder: Administrators can add verified academic, sports, and cultural achievements here via the Website CMS module without modifying source code.]', '{}', TRUE),
('ADMISSION_NOTICE', 'Admissions Open (2026–2027)', 'LKG to 10th Class • Kalluru Campus', 'Applications and admission enquiries are invited for the 2026–2027 academic session. Submit an online enquiry or visit the school office during working hours.', '{}', TRUE),
('CONTACT_INFO', 'Contact Telangana Grammar School', 'Kalluru, Khammam District, Telangana, India', 'Please update official phone numbers, office email, and campus address landmarks from the Admin Website Content Manager.', '{"address":"Kalluru, Khammam District, Telangana, India","phone":"[Official Phone - Update in Admin CMS]","email":"[Official Email - Update in Admin CMS]","officeHours":"Monday – Saturday, 9:00 AM – 4:30 PM"}', TRUE);
