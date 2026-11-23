# FindMyClass — Business Requirements Document (BRD)

**Version:** 0.1 — Draft for completion

**Date:** 20 November 2026

**Product owner / requirements decision-maker:** ldnang

**Status:** Not yet approved for full implementation

## 1. How to Use This Document

This document consolidates the conversation context, the updated functionality diagram, and the repository README. Requests quoted in the conversation file are reference material; this document does not itself authorize implementation or product release.

- **Confirmed:** explicitly requested or selected by the product owner.
- **Proposed:** an option for discussion, not an agreed decision.
- **TBD:** to be determined; provide an answer in section 12.
- **Existing:** behavior described in the current codebase README; it may need adjustment after the BRD is finalized.

You can answer directly below each question in section 12 or use its ID, for example `Q01: ...`. An unanswered proposal does not automatically become a requirement. After answers are provided, update the version and reflect agreed decisions in the corresponding business rules, requirements, and acceptance criteria.

## 2. Business Context and Objectives

### 2.1 Problem Statement

Independent teachers need a place to introduce themselves, publish courses, share enrollment information, and manage applicants. Students need to find suitable classes, review trustworthy information, and track their applications.

FindMyClass combines enrollment tools for individual teachers with a marketplace that helps students discover classes. The product owner has moved the marketplace into the MVP scope.

### 2.2 Objectives

| ID | Objective | Proposed Measurement |
|---|---|---|
| BO-01 | Enable teachers to create and share their own enrollment pages | Number of teachers who complete their pages and publish at least one course |
| BO-02 | Enable students to find classes and submit applications | Course detail views and valid applications |
| BO-03 | Help teachers manage applications through enrollment | Applications processed and students confirmed as enrolled |
| BO-04 | Measure enrollment performance by acquisition source | Views, applications, and conversions using agreed definitions |
| BO-05 | Validate teachers' willingness to pay | Teachers who use the service and actually purchase or renew Pro |

Numerical targets, measurement periods, and success conditions: **TBD — Q17**. Prices of VND 149,000/month and VND 299,000/month mentioned in the conversation are experimental ideas, not agreed selling prices. This BRD does not validate market demand, revenue projections, or competitor claims.

## 3. Users and Access

| User Type | Intended Needs / Permissions | Status |
|---|---|---|
| Teacher | Manage their own profile, courses, enrollment page, applications, and analytics | Confirmed at the feature level |
| Student | Discover classes, view courses, and apply | Confirmed |
| Parent | Find classes and apply on behalf of a child; relationship to student accounts | Included in the diagram; rules TBD — Q02 |
| Unauthenticated visitor | Browse the marketplace/enrollment pages; permission to submit applications | Proposed / TBD — Q01 |
| Administrator | Operate the platform, moderate content, and support users | Included in the diagram; MVP scope TBD — Q11 |

**Existing:** Google sign-in; users select either Teacher or Student once; teachers complete their profiles before accessing the dashboard. Whether to retain the single-role restriction or add other sign-in methods is not confirmed in this BRD — see Q03.

## 4. Release Scope

### 4.1 MVP Confirmed at the Feature-Group Level

1. Registration/sign-in for teachers and students.
2. Teacher course creation and management.
3. Shareable teacher enrollment pages.
4. Class marketplace: search, filtering, and course details.
5. Application forms and CRM for managing applications.
6. Analytics for views, applications, and student acquisition sources.
7. Basic Free and Pro plans.

### 4.2 MVP Details Requiring Confirmation

- Parent/child profiles, administration, and course approval.
- Enrollment page customization, image uploads, and QR codes.
- Email, Zalo, SMS, or web-only notifications.
- Pro benefits, limits, purchase, renewal, and downgrade rules.
- Enrollment counts, duplicate applications, full classes, cancellations, and schedule changes.

### 4.3 Deferred According to the Context

- Online tuition payments, reconciliation, and refunds.
- AI-assisted content or enrollment follow-up.
- Teacher reviews/ratings and course advertising.
- Native iOS/Android applications.
- Premium plans and advanced growth features.

