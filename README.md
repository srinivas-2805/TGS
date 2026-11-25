# Telangana Grammar School (TGS) — Complete School Management Platform

**Location:** Kalluru, Khammam District, Telangana, India  
**Education Levels:** LKG, UKG, 1st Class to 10th Class  

A complete, production-oriented **Public School Website** and **Role-Based School Management ERP Portal** built with **React + Vite + Bootstrap 5**, **Java 21 + Spring Boot 3.3 + Spring Security (JWT)**, and **PostgreSQL + Flyway**.

---

## 1. Architecture Overview

- **Public School Website (`/`):** 15 responsive pages (Home, About Us, Principal's Message, Vision & Mission, Academics, Classes LKG–10th, Teaching Methodology, Facilities, Gallery, Events, Achievements, Admission Enquiry, Contact Us, Notices, Login Portal). Content is database-backed via `website_content`, `events`, `gallery`, and `announcements`—no fabricated school claims are hardcoded.
- **School Management ERP Portal (`/portal/*`):** Modular monolithic backend (`com.tgs.school.*`) with 8 enforced RBAC roles (`SUPER_ADMIN`, `PRINCIPAL`, `ADMIN`, `TEACHER`, `ACCOUNTANT`, `PARENT`, `STUDENT`, `LIBRARIAN`) and ownership-based data access checks (`AccessControlService`).

### Original SVG Brand Assets
Located in [`frontend/src/assets/branding/`](./frontend/src/assets/branding) and [`frontend/public/favicon.svg`](./frontend/public/favicon.svg):
- `tgs-logo-main.svg`: Full crest (open book, graduation cap, rising sun, Kakatiya-inspired arch, growth leaves) + typography
- `tgs-logo-compact.svg`: Compact navbar crest
- `tgs-logo-document.svg`: Circular crest for report cards, fee receipts, and ID cards

---

## 2. Prerequisites

- **Java:** 21 (OpenJDK 21+)
- **Maven:** 3.6+
- **Node.js:** 20+ (and npm)
- **PostgreSQL:** 15+ (or Docker & Docker Compose)

---

## 3. Environment Setup

Copy the example environment files before running in production or custom local setups:

```bash
cp .env.example .env
cp backend/.env.example backend/.env
cp frontend/.env.example frontend/.env
```

Never commit real secrets (`JWT_SECRET`, `DB_PASSWORD`, `MAIL_PASSWORD`, `SMS_API_KEY`, `PAYMENT_GATEWAY_KEY_SECRET`, or S3 keys).

---

## 4. Database Setup & Running Locally

### Step 1: Start PostgreSQL
Using Docker Compose from `/home/user/TGS`:
```bash
docker compose up -d postgres
```
Or configure an existing local PostgreSQL 15+ instance matching `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USERNAME`, and `DB_PASSWORD`.  
Flyway automatically applies [`V1__init_schema.sql`](./backend/src/main/resources/db/migration/V1__init_schema.sql) and [`V2__seed_core_data.sql`](./backend/src/main/resources/db/migration/V2__seed_core_data.sql) when the backend starts.

### Step 2: Run the Spring Boot Backend
```bash
cd backend
mvn spring-boot:run
```
Backend API runs on `http://localhost:8080/api/v1` (health check: `GET /api/v1/health`).

### Step 3: Run the React Frontend
```bash
cd frontend
npm install
npm run dev
```
Frontend runs on `http://localhost:5173`.

---

## 5. Development Demo Accounts (`APP_SEED_DEMO_DATA=true`)

When `APP_SEED_DEMO_DATA=true` and the `users` table is empty, `DemoDataSeeder` creates fictional sample accounts for local development:

| Role | Username | Password | Notes |
| --- | --- | --- | --- |
| `SUPER_ADMIN` | `superadmin` | `Admin@123` | Full system access |
| `PRINCIPAL` | `principal` | `Principal@123` | Academic & administrative oversight |
| `ADMIN` | `admin` | `Admin@123` | Admissions, CMS, operations |
| `TEACHER` | `teacher1` | `Teacher@123` | Assigned classes (5th, 8th, 10th) |
| `ACCOUNTANT` | `accountant` | `Accountant@123` | Fees, concessions, receipts |
| `LIBRARIAN` | `librarian` | `Librarian@123` | Library catalog & circulation |
| `PARENT` | `parent1` | `Parent@123` | Linked to **2 children** (5th-A & LKG-A) |
| `STUDENT` | `student1` | `Student@123` | Student portal (5th Class-A) |

> Set `APP_SEED_DEMO_DATA=false` in production environments.

---

## 6. Testing Commands

### Backend (JUnit 5 + Spring Boot Test + Flyway + H2 PostgreSQL Mode)
```bash
cd backend
mvn clean test
```

### Frontend (Vitest + React Testing Library + Vite Production Build)
```bash
cd frontend
npm test
npm run build
```

---

## 7. Implemented Modules & Features

- **Authentication & RBAC:** BCrypt password hashing, JWT access tokens, rotating refresh tokens, logout revocation, forgot/reset/change password, user enable/lock management, ownership isolation (`AccessControlService`).
- **Student Management:** Admission wizard with auto-generated admission numbers (`TGS-YYYY-XXXX`), auto-provisioned student & parent accounts, 9-tab student profile, document & photo upload/download, ID Card PDF, transfer/withdrawal/archival, bulk promotion preserving historical enrollments.
- **Academics & Timetable:** Academic years, LKG–10th classes, sections with capacity & class teacher, subjects, weekly timetable with teacher & room conflict detection, Timetable PDF export.
- **Attendance & Leave:** Daily student attendance with duplicate prevention, absent parent notification workflow, admin attendance correction audit, teacher attendance, student & teacher leave requests updating attendance on approval.
- **Examinations & Report Cards:** Unit/Formative/Summative/Half-Yearly/Annual exams, numeric marks with validation + grading rules, LKG/UKG activity-based ratings & descriptive remarks, class performance, official Report Card PDF.
- **Fees & Finance:** Class-wise fee structures, student fee assignments, concessions & scholarships, Cash/UPI/Bank Transfer & server-verified online gateway support, Receipt PDFs, pending fee reminders.
- **Homework, Transport, Library, Communication, Website CMS & Reports:** Homework with file attachments & student submissions, study materials, buses/drivers/routes/allocations, library books & overdue late-fee returns, targeted announcements & notifications, database-backed Website CMS + admission enquiries, Recharts dashboard & PDF/Excel exports.

---

## 8. External Integrations Requiring Credentials

The platform includes configurable integration interfaces and environment variables for external services. Until configured with real credentials, they operate in safe fallback/local mode:

1. **Email (SMTP):** `MAIL_ENABLED=true`, `MAIL_HOST`, `MAIL_PORT`, `MAIL_USERNAME`, `MAIL_PASSWORD`, `MAIL_FROM`
2. **SMS Provider:** `SMS_PROVIDER` (`DISABLED` | `LOG_ONLY` | `TWILIO` | `MSG91`), `SMS_API_KEY`, `SMS_SENDER_ID`, `SMS_API_URL` (`LOG_ONLY` logs messages without sending SMS)
3. **Cloud Storage (S3):** `STORAGE_PROVIDER=S3`, `S3_BUCKET`, `S3_REGION`, `S3_ACCESS_KEY`, `S3_SECRET_KEY` (default `LOCAL` stores securely under `STORAGE_LOCAL_DIR`)
4. **Online Payment Gateway:** `PAYMENT_GATEWAY_PROVIDER=RAZORPAY`, `PAYMENT_GATEWAY_KEY_ID`, `PAYMENT_GATEWAY_KEY_SECRET`, `PAYMENT_GATEWAY_WEBHOOK_SECRET` (default `MANUAL_ONLY` restricts to Cash/UPI/Bank Transfer and rejects unverified online gateway payloads)

---

## 9. Additional Documentation

- [API Documentation](./docs/API_DOCUMENTATION.md)
- [Database Setup & Backup Guide](./docs/DATABASE_BACKUP_AND_SETUP.md)
