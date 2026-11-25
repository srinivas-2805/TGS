# Telangana Grammar School — REST API Documentation

Base URL: `/api/v1`  
Response envelope: `{ "success": boolean, "message": string, "data": T, "timestamp": ISO8601 }`

## Public & Health (No Authentication Required)
- `GET /api/v1/health` — Service health status
- `GET /api/v1/public/overview` — Published CMS content, public announcements, events, gallery items, and class levels
- `POST /api/v1/public/enquiries` — Submit admission enquiry (`studentName`, `parentName`, `phone`, `applyingClassName`, optional `email`, `previousSchool`, `message`)

## Authentication (`/api/v1/auth`)
- `POST /api/v1/auth/login` — Authenticate and receive JWT `accessToken` + `refreshToken`
- `POST /api/v1/auth/refresh` — Rotate refresh token and issue new access token
- `POST /api/v1/auth/logout` — Revoke refresh tokens
- `POST /api/v1/auth/forgot-password` — Request password reset token
- `POST /api/v1/auth/reset-password` — Reset password using token
- `POST /api/v1/auth/change-password` — Authenticated password change
- `GET /api/v1/auth/me` — Current authenticated user profile & roles

## Users (`/api/v1/users`) — `SUPER_ADMIN`, `PRINCIPAL`, `ADMIN`
- `GET /api/v1/users?q=&page=0&size=20`
- `POST /api/v1/users`
- `PATCH /api/v1/users/{id}/status`

## Students (`/api/v1/students`)
- `GET /api/v1/students?academicYearId=&classId=&sectionId=&status=&q=&page=0&size=20`
- `GET /api/v1/students/me` — Authenticated `STUDENT` profile
- `GET /api/v1/students/{id}` — Full 9-tab student detail (role & ownership enforced)
- `POST /api/v1/students/admit` — Admit student + enroll + auto-create student & parent accounts
- `PUT /api/v1/students/{id}` — Update student profile
- `PATCH /api/v1/students/{id}/status` — Transfer / withdraw / archive / reactivate
- `POST /api/v1/students/promote` — Bulk promote students to next academic year/class
- `POST /api/v1/students/{id}/documents` — Multipart upload (photo/certificates)
- `GET /api/v1/students/{id}/documents/{docId}/download` — Authorized document download
- `GET /api/v1/students/{id}/id-card/pdf` — Student ID Card PDF
- `POST /api/v1/students/{id}/remarks` — Add teacher/admin remark

## Parents (`/api/v1/parents`)
- `GET /api/v1/parents?q=&page=0&size=20`
- `POST /api/v1/parents`
- `POST /api/v1/parents/link-student` — Link additional child to parent account
- `GET /api/v1/parents/my-children` — Children linked to current `PARENT`

## Teachers & Staff (`/api/v1/teachers`, `/api/v1/staff`)
- `GET /api/v1/teachers`, `POST /api/v1/teachers`, `PUT /api/v1/teachers/{id}`
- `GET /api/v1/teachers/assignments`, `GET /api/v1/teachers/my-assignments`, `POST /api/v1/teachers/assignments`
- `GET /api/v1/staff`, `POST /api/v1/staff`

## Academics (`/api/v1/academics`)
- `GET /api/v1/academics/academic-years`, `POST /api/v1/academics/academic-years`, `PATCH /api/v1/academics/academic-years/{id}/set-current`
- `GET /api/v1/academics/classes`
- `GET /api/v1/academics/sections`, `POST /api/v1/academics/sections`, `PUT /api/v1/academics/sections/{id}`
- `GET /api/v1/academics/subjects`, `POST /api/v1/academics/subjects`
- `GET /api/v1/academics/classes/{classId}/subjects`, `POST /api/v1/academics/class-subjects`