**Note:** tuition payments and Pro subscription billing are separate business processes. Deferring tuition payments does not determine how Pro will be sold in the MVP.

## 5. Main Business Flows

### UF-01 — Teacher Enrollment Flow

Sign in → complete profile → create course → review information → publish under the agreed approval process → share link/QR → receive applications → follow up and update status → confirm enrollment → review analytics.

Branches to handle: invalid data, incomplete profiles, plan limits exceeded, insufficient permissions, hidden courses, full classes, and changes to courses that already have applications.

### UF-02 — Student Discovery and Application Flow

Open the marketplace or a teacher's link → search/filter classes → view course details and teacher profile → sign in if required by policy → submit application → receive submission confirmation → track the outcome/schedule within the agreed scope.

Distinguish **application submitted** from **accepted for enrollment**. Acceptance and seat reservation rules: TBD — Q04/Q05.

### UF-03 — Teacher Applicant Management

View applications by course/status → review contact details → record follow-up notes → make valid status transitions → retain change history → update enrollment counts according to capacity rules.

### UF-04 — Subscription Plans

View plans and benefits → request an upgrade or pay using the selected mechanism → activate → apply limits → handle expiry/downgrades. The detailed flow is not finalized — Q14.

## 6. Functional Requirements and Acceptance Criteria

The criteria below are **proposals to be finalized**, not evidence that features have been implemented or tested. Rows depending on TBD questions become complete specifications only after those questions are answered.

| ID | Requirement | Proposed Acceptance Criteria | Dependencies |
|---|---|---|---|
| FR-01 | Sign-in and authorization | Users reach the appropriate onboarding step after sign-in; they cannot read/edit another account's private data; logout ends the application session | Q03 |
| FR-02 | Teacher profile | Teachers can save required information; public data and private contact details are distinguished; updates persist after reload | Q08, Q16 |
| FR-03 | Create/edit courses | Save title, description, subject, schedule, date range, tuition, capacity, and delivery mode; invalid data produces clear messages | Q06, Q07 |
| FR-04 | Publish/hide courses | Only the owner or an authorized administrator can change status; the marketplace reflects the change; existing links follow the agreed policy | Q10, Q11 |
| FR-05 | Enrollment page | A dedicated link opens on desktop/mobile and displays the correct teacher's profile and public courses | Q08 |
| FR-06 | Shareable link/QR | The link/QR opens the correct page; valid source parameters are retained for application attribution under the analytics rules | Q08, Q13 |
| FR-07 | Marketplace | Search and filter by subject, area, tuition, online/offline mode, and schedule; combined filters return correct results, with empty states and pagination | Q07, Q09 |
| FR-08 | Course details | Enrollment information, teacher, schedule, tuition, and application availability match current data | Q05, Q06, Q10 |
| FR-09 | Submit applications | Valid data creates an application linked to the correct course and teacher; the applicant receives confirmation; repeated submissions follow the agreed policy | Q01, Q02, Q04, Q05 |
| FR-10 | Teacher CRM | Teachers only see applications for their own courses, can filter lists, add notes, and make valid status changes; the actor and time of changes are recorded | Q04, Q12, Q16 |
| FR-11 | Student application tracking | If included in the MVP, students see only their own applications, current statuses, and relevant schedules; cancellation follows the agreed policy | Q01, Q02, Q05, Q12 |
| FR-12 | Analytics | Show views and applications by source and period; detailed totals match report totals; sample figures are reconciled against agreed definitions | Q13, Q17 |
| FR-13 | Free/Pro | Benefits and limits are enforced by the backend; users receive explanations when limited; expiry/downgrades do not remove data outside the agreed policy | Q14 |
| FR-14 | Basic administration | If included in the MVP, administrators can perform authorized actions; moderation changes include reasons and history | Q11 |
| FR-15 | Confirmations/notifications | Users can distinguish successful submission, acceptance, rejection, and cancellation; delivery channels and failure handling follow the agreed policy | Q12 |

