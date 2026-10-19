# EduForge — Complete Product Blueprint

**Version:** 1.0 | **Date:** 19 October 2026 | **Status:** Detailed planning baseline  
**Scope:** Exam Test Platform | **Stack:** Angular + Spring Boot + PostgreSQL, modular monolith

## 1. Purpose and vision
EduForge lets admins maintain exam content and students take practice, chapter, subject and mock tests. This first release focuses on the Test Module. AI tutor, notes, payments, community, live classes and user-generated quiz marketplace are out of scope for MVP.

Principles:
- Admin can create, view, update, delete, publish, unpublish, archive, restore, reorder, duplicate, import, export and preview content where appropriate.
- Every edit screen supports validation, save/cancel, unsaved-change handling, loading/error states and change history.
- Never expose correct answers during active exam mode.
- Backend owns authorization, deadlines, scoring and final submission.
- Historical results remain reproducible after content edits.
- Destructive actions show dependency impact and require confirmation.
- Important admin actions are audited; APIs are versioned and documented in OpenAPI.

---

## 2. MVP scope

**In scope:** authentication/profile; Exam → Subject → Chapter taxonomy; MCQ question bank; question editor and revisions; bulk import/export; test builder; preview/validation/publish lifecycle; timed/practice attempts; autosave/resume/timeout; results and answer review; history and basic analytics; admin users/roles; audit logs/reports/settings; responsive UI; Docker, CI, database migrations and API documentation.

**Out of scope for MVP:** AI question generation/tutor, payments/subscriptions, live classes, community, notes marketplace, public user-generated quizzes, native mobile app, complex adaptive testing and proctoring.

## 3. Roles and permission matrix
- **Student:** browse published content, start eligible tests, save answers, submit, view own permitted results/history.
- **Content Admin:** CRUD taxonomy/questions/tests, imports/exports, preview, publish/unpublish, content reports.
- **Support Admin:** scoped user/attempt troubleshooting; cannot silently change answers or scores.
- **Super Admin:** manage admin roles, platform settings and high-risk purge/correction workflows.
- **System worker:** process import/export jobs and timeouts with least privilege.

| Capability | Student | Content Admin | Support Admin | Super Admin |
|---|---|---|---|---|
| Browse published exams/tests | Yes | Yes | Yes | Yes |
| Take tests/view own results | Yes | Preview only | No by default | Preview only |
| CRUD exams/subjects/chapters | No | Yes | No | Yes |
| CRUD questions/tests | No | Yes | No | Yes |
| Publish/unpublish content | No | Yes | No | Yes |
| View all users/attempts | No | Limited | Scoped | Yes |
| Assign admin roles | No | No | No | Yes |
| Platform-wide settings | No | No | No | Yes |
| Permanent purge | No | No | No | Guarded |

Backend authorization is mandatory; Angular route guards only improve UX. Never allow the last active Super Admin to be demoted/deactivated. Do not expose plaintext passwords, hashes, refresh tokens or secrets.

---

## 4. Full Admin rights and update workflow

For each relevant resource, Admin needs **Create, Read, Update, Delete, Archive, Restore, Publish/Unpublish, Duplicate, Reorder/Move, Preview, History and Bulk actions** as applicable.

**Update screen must include:** breadcrumb and ID/status; fields grouped logically; required/cross-field validation; Save Draft / Save Changes / Cancel; dirty-state indicator; optimistic locking using `version` or `If-Match`; success/error feedback; audit history; impact warning for published/used content; preview.

**Deletion rules**
- Draft test with no attempts: deletion may be allowed.
- Published test: unpublish first; delete only if no attempt history.
- Test with attempts: archive by default and preserve snapshots/results.
- Question linked to tests: show usage report and require resolution.
- Subject/chapter with descendants: show dependency tree before action.
- Soft delete supports restore within retention policy.
- Permanent purge is Super Admin-only, audited, confirmed/re-authenticated and blocked when dependencies/retention rules prohibit it.
- Never cascade-delete attempt answers or historical snapshots.

**Update/version rule:** edits to published tests should create a new draft/revision or require unpublishing, according to final policy. Existing attempts stay pinned to the test/question revision they started with. Concurrent edits must return `409 VERSION_CONFLICT`, not silently overwrite another admin.

---

## 5. Core workflows and lifecycles

**Student:** sign in → choose exam → subject → chapter → select practice/mini-test/chapter/subject/mock → read instructions → start → answer/autosave → navigate/mark for review → submit or timeout → result → permitted answer review → attempt history.

**Admin:** dashboard → create exam → subject/chapter → create/import questions → build test → configure marking/timing/availability/result policy → preview → validate → publish → monitor analytics → update future revisions safely → archive/restore/delete with dependency review.

Test lifecycle: `DRAFT → PUBLISHED → UNPUBLISHED / ARCHIVED`.  
Attempt lifecycle: `IN_PROGRESS → SUBMITTED | TIMED_OUT`.

Submission is idempotent. Server clock controls deadline. Refresh/reconnect recovers an active attempt. Duplicate submission returns the same finalized result. Unpublishing prevents new attempts but never erases old attempts.

---

## 6. Test types, settings and scoring

**Types:** practice mode; timed exam mode; Easy/Medium/Hard mini-test; curated chapter test; chapter practice; subject test; full mock.

**Settings:** title/description/scope; duration; question list/order; marks and negative marks; maximum attempts; availability window; shuffle questions/options; navigation and mark-for-review; result/explanation visibility; passing score/grade bands; instructions; status and version.

Keep these metrics distinct:
- Score = earned marks under test rules.
- Maximum score = maximum possible marks.
- Score percentage = score / maximum score × 100.
- Attempted = answered questions.
- Correct / incorrect / unanswered = separate counts.
- Accuracy = correct / attempted × 100.
- Duration = server-calculated elapsed time.
- Best/average and chapter/difficulty analytics follow documented eligibility rules.

Example: 10 questions, +2 correct, −0.5 incorrect, 2 unanswered; 6 correct and 2 incorrect → score 11/20 = 55% score percentage; accuracy = 6/8 = 75%. Do not confuse the two.

---

## 7. Screen inventory and Figma specifications

Design system: deep navy/blue primary, neutral surfaces, semantic statuses with text/icons, 4/8/12/16/24/32/48 px spacing, accessible contrast and visible focus. Desktop: 12-column grid, max width ~1200–1360 px. Admin: left sidebar + top bar + breadcrumb + content. Student: compact header, content area and mobile-friendly navigation. Every screen needs loading, empty, error, disabled and responsive states.