## Attendance & Leaves (`/api/v1/attendance`, `/api/v1/leaves`)
- `POST /api/v1/attendance/students` — Batch mark section attendance (prevents duplicates, triggers absent parent notifications)
- `PATCH /api/v1/attendance/students/{id}/correct` — Admin/Principal correction with reason
- `GET /api/v1/attendance/section?sectionId=&date=&session=`
- `GET /api/v1/attendance/student/{studentId}?academicYearId=`
- `GET /api/v1/attendance/report?academicYearId=&classId=&sectionId=&from=&to=`
- `POST /api/v1/attendance/teachers`, `GET /api/v1/attendance/teachers`
- `GET /api/v1/leaves`, `GET /api/v1/leaves/student/{studentId}`, `GET /api/v1/leaves/teacher/my`
- `POST /api/v1/leaves/student`, `POST /api/v1/leaves/teacher`, `PATCH /api/v1/leaves/{id}/review`

## Examinations (`/api/v1/examinations`)
- `GET /api/v1/examinations`, `POST /api/v1/examinations`, `PATCH /api/v1/examinations/{id}/status`
- `GET /api/v1/examinations/{examId}/subjects`, `POST /api/v1/examinations/subjects`
- `POST /api/v1/examinations/marks`, `GET /api/v1/examinations/subjects/{examSubjectId}/marks`
- `GET /api/v1/examinations/report-card?studentId=&examinationId=`
- `GET /api/v1/examinations/report-card/pdf?studentId=&examinationId=`
- `GET /api/v1/examinations/{examId}/class-performance/{classId}`
- `GET /api/v1/examinations/grading-rules`, `POST /api/v1/examinations/grading-rules`, `DELETE /api/v1/examinations/grading-rules/{id}`

## Fees (`/api/v1/fees`)
- `GET /api/v1/fees/gateway-status`
- `GET /api/v1/fees/structures`, `POST /api/v1/fees/structures`
- `POST /api/v1/fees/assign`, `PATCH /api/v1/fees/student-fees/{id}/concession`
- `GET /api/v1/fees/student-fees`, `GET /api/v1/fees/student/{studentId}`, `GET /api/v1/fees/student/{studentId}/payments`
- `POST /api/v1/fees/payments`, `GET /api/v1/fees/payments/{paymentId}/receipt`
- `GET /api/v1/fees/report`, `POST /api/v1/fees/reminders`

## Homework, Timetable, Communication, Transport, Library, CMS, Reports
- `GET /api/v1/homework`, `GET /api/v1/homework/student/{studentId}`, `POST /api/v1/homework`, `POST /api/v1/homework/with-file`, `POST /api/v1/homework/{id}/submit`, `GET /api/v1/homework/{id}/submissions`, `PATCH /api/v1/homework/submissions/{submissionId}/review`, `GET /api/v1/homework/{id}/attachment`, `GET/POST /api/v1/homework/materials`, `GET /api/v1/homework/materials/{id}/download`
- `GET /api/v1/timetables/section/{sectionId}`, `GET /api/v1/timetables/teacher/my`, `POST /api/v1/timetables`, `DELETE /api/v1/timetables/{id}`, `GET /api/v1/timetables/section/{sectionId}/pdf`
- `GET/POST/DELETE /api/v1/announcements`, `GET /api/v1/notifications/my`, `GET /api/v1/notifications/unread-count`, `PATCH /api/v1/notifications/{id}/read`, `PATCH /api/v1/notifications/read-all`
- `GET/POST /api/v1/transport/buses`, `drivers`, `routes`, `allocations`
- `GET/POST /api/v1/library/books`, `GET/POST /api/v1/library/issues`, `PATCH /api/v1/library/issues/{id}/return`
- `GET/PUT /api/v1/cms/content`, `GET/POST/DELETE /api/v1/cms/gallery`, `events`, `GET/PATCH /api/v1/cms/enquiries`, `GET /api/v1/cms/audit-logs`
- `GET /api/v1/reports/dashboard`, `GET /api/v1/reports/{students|attendance|teachers-attendance|fees|examinations|library|transport}/export?format=pdf|xlsx`