## 7. Business Rules Requiring Decisions

| ID | Rule | Status |
|---|---|---|
| BR-01 | Private teacher, student, and application data is accessible only with appropriate permissions; public data must be explicitly defined | Proposed mandatory rule; Q16 |
| BR-02 | Teachers must complete their profiles before publishing courses | Profile completion currently gates dashboard access; proposed extension to course publication; Q08 |
| BR-03 | An application can progress through New → In consultation → Confirmed → Attending; add Cancelled/Rejected | Proposed; meanings and transitions in Q04 |
| BR-04 | Reduce available seats only when the teacher confirms enrollment; concurrent actions must not exceed capacity | Proposed; Q05 |
| BR-05 | A student cannot have two active applications for the same course | Proposed; student identity and reapplication after cancellation in Q01/Q02/Q05 |
| BR-06 | A course appears in the marketplace only when required information and publication conditions are satisfied | Proposed; Q10/Q11 |
| BR-07 | Schedules repeat weekly within start/end dates, using Vietnam's time zone | Proposed; Q06 |
| BR-08 | Acquisition sources may be recorded using UTM parameters; visits without a source receive a separate label | Proposed; attribution rules in Q13 |
| BR-09 | Free/Pro differ only in published benefits/limits; direct API calls cannot bypass limits | Proposed; Q14 |
| BR-10 | Tuition has an explicit unit: per course, month, or session; payment status must not be inferred from an application | Proposed; Q07 |

## 8. Business Information to Manage

This is an information inventory, not a database schema or API design.

| Category | Proposed Information | Clarification Needed |
|---|---|---|
| Account | Identifier, name, email, role, status | Multiple roles, parents, account suspension |
| Teacher | Biography, subjects, images, area, enrollment link | Qualification verification; public/private contact details |
| Course | Teacher, content, subject, level, delivery mode, location, price and pricing unit, capacity, schedule, status | Required fields; application deadline; edit/hide/delete rules |
| Application | Course, learner/contact person, submission time, status, source | Guest applications; parents applying for multiple children |
| CRM history | Notes, status changes, actor, and timestamp | Who can view/edit it and retention period |
| Analytics event | Page/course, event type, timestamp, and source | Deduplication, bots, privacy, retention period |
| Subscription | Plan, limits, effective/expiry dates, status | Plan payments, renewal, downgrade |

## 9. Quality and Operational Requirements

The following are proposals for an MVP that can be piloted with real users; quantitative targets require product owner decisions.

- **NFR-01 — Interface:** responsive web application; core actions work on mobile and desktop; UI language and currency/date/time conventions: Q15.
- **NFR-02 — Security:** backend authorization; no credentials in frontend/source; protect sessions and data-changing actions; do not log passwords/tokens.
- **NFR-03 — Privacy:** restrict exposure of email, phone numbers, addresses, and learner data; handle child/parent data according to the agreed policy: Q16.
- **NFR-04 — Consistency:** repeated submissions, concurrent confirmations, capacity changes, and schedule changes preserve business data integrity; errors must not appear as successes.
- **NFR-05 — Performance:** dataset size, concurrent users, and response-time targets: Q18. Measure the marketplace with representative data rather than an empty list.
- **NFR-06 — Usability:** labeled forms, field-specific errors, keyboard access, and clear loading, session-expiry, empty-result, and network-error states.
- **NFR-07 — Operations:** environment configuration, data migrations, and diagnostic logs without secrets; hosting, backup/recovery, and ownership: Q18.

## 10. Metrics and MVP Acceptance Conditions

### 10.1 Proposed Metrics

- Teacher activation: teachers with complete profiles and at least one public course.
- Enrollment: valid applications and confirmed enrollments; distinguish these from button clicks/resubmissions.
- Conversion: confirmed enrollments / valid applications within the same measurement cohort. Time window and treatment of older applications: Q13/Q17.
- Acquisition sources: views and applications by defined source; accurate cross-device user tracking is not promised.
- Willingness to pay: actual Pro purchases/renewals; an “Upgrade” click does not count as revenue.