### Student screens
1. **Exam Catalogue:** header, search/filter, exam cards, subject counts, Explore action. Empty state resets filters. API `GET /api/v1/exams?page=0&size=12&search=UPSC`.
2. **Exam Detail:** breadcrumb, exam description, subject cards and published test list. APIs `GET /exams/{id}`, `GET /exams/{id}/subjects`.
3. **Subject Detail:** subject overview, chapter cards/table, question/test availability and progress. APIs `GET /subjects/{id}`, `GET /subjects/{id}/chapters`.
4. **Chapter Detail:** syllabus, question count, Start Practice and Easy/Medium/Hard/Chapter Test actions. APIs `GET /chapters/{id}`, `GET /chapters/{id}/tests`.
5. **Test Instructions:** duration, question count, marking, attempt number, instructions and Start Test.
6. **Test Runner:** top bar with test title, server deadline countdown, save status and Submit; main question/options; side question palette with answered/unanswered/review states; Previous, Mark for Review, Clear, Save & Next. Mobile palette opens as drawer and bottom controls stay accessible. No answer key in exam mode.
7. **Submit Confirmation:** modal showing answered/unanswered/review counts; Continue or Submit final answers; retry on network failure.
8. **Result Summary:** score, percentage, accuracy, correct/incorrect/unanswered, duration, max marks, Review/Retake actions.
9. **Detailed Review:** question navigation, selected/correct answer, explanation and marks; only when policy allows.
10. **My Attempts:** date/test/status filters, score/accuracy/duration, pagination.
11. **Student Performance:** attempts, average/best score, accuracy, trend and exam/subject/chapter/difficulty breakdown.

### Admin screens
12. **Admin Login:** email/username, password visibility toggle, clear auth errors.
13. **Admin Dashboard:** sidebar (Dashboard, Taxonomy, Question Bank, Test Builder, Attempts, Users, Reports, Audit, Settings), KPI cards, recent activity and quick actions.
14. **Exam List:** search/status/pagination; name, subjects, status, updated by/time; View/Edit/Duplicate/Archive/Restore/Delete; bulk actions.
15. **Create/Edit Exam:** basic details, slug, description, display order/status; Save Draft/Save Changes/Cancel/Publish; dirty-state warning and version conflict handling.
16. **Subject/Chapter Tree:** exam→subject→chapter tree, detail editor, add/edit/move/reorder/archive/delete and dependency preview.
17. **Question Bank:** search/filters, ID/preview/chapter/difficulty/status/usage/updated date; edit/duplicate/archive/delete/history; import/export and bulk actions.
18. **Question Editor:** taxonomy, type, language, difficulty, tags, source, question text, options add/remove/reorder, correct answer, explanation, marks/negative marks, preview, save draft/update/archive.
19. **Import/Export:** template download, file upload, duplicate policy, dry run, valid/warning/invalid row counts, job progress and error report download.
20. **Test List:** filters by status/exam/type; question count, duration, attempts, status, updated by; Edit/Preview/Duplicate/Validate/Publish/Unpublish/Archive/Restore/Delete.
21. **Test Builder:** stepper for identity/scope, question selection, marks/order, duration/marking/availability, result visibility, preview/validation; sticky summary of count/marks/duration.
22. **Student Preview:** exact student-facing rendering, visible “Preview — no attempt will be recorded” banner, never creates a real attempt.
23. **Attempt Monitoring:** search by attempt/student/test/status/date; start/deadline/submit times; scoped support actions, no silent score edits.
24. **User Management:** search/paginate, account status, registration, attempt count, view/suspend/reactivate/revoke sessions; role management only for Super Admin.
25. **Reports & Analytics:** date/exam/test filters, attempts trend, score distribution, timeouts, question performance and audited CSV export.
26. **Audit Logs:** filter actor/action/resource/date/outcome; detail drawer with before/after diff and request ID; redact sensitive fields.
27. **Settings:** platform defaults, grade bands, result policies and maintenance mode; grouped validation and last-updated-by metadata.
28. **Trash/Archived:** archived and soft-deleted tabs, resource filters, restore with revalidation; purge only for Super Admin and if allowed.

### Figma pages
Cover & notes; Design Foundations; User Flows; Low-fi Wireframes; Student UI; Admin UI; Components & Variants; Clickable Prototype; Responsive/Mobile. First wireframe: Test Runner, Result Review, Question Editor, Test Builder.

---

## 8. Angular frontend architecture

```text
frontend/eduforge-web/src/app/
  core/auth/ guards/ interceptors/ services/
  shared/components/ pipes/ validators/ models/
  features/auth/
  features/exams/ subjects/ chapters/
  features/question-bank/ test-builder/ test-runner/
  features/attempts/ results/ analytics/
  features/admin-users/ audit-logs/ settings/
```
Use Reactive Forms for complex admin forms; Signals for local UI state where useful; typed DTOs/API services; shared loading/empty/error components; accessible labels and keyboard navigation. Route guards are not security boundaries. The server remains the source of truth for timer, scoring and attempt state.

---

## 9. Spring Boot backend architecture

```text
backend/eduforge-api/src/main/java/.../
  common/error validation pagination audit
  security/ auth/ user/ exam/ subject/ chapter/
  question/ test/ attempt/ result/ analytics/
  importexport/ admin/
```
Flow: Controller → validated request DTO → service/business rules → repository → PostgreSQL → response DTO. Do not expose JPA entities directly. Use Spring Security method/request authorization, transactions, centralized error mapping, Flyway migrations, optimistic locking (`@Version`), audit events and idempotent submission. Do not rely on production `ddl-auto=update`.

---

## 10. Database baseline

