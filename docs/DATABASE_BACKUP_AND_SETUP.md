# Database Setup, Migrations & Backup Guide

## 1. PostgreSQL Schema & Flyway Migrations

All schema objects live in [`backend/src/main/resources/db/migration/`](../backend/src/main/resources/db/migration):
- `V1__init_schema.sql`: Normalized tables (`users`, `roles`, `user_roles`, `refresh_tokens`, `password_reset_tokens`, `academic_years`, `classes`, `subjects`, `class_subjects`, `teachers`, `staff`, `sections`, `teacher_assignments`, `parents`, `students`, `student_parents`, `student_enrollments`, `student_documents`, `student_remarks`, `attendance`, `teacher_attendance`, `examinations`, `examination_subjects`, `grading_rules`, `marks`, `fee_structures`, `student_fees`, `payments`, `homework`, `homework_submissions`, `study_materials`, `timetables`, `leave_requests`, `announcements`, `notifications`, `buses`, `drivers`, `transport_routes`, `student_transport`, `books`, `book_issues`, `website_content`, `gallery`, `events`, `admission_enquiries`, `audit_logs`) with foreign keys, unique constraints, and performance indexes.
- `V2__seed_core_data.sql`: Core roles, academic years (`2025-2026`, `2026-2027`), classes (`LKG` to `10th Class`), core subjects, grading scale, and editable CMS sections.

Hibernate runs with `spring.jpa.hibernate.ddl-auto=validate`, guaranteeing entity mappings always match Flyway migrations.

---

## 2. Creating a Backup (`pg_dump`)

Run daily automated backups in compressed custom format:

```bash
pg_dump -h "${DB_HOST:-localhost}" -p "${DB_PORT:-5432}" -U "${DB_USERNAME:-tgs_admin}" \
  -F c -b -v -f "tgs_school_$(date +%Y%m%d_%H%M%S).dump" "${DB_NAME:-tgs_school}"
```

Also back up the uploaded documents directory (`STORAGE_LOCAL_DIR`, default `./uploads`) or enable versioning on your S3 bucket.

---

## 3. Restoring from Backup (`pg_restore`)

```bash
createdb -h "${DB_HOST:-localhost}" -p "${DB_PORT:-5432}" -U "${DB_USERNAME:-tgs_admin}" tgs_school_restore
pg_restore -h "${DB_HOST:-localhost}" -p "${DB_PORT:-5432}" -U "${DB_USERNAME:-tgs_admin}" \
  -d tgs_school_restore --clean --if-exists -v tgs_school_YYYYMMDD_HHMMSS.dump
```

---

## 4. Security Checklist for Production

1. Replace `JWT_SECRET` with a 256-bit random secret (`openssl rand -base64 48`).
2. Set `APP_SEED_DEMO_DATA=false` after initial production provisioning.
3. Restrict `CORS_ALLOWED_ORIGINS` to the exact production frontend domain over HTTPS.
4. Enable PostgreSQL SSL connections (`?sslmode=require`) and encrypt database backups at rest.