### 10.2 Proposed Completion Criteria

1. Questions blocking core flows are answered; release scope and acceptance criteria are finalized.
2. Teacher flow: sign in → create/publish course → share → receive applications → manage CRM → view analytics, using persisted real data.
3. Student flow: search/filter → view details → submit application → receive confirmation/track outcome under the selected policy.
4. Test unauthorized access, invalid data, duplicate submissions, full classes, and concurrent confirmations; verify plan benefits in the backend.
5. Reloading retains saved data; other accounts cannot read private data; mobile layouts do not obstruct core actions.
6. Provide local startup instructions, explicitly identified demo data, test results, and remaining limitations.
7. Record acceptance with real OAuth and the target environment separately. Simulated OIDC/H2/mocked API tests do not replace evidence of real sign-in or production deployment.

## 11. Constraints, Dependencies, and Risks

- **Existing platform:** React/TypeScript, Spring Boot/Java 21, Oracle. This is the repository's stack; reuse is proposed, and no stack replacement has been requested.
- **Current system:** the README describes Google sign-in, role selection, teacher profiles, and basic dashboards. New feature groups in this BRD are not considered implemented merely because they appear in the document.
- **External dependencies:** correct Google OAuth configuration and environment-specific redirect URIs; database and hosting; notification/payment providers only if selected for the MVP.
- **Content quality:** impersonation, incorrect class information, and teacher verification need an accountable policy owner, not just UI handling.
- **Scale:** the marketplace needs real classes to provide value; target metrics and teacher/student pilot recruitment plans are not finalized.
- **Implementation boundaries:** the BRD describes business needs. Architecture, schema/API, UI design, backlog, and test plans will follow finalized requirements; the BRD need not contain the entire technical design.

## 12. Questions for the Product Owner

**Priority A:** directly affects flows and the data model; answer before preparing a full implementation specification. **Priority B:** must be settled before acceptance/release and can be completed after Priority A. A feature may be deferred through an explicit answer.

### Q01 — Is sign-in required to apply for a course? [A]

Proposed: anyone can browse the marketplace and enrollment pages, but sign-in is required before applying. If guest applications are allowed, define contact verification, spam prevention, and outcome tracking.

**Answer:** _TBD_

### Q02 — Are parents and younger students included in the MVP? [A]

Who owns the account? Can a parent apply for multiple children? What child information needs to be stored? If unsupported initially, should parents be explicitly excluded from the MVP?

**Answer:** _TBD_

### Q03 — Sign-in and roles [A]

Keep Google-only sign-in or add email/password/phone? Can an account both teach and study? Can users change roles? The repository currently allows one role to be selected once.

**Answer:** _TBD_

### Q04 — Is an application a consultation request or an enrollment? [A]

Proposed: form submission creates a request, and the teacher decides whether to accept the student. Define statuses, what “Confirmed” means, who can change status, whether backward transitions are allowed, and the history to retain.

**Answer:** _TBD_

### Q05 — Capacity, duplicate applications, cancellations, and full classes [A]

When are seats reserved/consumed? Is there a waiting list? Does cancellation release a seat? Can students reapply after cancellation/rejection? Is there an application deadline, and can capacity be reduced below the number already accepted?

**Answer:** _TBD_

### Q06 — Class schedules and schedule changes [A]

Proposed: multiple fixed weekly time slots within start/end dates, using Asia/Ho_Chi_Minh. Are individual sessions, holidays, makeup classes, conflict detection, multiple locations, or online meeting links required? Who is notified when schedules change?

**Answer:** _TBD_

### Q07 — Course information, tuition, and categories [A]

Is tuition charged per course/month/session? Are free classes allowed? Which currency? Are subjects, levels, and areas selected from catalogs or entered as free text? Which fields are required for publication?