Core tables and key fields:
- `users`: id, email, username, password_hash, status, created_at, updated_at, version.
- `roles`, `user_roles`: role assignments include assigned_by/assigned_at.
- `exams`: name, slug, description, status, display_order, created_by/updated_by, timestamps, version, deleted_at.
- `subjects`: exam_id, name, slug, display_order, status, version, deleted_at.
- `chapters`: subject_id, name, slug, display_order, status, version, deleted_at.
- `questions`: chapter_id or join table, type, text, language, difficulty, marks, negative_marks, explanation, source, status, current_version, version, deleted_at.
- `question_options`: question_id, key, text, display_order, is_correct (admin/server only).
- `question_revisions`: question_id, revision_no, content_json, changed_by, changed_at, change_summary.
- `tests`: title, type, scope, duration_seconds, settings, status, availability, max_attempts, result_policy, creator/updater, version, deleted_at.
- `test_questions`: test_id, question_id, pinned_question_revision_id, display_order, marks overrides.
- `test_revisions`: test_id, revision_no, snapshot_json, changed_by, changed_at.
- `quiz_attempts`: test_id/revision, user_id, status, started_at, deadline_at, submitted_at, score, max_score, percentages, duration, idempotency key.
- `attempt_answers`: attempt_id, question snapshot, selected options, correctness, marks, saved_at.
- `audit_logs`: actor, action, resource, before/after JSON, outcome, request ID, timestamp.
- `import_jobs`, `platform_settings`.

Use foreign keys, unique constraints, indexes for common filters, transactions, UTC timestamps, password hashes only. Decide whether questions may belong to multiple chapters; MVP recommendation is one primary chapter unless cross-chapter reuse is required. Preserve attempt-time snapshots. Keep core searchable fields typed, not hidden only in JSON.

---

## 11. API conventions

Base path: `/api/v1`; JSON; `GET` reads, `POST` creates/commands, `PATCH` partial updates, `PUT` replacement/idempotent save, `DELETE` delete. Collection endpoints use `page`, `size`, `sort` and filters. Dates are ISO-8601 UTC. Admin mutation endpoints accept `version` or `If-Match`. Suggested statuses: 200, 201, 204, 400, 401, 403, 404, 409, 422, 429, 500.

Standard error:
```json
{
  "timestamp":"2026-10-19T17:30:00Z",
  "status":422,
  "code":"VALIDATION_FAILED",
  "message":"Please correct the highlighted fields.",
  "path":"/api/v1/admin/questions",
  "fieldErrors":[{"field":"options","message":"A single-choice MCQ needs at least two options."}],
  "requestId":"req_01J..."
}
```
Never return `isCorrect` to a student during an active exam. Never trust client-submitted scores. Use consistent validation and authorization.

---

## 12. API catalogue — complete baseline

The following endpoint list is a proposed inventory, not implemented software. Add request/response DTOs and OpenAPI definitions before coding. `:id` below means the resource identifier.

### A. Authentication and profile
- `POST /auth/register` — register student.
- `POST /auth/login` — authenticate.
- `POST /auth/refresh` — refresh access if refresh-token flow is selected.
- `POST /auth/logout` — revoke session/refresh token.
- `POST /auth/forgot-password` — request reset.
- `POST /auth/reset-password` — complete reset.
- `GET /users/me` — current profile.
- `PATCH /users/me` — update allowed profile fields.
- `PATCH /users/me/password` — change password.
- `GET /users/me/sessions` — list sessions if supported.
- `DELETE /users/me/sessions/:sessionId` — revoke one.
- `DELETE /users/me/sessions` — revoke sessions with explicit semantics.

### B. Student taxonomy and catalogue
- `GET /exams` — published exam list, search and pagination.
- `GET /exams/:examId` — public exam details.
- `GET /exams/:examId/subjects` — published subjects.
- `GET /subjects/:subjectId` — subject detail.
- `GET /subjects/:subjectId/chapters` — chapters.
- `GET /chapters/:chapterId` — chapter detail.
- `GET /chapters/:chapterId/tests` — published chapter tests.
- `GET /tests` — published test catalogue and filters.
- `GET /tests/:testId` — test metadata/eligibility, no answer key.

### C. Admin exams, subjects, chapters
- `POST /admin/exams` — create.
- `GET /admin/exams` — list drafts/published/archived.
- `GET /admin/exams/:examId` — read full record.
- `PATCH /admin/exams/:examId` — update.
- `DELETE /admin/exams/:examId` — dependency-aware delete.
- `POST /admin/exams/:examId/archive` — archive.
- `POST /admin/exams/:examId/restore` — restore.
- `GET /admin/exams/:examId/dependencies` — impact report.
- `POST /admin/exams/:examId/subjects` — create subject.
- `GET /admin/subjects/:subjectId` — admin detail.
- `PATCH /admin/subjects/:subjectId` — update subject.
- `DELETE /admin/subjects/:subjectId` — delete subject.
- `POST /admin/subjects/:subjectId/archive` — archive subject.
- `POST /admin/subjects/:subjectId/restore` — restore subject.
- `GET /admin/subjects/:subjectId/dependencies` — impact report.
- `POST /admin/subjects/:subjectId/chapters` — create chapter.
- `GET /admin/chapters/:chapterId` — admin detail.
- `PATCH /admin/chapters/:chapterId` — update chapter.
- `DELETE /admin/chapters/:chapterId` — delete chapter.
- `POST /admin/chapters/:chapterId/archive` — archive chapter.
- `POST /admin/chapters/:chapterId/restore` — restore chapter.
- `PATCH /admin/chapters/:chapterId/move` — move chapter.
- `PATCH /admin/taxonomy/reorder` — reorder valid siblings.

### D. Admin question bank
- `POST /admin/questions` — create question.
- `GET /admin/questions` — search/filter/page.
- `GET /admin/questions/:questionId` — full editor data.
- `PATCH /admin/questions/:questionId` — update.
- `DELETE /admin/questions/:questionId` — dependency-aware delete.
- `POST /admin/questions/:questionId/duplicate` — duplicate to draft.
- `POST /admin/questions/:questionId/archive` — archive.
- `POST /admin/questions/:questionId/restore` — restore.
- `GET /admin/questions/:questionId/usage` — linked tests.
- `GET /admin/questions/:questionId/versions` — revision history.
- `PATCH /admin/questions/bulk` — bulk field update.
- `POST /admin/questions/bulk-delete` — dependency-aware bulk delete.
- `POST /admin/questions/import` — CSV/XLSX import job.
- `GET /admin/questions/import-template` — template download.
- `GET /admin/questions/imports/:jobId` — import status/errors.
- `GET /admin/questions/export` — filtered export.
- `GET /admin/jobs/:jobId/errors` — row-level error report.

### E. Admin test builder and lifecycle
- `POST /admin/tests` — create draft.
- `GET /admin/tests` — search/filter all tests.
- `GET /admin/tests/:testId` — details/settings.
- `PATCH /admin/tests/:testId` — update metadata.
- `DELETE /admin/tests/:testId` — dependency-aware delete.
- `POST /admin/tests/:testId/duplicate` — draft copy.
- `GET /admin/tests/:testId/questions` — attached questions/order.
- `POST /admin/tests/:testId/questions` — add questions.
- `DELETE /admin/tests/:testId/questions/:questionId` — remove from future test revision.
- `PATCH /admin/tests/:testId/questions/reorder` — reorder.
- `PATCH /admin/tests/:testId/questions/:questionId` — per-test marks/order override.
- `PATCH /admin/tests/:testId/settings` — duration/marking/shuffle/attempt rules.
- `PATCH /admin/tests/:testId/availability` — start/end window.
- `POST /admin/tests/:testId/validate` — pre-publish validation.
- `POST /admin/tests/:testId/preview` — safe preview, no real attempt.
- `POST /admin/tests/:testId/publish` — publish validated revision.
- `POST /admin/tests/:testId/unpublish` — stop new attempts.
- `POST /admin/tests/:testId/archive` — archive.
- `POST /admin/tests/:testId/restore` — restore.
- `GET /admin/tests/:testId/dependencies` — dependency report.
- `GET /admin/tests/:testId/revisions` — revision history.
- `GET /admin/tests/:testId/analytics` — test analytics.
- `POST /admin/tests/bulk-archive` — bulk archive with preview/summary.

### F. Student attempt engine
- `POST /tests/:testId/attempts` — start attempt.
- `GET /attempts/:attemptId` — resume safe attempt state.
- `PUT /attempts/:attemptId/answers/:questionId` — save one answer.
- `PUT /attempts/:attemptId/answers` — batch save.
- `PATCH /attempts/:attemptId/questions/:questionId/status` — mark/unmark review.
- `POST /attempts/:attemptId/heartbeat` — optional connectivity signal.
- `GET /attempts/:attemptId/status` — status/deadline.
- `POST /attempts/:attemptId/submit` — idempotent submit.
- `GET /attempts/:attemptId/result` — result when allowed.
- `GET /attempts/:attemptId/review` — answer review when allowed.
- `GET /attempts/:attemptId/result/answers` — question-level marks if policy allows.

### G. Student history and analytics
- `GET /users/me/attempts` — paginated history.
- `GET /users/me/attempts/summary` — attempt summary.
- `GET /users/me/performance` — overall metrics.
- `GET /users/me/performance/by-exam` — exam breakdown.
- `GET /users/me/performance/by-subject` — subject breakdown.
- `GET /users/me/performance/by-chapter` — chapter breakdown.
- `GET /users/me/performance/by-difficulty` — difficulty breakdown.

### H. Admin user/role management
- `GET /admin/users` — search/paginate.
- `GET /admin/users/:userId` — user detail.
- `PATCH /admin/users/:userId` — update allowed fields.
- `POST /admin/users/:userId/activate` — activate.
- `POST /admin/users/:userId/deactivate` — deactivate.
- `POST /admin/users/:userId/suspend` — suspend.
- `POST /admin/users/:userId/restore` — restore eligible account.
- `GET /admin/users/:userId/attempts` — history.
- `GET /admin/roles` — roles.
- `POST /admin/users/:userId/roles` — assign permitted role.
- `DELETE /admin/users/:userId/roles/:roleId` — remove role.
- `POST /admin/users/:userId/revoke-sessions` — revoke sessions.

### I. Admin attempts, analytics, reports, audit and settings
- `GET /admin/attempts` — search/filter attempts.
- `GET /admin/attempts/:attemptId` — attempt detail.
- `GET /admin/dashboard` — KPI summary.
- `GET /admin/analytics/attempts` — time series.
- `GET /admin/analytics/scores` — score distribution.
- `GET /admin/analytics/questions` — item performance.
- `GET /admin/analytics/completion` — completion/timeout metrics.
- `GET /admin/reports/attempts` — report/export.
- `GET /admin/reports/questions` — question report.
- `GET /admin/reports/export` — approved report export.
- `GET /admin/audit-logs` — filter logs.
- `GET /admin/audit-logs/:logId` — detail.
- `GET /admin/settings` — settings.
- `PATCH /admin/settings` — update allowed settings.
- `GET /admin/trash` — archived/soft-deleted records.
- `POST /admin/trash/:resourceType/:id/restore` — restore.
- `POST /admin/purge/preview` — Super Admin impact preview, only if purge is needed.
- `POST /admin/purge/execute` — Super Admin guarded purge, only if policy permits.

### J. Operational
- `GET /health` — health.
- `GET /readiness` — readiness.
- `GET /admin/jobs/:jobId` — background job status.
- `POST /admin/jobs/:jobId/cancel` — cancel if supported.
- `GET /admin/system-info` — sanitized restricted operational summary.
- `POST /admin/maintenance-mode` — Super Admin maintenance toggle.

**Endpoint count is intentionally broad**; consolidate duplicates only during OpenAPI review, not by omitting core behaviours.

---

## 13. Request/response examples — API contract patterns

Examples below are illustrative. IDs and data are fake; endpoint contracts must be validated before implementation.

### Register
`POST /api/v1/auth/register`
```json
{"email":"student@example.com","password":"Strong-example-secret","displayName":"Aarav"}
```
`201 Created`
```json
{"userId":"usr_101","email":"student@example.com","displayName":"Aarav","status":"ACTIVE"}
```

### Login
`POST /api/v1/auth/login`
```json
{"email":"student@example.com","password":"Strong-example-secret"}
```
`200 OK`
```json
{"accessToken":"<redacted-example-token>","expiresIn":900,"user":{"id":"usr_101","displayName":"Aarav","roles":["STUDENT"]}}
```
Choose cookie/token strategy deliberately; browser refresh tokens should be protected appropriately and CSRF/CORS configured for that model.

### List exams
`GET /api/v1/exams?page=0&size=12&search=UPSC&sort=name,asc`
```json
{"items":[{"id":"exam_upsc","name":"UPSC Civil Services","slug":"upsc-civil-services","subjectCount":8}],"page":0,"size":12,"totalElements":1,"totalPages":1}
```