**Answer:** _TBD_

### Q08 — Teacher profiles and enrollment pages [B]

What is needed for avatars/cover images, qualifications, experience, phone numbers, addresses, theme colors, custom slugs, and QR codes? Is teacher verification required? What happens to old links when a slug changes?

**Answer:** _TBD_

### Q09 — Marketplace search and filters [A]

Are areas defined by province/district, distance radius, or both? Which weekdays/time slots can the schedule filter use? What pricing unit is used to compare tuition ranges? Search course titles, teachers, subjects, or all of them? What is the default sort order?

**Answer:** _TBD_

### Q10 — Course status and lifecycle [A]

Does publication take effect immediately or require approval? Which fields can change after students have applied? Can a hidden course still be opened through a direct link? Do completed courses remain in the marketplace? Is deletion allowed, or only archiving?

**Answer:** _TBD_

### Q11 — MVP administration/moderation [B]

Is an administrator dashboard needed immediately? Who grants administrator access? Define permissions to approve/hide classes, suspend accounts, handle reports, view contact details, and grant Pro subscriptions.

**Answer:** _TBD_

### Q12 — Tracking, notifications, and CRM [B]

Do students need a “My applications” page and cancellation rights? Can teachers edit contact details, manually add applications, or export lists? Are notifications web-only or sent by email/Zalo/SMS? Which events trigger notifications, and who pays provider fees?

**Answer:** _TBD_

### Q13 — Analytics and attribution definitions [A]

Count total views or unique visitors? How are teacher visits/bots excluded? Track teacher pages, course pages, or both? Does the source come from UTM, referrer, or student selection? Use first-touch/last-touch attribution, retain it for how long, and handle missing sources how?

**Answer:** _TBD_

### Q14 — Free/Pro plans [A]

What are the exact prices, benefits, and limits? Activate Pro manually after a bank transfer or through automated payments? Are trials, renewals, downgrades, and grace periods supported? On expiry, how are existing data and courses handled if limits are exceeded?

**Answer:** _TBD_

### Q15 — Language and user experience [B]

Should the entire UI be Vietnamese, English, or bilingual? The login introduction was translated into English at the owner's earlier request; that does not decide the language of the whole product. Are there preferred UI references, logos, and date/currency formats?

**Answer:** _TBD_

### Q16 — Private data and contact information [A]

Who can see teacher/student phone numbers, emails, and addresses? When do students consent to sharing information with teachers? Are terms, a privacy policy, account/data deletion, and child-data retention rules needed? How long are applications, notes, and analytics events retained?

**Answer:** _TBD_

### Q17 — Business pilot objectives [B]

Which teachers/subjects/areas are targeted first? What are the targets for teachers publishing classes, applications, enrollments, and Pro purchases? Over what period? Which thresholds determine whether to continue, change, or stop the experiment?

**Answer:** _TBD_

### Q18 — Runtime environment and operational expectations [B]

Prioritize a local demo or a public release with real users? Which hosting/domain/budget? What dataset size and concurrent user count? What response-time, backup/recovery, and support expectations apply, and who operates the service?

**Answer:** _TBD_

## 13. Decision Log and Next Steps

| ID | Decision | Status / Source |
|---|---|---|
| D-01 | Support independent teachers in publishing courses, recruiting students, and managing applications | Conversation context |
| D-02 | Include class discovery marketplace in the MVP | Product owner explicitly moved it into the MVP |
| D-03 | MVP includes the seven feature groups in section 4.1 | Context and updated diagram |
| D-04 | Responsive web first; native mobile later | Proposed direction in the context; confirm when finalizing scope |
| D-05 | Plan prices, payment methods, capacity rules, and other detailed policies | Not finalized; do not automatically apply proposed options |

After section 12 is answered: update the BRD → finalize scope and acceptance criteria → create the backlog/user stories and technical design → write a comprehensive implementation prompt based on the finalized BRD version. This draft is not an instruction to build everything or to commit/push/deploy.