### Create and update exam
`POST /api/v1/admin/exams`
```json
{"name":"UPSC Civil Services","slug":"upsc-civil-services","description":"Exam preparation","displayOrder":1,"status":"DRAFT"}
```
`201`
```json
{"id":"exam_upsc","name":"UPSC Civil Services","status":"DRAFT","version":0}
```
`PATCH /api/v1/admin/exams/exam_upsc`
```json
{"name":"UPSC Civil Services Examination","description":"Updated overview and syllabus.","displayOrder":2,"version":0}
```
`200`
```json
{"id":"exam_upsc","name":"UPSC Civil Services Examination","description":"Updated overview and syllabus.","displayOrder":2,"status":"DRAFT","version":1,"updatedAt":"2026-10-19T17:10:00Z"}
```
If `version` is stale, return `409 VERSION_CONFLICT`. This explicitly demonstrates that Admin can UPDATE, not only create/delete.

### Create/update subject and chapter
`POST /api/v1/admin/exams/exam_upsc/subjects`
```json
{"name":"History","slug":"history","description":"History syllabus","displayOrder":1,"status":"DRAFT"}
```
`201`
```json
{"id":"sub_history","examId":"exam_upsc","name":"History","status":"DRAFT","version":0}
```
`PATCH /api/v1/admin/subjects/sub_history`
```json
{"name":"Indian History","description":"Updated subject description","version":0}
```
`POST /api/v1/admin/subjects/sub_history/chapters`
```json
{"name":"Modern India","slug":"modern-india","displayOrder":1,"status":"DRAFT"}
```
`PATCH /api/v1/admin/chapters/ch_modern`
```json
{"name":"Modern Indian History","description":"Updated chapter outline","version":0}
```
Move/reorder examples:
```json
{"targetSubjectId":"sub_history_modern","version":1}
```
for `PATCH /api/v1/admin/chapters/ch_modern/move`, and
```json
{"parentType":"SUBJECT","parentId":"exam_upsc","orderedIds":["sub_polity","sub_history"]}
```
for `PATCH /api/v1/admin/taxonomy/reorder`.

### Create/edit question
`POST /api/v1/admin/questions`
```json
{
  "chapterId":"ch_modern","type":"SINGLE_CHOICE","language":"en","difficulty":"MEDIUM",
  "text":"Which event is associated with 1857 in Indian history?",
  "options":[
    {"key":"A","text":"Revolt of 1857","isCorrect":true},
    {"key":"B","text":"Partition of Bengal","isCorrect":false},
    {"key":"C","text":"Dandi March","isCorrect":false},
    {"key":"D","text":"Quit India Movement","isCorrect":false}
  ],
  "explanation":"The Revolt of 1857 is associated with this year.",
  "marks":2,"negativeMarks":0.5,"tags":["modern-india","revolt"],"source":"Practice set 1","status":"DRAFT"
}
```
`201`
```json
{"id":"q_1001","chapterId":"ch_modern","type":"SINGLE_CHOICE","difficulty":"MEDIUM","status":"DRAFT","version":0,"revision":1}
```
`PATCH /api/v1/admin/questions/q_1001`
```json
{"text":"Which major event took place in 1857 in India?","explanation":"Updated explanation.","difficulty":"EASY","version":0}
```
Response includes `version:1` and `revision:2`. If linked to a published test, create a new revision for future content; preserve historical attempt snapshots.

### Search/bulk question examples
`GET /api/v1/admin/questions?page=0&size=25&chapterId=ch_modern&difficulty=MEDIUM&status=DRAFT&search=1857`
```json
{"items":[{"id":"q_1001","preview":"Which event is associated with 1857...","difficulty":"MEDIUM","status":"DRAFT","usedInTests":0,"version":0}],"page":0,"size":25,"totalElements":1,"totalPages":1}
```
`PATCH /api/v1/admin/questions/bulk`
```json
{"questionIds":["q_1001","q_1002"],"changes":{"difficulty":"EASY","status":"DRAFT"}}
```
```json
{"updatedCount":2,"failedCount":0,"failures":[]}
```
Import uses multipart upload to `POST /api/v1/admin/questions/import`; response `202` returns `jobId`. Poll `GET /api/v1/admin/questions/imports/{jobId}` for row counts and errors.

### Create/update test
`POST /api/v1/admin/tests`
```json
{
  "title":"Modern India — Chapter Test 1","description":"Practice test",
  "testType":"CHAPTER_TEST","chapterId":"ch_modern","durationSeconds":1800,
  "maxAttempts":3,"shuffleQuestions":false,"shuffleOptions":true,
  "negativeMarkingEnabled":true,"resultPolicy":"AFTER_SUBMIT","reviewPolicy":"AFTER_SUBMIT","status":"DRAFT"
}
```
`201`
```json
{"id":"test_2001","title":"Modern India — Chapter Test 1","status":"DRAFT","questionCount":0,"version":0}
```
`PATCH /api/v1/admin/tests/test_2001/settings`
```json
{"durationSeconds":2400,"maxAttempts":2,"shuffleQuestions":true,"shuffleOptions":true,"negativeMarkingEnabled":true,"resultPolicy":"AFTER_SUBMIT","reviewPolicy":"AFTER_SUBMIT","version":0}
```
`POST /api/v1/admin/tests/test_2001/questions`
```json
{"questionIds":["q_1001","q_1002"],"placement":"APPEND"}
```
`PATCH /api/v1/admin/tests/test_2001/questions/reorder`
```json
{"orderedQuestionIds":["q_1002","q_1001"],"version":2}
```
`POST /api/v1/admin/tests/test_2001/validate` returns errors/warnings:
```json
{"valid":false,"errors":[{"code":"TOO_FEW_QUESTIONS","message":"Add at least 5 questions before publishing."}],"warnings":[{"code":"NO_END_DATE","message":"This test has no scheduled end date."}]}
```
`POST /api/v1/admin/tests/test_2001/preview` with `{"mode":"PREVIEW","recordAttempt":false}` must not create a real attempt. Publish response:
```json
{"id":"test_2001","status":"PUBLISHED","publishedAt":"2026-10-19T17:30:00Z","testRevision":1}
```

### Start/save/resume/submit attempt
`POST /api/v1/tests/test_2001/attempts`
```json
{"clientRequestId":"start_7f9d"}
```
`201`
```json
{
  "attemptId":"att_9001","testId":"test_2001","status":"IN_PROGRESS",
  "startedAt":"2026-10-19T17:40:00Z","deadlineAt":"2026-10-19T18:20:00Z","durationSeconds":2400,
  "questions":[{"questionId":"q_1001","position":1,"text":"Which major event took place in 1857 in India?","options":[{"id":"opt_a","text":"Revolt of 1857"},{"id":"opt_b","text":"Partition of Bengal"}]}]
}
```
No `isCorrect` in active attempt. Resume with `GET /api/v1/attempts/att_9001`.
`PUT /api/v1/attempts/att_9001/answers/q_1001`
```json
{"selectedOptionIds":["opt_a"],"clientSequence":4}
```
```json
{"attemptId":"att_9001","questionId":"q_1001","saveStatus":"SAVED","savedAt":"2026-10-19T17:42:10Z","clientSequence":4}
```
`POST /api/v1/attempts/att_9001/submit`
```json
{"submissionRequestId":"submit_abc123"}
```
`200`
```json
{
  "attemptId":"att_9001","status":"SUBMITTED","score":11,"maxScore":20,"scorePercentage":55.0,
  "correctCount":6,"incorrectCount":2,"unansweredCount":2,"attemptedCount":8,
  "accuracy":75.0,"durationSeconds":1320,"resultAvailable":true
}
```
Backend computes score from saved answers and pinned revisions; client cannot submit score. Repeated submission returns the same finalized result.

### Result/history examples
- `GET /api/v1/attempts/att_9001/result`
- `GET /api/v1/attempts/att_9001/review`
- `GET /api/v1/users/me/attempts?page=0&size=20&status=SUBMITTED`
- `GET /api/v1/users/me/performance`
- `GET /api/v1/users/me/performance/by-exam`
- `GET /api/v1/users/me/performance/by-subject`
- `GET /api/v1/users/me/performance/by-chapter`
- `GET /api/v1/users/me/performance/by-difficulty`

Example history response:
```json
{"items":[{"attemptId":"att_9001","testTitle":"Modern India — Chapter Test 1","status":"SUBMITTED","score":11,"maxScore":20,"scorePercentage":55.0,"accuracy":75.0,"submittedAt":"2026-10-19T18:02:00Z"}],"page":0,"size":20,"totalElements":1,"totalPages":1}
```

### Admin users, roles and monitoring
- `GET /api/v1/admin/users?page=0&size=25&status=ACTIVE&search=student`
- `GET /api/v1/admin/users/{userId}`
- `PATCH /api/v1/admin/users/{userId}`
- `POST /api/v1/admin/users/{userId}/activate`
- `POST /api/v1/admin/users/{userId}/deactivate`
- `POST /api/v1/admin/users/{userId}/suspend`
- `POST /api/v1/admin/users/{userId}/restore`
- `GET /api/v1/admin/users/{userId}/attempts`
- `GET /api/v1/admin/roles`
- `POST /api/v1/admin/users/{userId}/roles`
- `DELETE /api/v1/admin/users/{userId}/roles/{roleId}`
- `POST /api/v1/admin/users/{userId}/revoke-sessions`
- `GET /api/v1/admin/attempts`
- `GET /api/v1/admin/attempts/{attemptId}`

Example update:
`PATCH /api/v1/admin/users/usr_101`
```json
{"displayName":"Aarav Kumar","status":"ACTIVE","version":2}
```
Allowlist editable fields; never allow mass assignment of password hash, arbitrary roles or internal flags.

### Analytics, audit, settings and operations examples
- `GET /api/v1/admin/dashboard`
- `GET /api/v1/admin/analytics/attempts?from=2026-10-01&to=2026-10-19&examId=exam_upsc`
- `GET /api/v1/admin/analytics/scores?testId=test_2001`
- `GET /api/v1/admin/analytics/questions?chapterId=ch_modern`
- `GET /api/v1/admin/analytics/completion`
- `GET /api/v1/admin/reports/attempts?from=2026-10-01&to=2026-10-19&format=csv`
- `GET /api/v1/admin/reports/questions`
- `GET /api/v1/admin/reports/export`
- `GET /api/v1/admin/audit-logs?actorId=usr_admin&action=QUESTION_UPDATED&page=0&size=50`
- `GET /api/v1/admin/audit-logs/{logId}`
- `GET /api/v1/admin/settings`
- `PATCH /api/v1/admin/settings`
- `GET /api/v1/admin/trash?resourceType=QUESTION`
- `POST /api/v1/admin/trash/{resourceType}/{id}/restore`
- `GET /api/v1/health`
- `GET /api/v1/readiness`
- `GET /api/v1/admin/jobs/{jobId}`
- `POST /api/v1/admin/jobs/{jobId}/cancel`
- `GET /api/v1/admin/system-info` (sanitized/restricted)
- `POST /api/v1/admin/maintenance-mode`

Dashboard example:
```json
{"publishedTests":42,"draftTests":9,"activeQuestions":1250,"attemptsToday":186,"failedImports":2,"recentActivity":[{"action":"TEST_PUBLISHED","resourceId":"test_2001","at":"2026-10-19T17:30:00Z"}]}
```
Settings update:
```json
{"defaultNegativeMarkingEnabled":true,"defaultResultPolicy":"AFTER_SUBMIT","version":4}
```
Audit all setting changes. Never expose environment secrets or internal exception traces.

---

## 14. Security, privacy and reliability

- Modern adaptive password hashing via Spring Security; HTTPS.
- Backend role, ownership and resource-level checks.
- Secure token/cookie strategy; refresh rotation/revocation if used.
- Rate limiting for login/reset; restrictive CORS and appropriate CSRF protection.
- Validate request DTOs and sanitize/validate uploaded files.
- No answer-key leakage during active tests.
- Server-side score/deadline; idempotent submit; transactional result creation.
- Audit high-impact admin mutations and redact secrets.
- Least-privilege DB account, separate environment secrets, no secrets committed.
- Backups, restore drills, retention policy and safe logs.
- Define performance targets and load-test the runner before launch.

---

## 15. Testing strategy

**Unit:** scoring/negative marking, accuracy vs percentage, grade boundaries, publication validation, deadline/timeout, question selection, dependency deletion, DTO validation.

**Integration:** create exam→subject→chapter→question→test→publish; student start/save/resume/submit/result; duplicate submit; cross-user access blocked; answer-key leakage blocked; stale admin update rejected; archive/restore dependencies; imports report row-level errors.

**Frontend:** create/edit validation, dirty-form warning, autosave/reconnect, timer, responsive navigation, keyboard accessibility, loading/empty/error/permission states.

**E2E:** Admin builds and publishes test → Student completes → result/history persist → Admin edits future revision → old result stays unchanged → archive/restore works → audit trail captures actions.

---

## 16. Repo, Git, Jira, Docker and CI/CD

Repository:
```text
eduforge/
  frontend/eduforge-web/
  backend/eduforge-api/
  infrastructure/docker/
  docs/requirements/ docs/design/ docs/architecture/
  docs/database/ docs/api/ docs/testing/ docs/runbooks/
  .github/workflows/ docker-compose.yml .env.example
  README.md CONTRIBUTING.md
```
Git: `main` remains releasable; short-lived branches such as `feature/EDU-12-question-editor`; PRs require CI/review; no `.env` or real student data in Git.

Jira epics: Foundation/Auth; Taxonomy; Question Bank; Test Builder; Attempt Engine; Results/Analytics; Admin/Audit; Security/Privacy; Testing/Deployment.

CI: backend compile/unit/integration/static checks; frontend lint/tests/build; dependency/image scan; controlled migrations; separate dev/staging/prod config; health/readiness checks and rollback runbook.

---

## 17. Milestones and definition of done

- **M0:** decisions, PRD, ERD, OpenAPI, Figma wireframes.
- **M1:** repo, Angular shell, Spring Boot shell, PostgreSQL, migrations, auth, CI.
- **M2:** taxonomy CRUD and update screens.
- **M3:** question editor, revisions, usage, import/export.
- **M4:** test builder, settings, preview, validation, publish lifecycle.
- **M5:** attempt engine, autosave/resume/submit/timeout/security.
- **M6:** results, review, history, analytics.
- **M7:** user admin, audit, reports, settings, restore/trash.
- **M8:** accessibility, load/security tests, backups and release.

Definition of done for each screen/API: requirements/permissions documented; DTOs/validation/errors defined; authorization tested; version/audit behaviour decided; tests pass; OpenAPI updated; loading/empty/error/success UI done; keyboard/responsive reviewed; no sensitive data in logs; Jira/docs updated.

---

## 18. Decisions to finalize before coding

1. Can questions belong to multiple chapters or only one?
2. MVP question types: single-correct MCQ only, or multiple-correct/numerical too?
3. Should practice mode reveal answers immediately?
4. When can explanations be shown?
5. Editing a published test: force unpublish or create new revision?
6. Account deletion and data-retention policy?
7. Negative marks global, per-test or per-question?
8. Results immediately or at configured time?
9. Cookie/refresh-token approach for browser auth?
10. CSV only or CSV + XLSX import?
11. Multilingual and image-based questions in MVP?
12. Student-created quizzes excluded from MVP? Recommendation: yes.

## 19. Next actions
1. Approve or adjust the open decisions.
2. Freeze deletion, revision, publication and result-visibility policies.
3. Create ERD and database migration plan.
4. Convert this inventory into `openapi.yaml` and validate all DTO examples.
5. Design Figma wireframes first for Test Runner, Result Review, Question Editor and Test Builder.
6. Create Jira stories and repository skeleton.
7. Build the first vertical slice: Admin creates exam → subject → chapter → question → test → publish; Student attempts it and sees a persisted result.

**Important:** This is a detailed planning baseline, not a claim that the endpoints are already implemented. API examples are proposed contracts. Finalize open decisions, security review and OpenAPI before production coding.


---

## 20. Screen wireframe sketches (low fidelity)

These are structural wireframes, not final visual designs. Build them in Figma with responsive variants.

### A. Student Test Runner — desktop

```text
┌────────────────────────────────────────────────────────────────────────────┐
│ EduForge   Modern India Test 1      Saved ✓      31:42 remaining   [Submit] │
├──────────────────────────────────────────────────────┬─────────────────────┤
│ Question 04 of 20                                    │ QUESTION PALETTE    │
│                                                      │ [01✓] [02✓] [03○]   │
│ Which major event took place in 1857 in India?       │ [04●] [05○] [06⚑]   │
│                                                      │ [07○] [08✓] [09○]   │
│  ○ A. Partition of Bengal                            │                     │
│  ● B. Revolt of 1857                                 │ Legend:             │
│  ○ C. Dandi March                                    │ ✓ Answered          │
│  ○ D. Quit India Movement                            │ ○ Not answered      │
│                                                      │ ⚑ Review            │
│ [Previous] [Mark for Review] [Clear] [Save & Next]   │ Answered: 8/20      │
└──────────────────────────────────────────────────────┴─────────────────────┘
```

### B. Admin Question Editor — desktop

```text
┌────────────────────────────────────────────────────────────────────────────┐
│ Admin / Question Bank / Edit Q-1001        Draft • Version 3   [Preview]    │
├───────────────────┬───────────────────────────────────────┬────────────────┤
│ ADMIN SIDEBAR     │ QUESTION DETAILS                      │ LIVE PREVIEW   │
│ Dashboard         │ Exam       [UPSC Civil Services   ▼]  │ Q: Which event │
│ Exams & Syllabus  │ Subject    [History               ▼]  │ took place in  │
│ Question Bank    │ Chapter    [Modern India          ▼]  │ 1857?          │
│ Test Builder     │ Difficulty [Medium                ▼]  │ ○ A. ...       │
│ Attempts         │ Question text [....................]  │ ○ B. ...       │
│ Users            │ Options:                              │                │
│ Reports          │ [A] [Option text........] [Correct ✓] │                │
│ Audit Logs       │ [B] [Option text........] [Correct ]  │                │
│ Settings         │ [+ Add option] Explanation [........]│                │
│                  │ Marks [2] Negative marks [0.5]        │                │
│                  │ [Cancel] [Save Draft] [Save Changes]  │                │
└───────────────────┴───────────────────────────────────────┴────────────────┘
```

### C. Admin Test Builder

```text
┌────────────────────────────────────────────────────────────────────────────┐
│ Admin / Test Builder / Modern India Test 1      Draft v2   [Preview]        │
├────────────────────────────────────────────────────────────────────────────┤
│ 1. Details  ──  2. Questions  ──  3. Rules  ──  4. Review & Publish         │
├─────────────────────────────────────┬──────────────────────────────────────┤
│ Test details                        │ Test summary                         │
│ Title [Modern India Test 1       ]  │ Questions: 20                        │
│ Exam  [UPSC Civil Services      ▼]  │ Total marks: 40                      │
│ Chapter [Modern India           ▼]  │ Duration: 30 min                     │
│                                     │ Negative marking: 0.5                │
│ Question bank                       │ Status: Draft                        │
│ Search [1857                 ]      │ Validation: 2 warnings               │
│ Difficulty [Any ▼] [Filter]        │                                      │
│ □ Q-1001  Revolt of 1857     [+]    │ [Run Validation]                     │
│ □ Q-1002  Governor-General  [+]     │ [Save Draft] [Publish]               │
│ Selected questions                 │                                      │
│ 1. Q-1001  [↑] [↓] [Remove]         │                                      │
│ 2. Q-1002  [↑] [↓] [Remove]         │                                      │
└─────────────────────────────────────┴──────────────────────────────────────┘
```

### D. Admin Dashboard

```text
┌────────────────────────────────────────────────────────────────────────────┐
│ EduForge Admin                              Search...      Admin Profile ▼ │
├───────────────────┬────────────────────────────────────────────────────────┤
│ Dashboard         │ Overview                                               │
│ Exams & Syllabus  │ ┌────────────┐ ┌────────────┐ ┌────────────┐ ┌────────┐ │
│ Question Bank    │ │ Published  │ │ Draft tests│ │ Questions  │ │Attempts│ │
│ Test Builder     │ │ 42 tests   │ │ 9 tests    │ │ 1,250      │ │ 186    │ │
│ Attempts         │ └────────────┘ └────────────┘ └────────────┘ └────────┘ │
│ Users            │                                                        │
│ Reports          │ Recent activity                   Quick actions         │
│ Audit Logs       │ • Test published                  [+ Create exam]       │
│ Settings         │ • Question updated                [+ Add question]     │
│                  │ • Import completed                [+ Build test]       │
└───────────────────┴────────────────────────────────────────────────────────┘
```

### E. Result Summary

```text
┌────────────────────────────────────────────────────────────────────────────┐
│ Result: Modern India Test 1                                                │
├────────────────────────────────────────────────────────────────────────────┤
│  SCORE             SCORE %           ACCURACY          TIME                 │
│  11 / 20            55%               75%               22m                  │
├────────────────────────────────────────────────────────────────────────────┤
│ Correct: 6       Incorrect: 2       Unanswered: 2       Attempted: 8        │
│                                                                            │
│ [Review answers]   [Retake test, if allowed]   [Back to chapter]            │
└────────────────────────────────────────────────────────────────────────────┘
```

---

## 21. Endpoint-by-endpoint example rules

For every API in Section 12, the final `openapi.yaml` must contain:
- A concrete path and method.
- Authentication/role requirement.
- Path/query parameters and a valid example.
- Request-body example for POST/PUT/PATCH/DELETE-with-body operations.
- Success response example, including status code.
- Validation, permission, not-found and conflict error examples where relevant.
- Pagination example for collection endpoints.
- Idempotency/concurrency notes for submit, imports, publish, bulk actions and updates.

### Standard request/response examples by operation family
| Operation family | Example request | Example success response |
|---|---|---|
| List/search | `GET /api/v1/admin/questions?page=0&size=25&status=DRAFT` | `200 {"items":[{"id":"q_1001","status":"DRAFT"}],"page":0,"size":25,"totalElements":1,"totalPages":1}` |
| Read one | `GET /api/v1/admin/questions/q_1001` | `200 {"id":"q_1001","text":"Example question","status":"DRAFT","version":1}` |
| Create | `POST /api/v1/admin/subjects/sub_history/chapters` body `{"name":"Modern India","slug":"modern-india","status":"DRAFT"}` | `201 {"id":"ch_modern","name":"Modern India","status":"DRAFT","version":0}` |
| Partial update | `PATCH /api/v1/admin/chapters/ch_modern` body `{"name":"Modern Indian History","version":0}` | `200 {"id":"ch_modern","name":"Modern Indian History","version":1}` |
| Delete | `DELETE /api/v1/admin/questions/q_1001` | `204` if permitted; `409 {"code":"DEPENDENCIES_EXIST","message":"Question is used by published tests."}` if blocked |
| Archive/restore | `POST /api/v1/admin/tests/test_2001/archive` | `200 {"id":"test_2001","status":"ARCHIVED"}` |
| Publish | `POST /api/v1/admin/tests/test_2001/publish` | `200 {"id":"test_2001","status":"PUBLISHED","testRevision":1}` |
| Start attempt | `POST /api/v1/tests/test_2001/attempts` body `{"clientRequestId":"start_7f9d"}` | `201 {"attemptId":"att_9001","status":"IN_PROGRESS","deadlineAt":"2026-10-19T18:20:00Z"}` |
| Save answer | `PUT /api/v1/attempts/att_9001/answers/q_1001` body `{"selectedOptionIds":["opt_a"],"clientSequence":4}` | `200 {"saveStatus":"SAVED","questionId":"q_1001"}` |
| Submit | `POST /api/v1/attempts/att_9001/submit` body `{"submissionRequestId":"submit_abc123"}` | `200 {"attemptId":"att_9001","status":"SUBMITTED","score":11,"scorePercentage":55.0,"accuracy":75.0}` |
| Bulk update | `PATCH /api/v1/admin/questions/bulk` body `{"questionIds":["q_1001","q_1002"],"changes":{"difficulty":"EASY"}}` | `200 {"updatedCount":2,"failedCount":0,"failures":[]}` |
| Import job | `POST /api/v1/admin/questions/import` multipart file + `dryRun=true` | `202 {"jobId":"job_import_01","status":"QUEUED"}` |
| Export | `GET /api/v1/admin/questions/export?chapterId=ch_modern&format=csv` | `200` CSV stream or `202` job if asynchronous |
| Analytics | `GET /api/v1/admin/analytics/attempts?from=2026-10-01&to=2026-10-19` | `200 {"series":[{"date":"2026-10-19","attempts":186}],"total":186}` |
| Audit logs | `GET /api/v1/admin/audit-logs?page=0&size=50` | `200 {"items":[{"id":"log_1","action":"QUESTION_UPDATED","resourceId":"q_1001"}],"page":0,"size":50}` |
| Settings update | `PATCH /api/v1/admin/settings` body `{"defaultResultPolicy":"AFTER_SUBMIT","version":4}` | `200 {"defaultResultPolicy":"AFTER_SUBMIT","version":5}` |
| Job status | `GET /api/v1/admin/jobs/job_import_01` | `200 {"jobId":"job_import_01","status":"COMPLETED","progress":100}` |

The family examples above supplement, rather than replace, the specific examples in Section 13. Before implementation is declared ready, generate an OpenAPI operation for each route in Section 12 and include concrete examples for all route parameters, request bodies and success/error responses.
