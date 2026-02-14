# Unit Test Report: Passenger Registration Module

| Field       | Value                                    |
|-------------|------------------------------------------|
| **Project** | Lucky3 Ride-Sharing (Web Client)         |
| **Date**    | February 14, 2026                        |
| **Component** | `RegisterComponent`                    |
| **Version** | 1.1.3                                    |
| **Author**  | Milan Sazdov                       |
| **Status**  | **104 / 104 PASSED**                     |

---

## 1. Executive Summary

This document details the unit testing strategy and results for the **Passenger Registration** feature (**Feature 2.2.2**). The test suite was executed against the `RegisterComponent` to ensure compliance with business logic, data integrity, and user experience requirements.

**Key Highlights:**

| Metric                    | Value                                                                                     |
|---------------------------|-------------------------------------------------------------------------------------------|
| Total Tests Executed      | 104                                                                                       |
| Pass Rate                 | **100%**                                                                                  |
| Code Coverage Target      | > 90% Statements, Branches, and Functions                                                 |
| Critical Risk Coverage    | Asynchronous loading states, file upload cancellation, and DTO data mapping fully covered  |

---

## 2. Scope & Traceability

### 2.1 Requirement Mapping

This test suite verifies the following requirements defined in the project specification (*Testiranje softvera 2025/26.pdf*):

| Req ID      | Description                                                                 | Test Coverage                                      |
|-------------|-----------------------------------------------------------------------------|-----------------------------------------------------|
| **REQ-2.2.2** | User Registration (Student 3) — Test the user registration form and verify that the entered data is submitted correctly. | Covered (Data Mapping & Service Integration) |
| **REQ-GEN-01** | Field Validation — All fields must be validated before submission.        | Covered (Field-level & Form-level validators)       |
| **REQ-GEN-02** | Boundary Cases — Testing boundary and exceptional cases.                  | Covered (Boundary values, Error API responses)      |

### 2.2 System Under Test (SUT)

| Property             | Value                                          |
|----------------------|------------------------------------------------|
| **Component**        | `RegisterComponent` (Standalone)               |
| **Form Model**       | Reactive Forms (`FormGroup`)                   |
| **External Deps**    | `AuthService`, `Router`, `localStorage`        |

### 2.3 Form Validation Rules

| Field             | Validators                                        | Rules                                                        |
|-------------------|---------------------------------------------------|--------------------------------------------------------------|
| `firstName`       | `required`, `minLength(2)`                        | Cannot be empty; must be at least 2 characters               |
| `lastName`        | `required`, `minLength(2)`                        | Cannot be empty; must be at least 2 characters               |
| `email`           | `required`, `email`                               | Cannot be empty; must match email format                     |
| `phone`           | `required`, `pattern(/^\+?[\d\s-()]{10,}$/)`     | Cannot be empty; 10+ digits with optional +, spaces, dashes  |
| `address`         | `required`, `minLength(5)`                        | Cannot be empty; must be at least 5 characters               |
| `password`        | `required`, `minLength(8)`                        | Cannot be empty; must be at least 8 characters               |
| `confirmPassword` | `required`                                        | Cannot be empty                                              |
| **Cross-field**   | `passwordMatchValidator`                          | `password` and `confirmPassword` must match when both filled |

### 2.4 DTO Mapping (Form → Backend API)

The component transforms form field names to the `PassengerRegistrationRequest` DTO format before calling `AuthService.register()`:

| Form Field        | DTO Field          | Example Value                    |
|-------------------|--------------------|----------------------------------|
| `firstName`       | `name`             | `Milan`                          |
| `lastName`        | `surname`          | `Pleb`                           |
| `email`           | `email`            | `milanthepleb@gmail.com`         |
| `phone`           | `phoneNumber`      | `0694523210`                     |
| `address`         | `address`          | `Beogradska 23, Petrovaradin`   |
| `password`        | `password`         | `Sifra123!`                      |
| `confirmPassword` | `confirmPassword`  | `Sifra123!`                      |

### 2.5 Submission Flow

```
User fills form → clicks Submit
  ├── IF form invalid → markAllAsTouched() → show validation errors → STOP
  ├── IF loading=true (duplicate click) → STOP
  └── IF valid:
        ├── Set loading=true, error=''
        ├── Build PassengerRegistrationRequest DTO
        ├── Call authService.register(dto, selectedFile?)
        ├── pipe(take(1), finalize(() => loading=false))
        ├── ON SUCCESS:
        │     ├── localStorage.setItem('pendingActivationEmail', email)
        │     └── router.navigate(['/register-verification-sent'], { queryParams, state })
        └── ON ERROR:
              ├── Error instance       → error = err.message
              ├── err.error is string  → error = err.error
              ├── err.error.message    → error = err.error.message
              └── else                 → error = 'Registration failed. Please try again.'
```

---

## 3. Test Strategy & Infrastructure

### 3.1 Framework Configuration

| Component          | Technology                                        |
|--------------------|---------------------------------------------------|
| **Test Runner**    | Karma 6.4.4 (ChromeHeadless)                     |
| **Assertion Lib**  | Jasmine 6.0.1                                     |
| **Mocking**        | `jasmine.createSpyObj` (Zero backend dependency)  |
| **Router**         | Spied navigation to verify state transfer          |
| **Time Control**   | `fakeAsync` / `tick` for async loading states      |

### 3.2 TestBed Configuration

```typescript
TestBed.configureTestingModule({
  imports: [RegisterComponent],       // Standalone component import
  providers: [
    provideRouter([]),                 // Minimal router (no actual routes needed)
    { provide: AuthService, useValue: authServiceSpy }  // Jasmine spy object
  ]
})
```

**Key decisions:**
- `provideRouter([])` is used instead of the deprecated `RouterTestingModule` (Angular 21 compatibility).
- `AuthService` is fully mocked via `jasmine.createSpyObj` — no real HTTP calls are made.
- `Router.navigate` is spied on to verify post-registration navigation without side effects.

### 3.3 Test Data (Context: Serbia / Novi Sad)

Tests utilize localized data to ensure realistic validation formats (e.g., Serbian phone numbering):

```typescript
const validFormData = {
  firstName: 'Milan',
  lastName: 'Pleb',
  email: 'milanthepleb@gmail.com',
  phone: '0694523210',           // Local format
  address: 'Beogradska 23, Petrovaradin',
  password: 'Sifra123!',
  confirmPassword: 'Sifra123!'
};
```

### 3.4 Patterns Used to Avoid Angular Test Pitfalls

| Problem | Solution | Where Applied |
|---------|----------|---------------|
| **NG0100** `ExpressionChangedAfterItHasBeenCheckedError` | Create a fresh `TestBed.createComponent()` and set state **before** the first `detectChanges()` | Submit button state, password type, error/file display tests |
| **Zone.js uncaught errors** from `throwError()` | Use `Subject<any>` pattern — subscribe first via `onSubmit()`, then call `subject.error(...)` | All error handling tests |
| **Synchronous Observable** completing before assertion | Use `of({}).pipe(delay(1000))` with `fakeAsync`/`tick(1000)` to test intermediate states | In-flight loading state test |

---

## 4. Test Execution Results

### 4.1 Summary Metrics

| Category                                        | Count  | Status     |
|-------------------------------------------------|--------|------------|
| Smoke Tests (Component Creation)                | 1      | PASS       |
| State Initialization                            | 8      | PASS       |
| Field Validation (Positive / Negative / Boundary) | 33   | PASS       |
| Cross-Field Logic (Password Match)              | 4      | PASS       |
| User Interaction (File Upload, Toggles)         | 8      | PASS       |
| Integration & DTO Mapping                       | 14     | PASS       |
| Error Handling & Resilience                     | 10     | PASS       |
| UI / DOM Verification                           | 26     | PASS       |
| **TOTAL**                                       | **104**| **100% PASS** |

### 4.2 Detailed Breakdown

#### A. Data Integrity & Validation (Business Logic)

| Feature              | Scenario                                                       | Result |
|----------------------|----------------------------------------------------------------|--------|
| Required Fields      | Verify all 7 fields trigger `required` error when empty.       | PASS   |
| Min Length           | Boundary analysis: 1 char fails, 2 chars pass (Name/Surname). | PASS   |
| Email Format         | Verify rejection of no-at-symbol, no-domain.                  | PASS   |
| Password Complexity  | Boundary analysis: 7 chars fails, 8 chars pass.               | PASS   |
| Password Matching    | `pass !== confirm` triggers `passwordMismatch` error.          | PASS   |
| DTO Transformation   | Verify `firstName` maps to `name`, `lastName` maps to `surname`. | PASS |

**Field-Level Edge Cases:**

| Field | Tests | Specific Boundary Values |
|-------|-------|--------------------------|
| `firstName` | 4 | `''` (required), `'M'` (minlength=2), `'Mi'` (boundary pass), `'Milan'` (valid) |
| `lastName` | 4 | `''` (required), `'P'` (minlength=2), `'Pl'` (boundary pass), `'Pleb'` (valid) |
| `email` | 4 | `''` (required), `'invalidemail'` (no @), `'user@'` (no domain), `'milanthepleb@gmail.com'` (valid) |
| `phone` | 5 | `''` (required), `'123'` (too short), `'abcdefghijk'` (letters), `'0694523210'` (10-digit), `'+381 69 452-3210'` (international) |
| `address` | 4 | `''` (required), `'Zmaj'` (minlength=5), `'NS 21'` (boundary pass), `'Beogradska 23, Petrovaradin'` (valid) |
| `password` | 4 | `''` (required), `'Sif1!'` (minlength=8), `'Sifra12!'` (boundary pass), `'Sifra123!'` (strong) |
| `confirmPassword` | 2 | `''` (required), `'something'` (any non-empty valid at field level) |

#### B. User Experience & Asynchronous State

| Feature              | Scenario                                                                                        | Why It Matters (Senior QA Note)                                          | Result |
|----------------------|-------------------------------------------------------------------------------------------------|--------------------------------------------------------------------------|--------|
| **In-Flight Loading** | Simulate 1s network delay. Verify button is disabled and text changes to "Creating Account...". | Prevents double-submission and gives user feedback.                      | PASS   |
| **File Cancellation** | User selects file → opens dialog → clicks Cancel. Verify previous file is not lost.            | Native browser behavior clears the input; our code must prevent this.    | PASS   |
| **Visual Validation** | Verify red error text appears only after user interaction (`touched`), not on load.             | Prevents "aggressive" validation UX.                                     | PASS   |

#### C. Error Handling & Resilience

| Feature             | Scenario                                                                     | Result |
|---------------------|------------------------------------------------------------------------------|--------|
| Duplicate Submit    | Rapid double-clicks do not trigger two API calls (`loading` guard).          | PASS   |
| API Error 409       | Backend returns "Email already exists". UI displays exact message.           | PASS   |
| API Error 400       | Backend returns `{ error: { message: 'Validation failed' } }`. UI extracts. | PASS   |
| API Error 500       | Backend returns generic error. UI displays fallback "Registration failed".   | PASS   |
| Unknown Error Shape | Backend returns unrecognizable format. UI shows safe fallback message.       | PASS   |
| Graceful Recovery   | `loading` spinner resets to `false` even if API fails (`finalize`).          | PASS   |
| No Navigation       | On error, `router.navigate` is NOT called.                                   | PASS   |

#### D. DOM / Template Verification

| Feature                | Scenario                                                          | Result |
|------------------------|-------------------------------------------------------------------|--------|
| Form Structure         | `<form>`, 7 `input[formControlName]`, submit button, file input.  | PASS   |
| Button Default Text    | Shows "Create Account" when not loading.                          | PASS   |
| Button Loading Text    | Shows "Creating Account..." and `disabled` when loading.          | PASS   |
| Error Banner           | Hidden initially; shown with message when `error` is set.         | PASS   |
| File Name Display      | Shows `profilna-slika.png` when file is selected.                 | PASS   |
| Validation Messages    | All 13 distinct messages appear only when control is touched.     | PASS   |
| Untouched Negative     | No validation errors shown for untouched fields.                  | PASS   |
| Password Input Types   | `type="password"` by default; `type="text"` when toggled.        | PASS   |

---

## 5. Code Coverage

### 5.1 Coverage Targets

| Metric          | Target  | Justification                                                       |
|-----------------|---------|---------------------------------------------------------------------|
| **Statements**  | > 90%   | All executable paths in the component are exercised.                |
| **Branches**    | > 90%   | All `if/else` branches (error handler, guard clauses, validators).  |
| **Functions**   | > 90%   | All public methods (`onSubmit`, `onFileSelected`, toggles, `f`).    |
| **Lines**       | > 90%   | Near-total line coverage across 129 lines of component code.        |

### 5.2 How to Generate Coverage Report

```bash
cd web
npx ng test --include="**/register.component.spec.ts" --no-watch --code-coverage
```

The HTML coverage report will be generated in `web/coverage/`. Open `index.html` to view detailed per-line coverage.

### 5.3 Coverage Confidence Assessment

| Component Area           | Estimated Coverage | Notes                                                         |
|--------------------------|--------------------|---------------------------------------------------------------|
| `constructor` / init     | 100%               | All 8 initial state properties verified.                      |
| `passwordMatchValidator` | 100%               | All 4 branches tested (match, mismatch, empty password, empty confirm). |
| `onFileSelected`         | 100%               | File present, empty list, cancellation preservation.          |
| `togglePasswordVisibility` | 100%             | Both toggle directions tested.                                |
| `toggleConfirmPasswordVisibility` | 100%      | Both toggle directions tested.                                |
| `onSubmit` — guard       | 100%               | Invalid form and loading=true paths.                          |
| `onSubmit` — success     | 100%               | DTO build, service call, localStorage, navigation.            |
| `onSubmit` — error       | 100%               | All 4 error format branches + finalize.                       |
| `get f()`                | 100%               | Accessor tested.                                              |
| Template bindings        | ~95%               | All `*ngIf`, `[disabled]`, `[type]`, `{{interpolation}}` verified. |

---

## 6. Defects Prevented & Quality Assurance Value

The writing of these unit tests **proactively identified and prevented** the following potential defects:

### 6.1 Silent Data Corruption (Severity: Critical)

| Defect | Impact | Test That Prevents It |
|--------|--------|-----------------------|
| Form field `firstName` mapped to wrong DTO field | Backend receives `null` for `name`, user registered with no first name | DTO Field Mapping — `should map firstName to name in the DTO` |
| Form field `phone` mapped to wrong DTO field | Backend receives `null` for `phoneNumber`, user unreachable | DTO Field Mapping — `should map phone to phoneNumber in the DTO` |
| Form field `lastName` mapped to wrong DTO field | Backend receives `null` for `surname` | DTO Field Mapping — `should map lastName to surname in the DTO` |

**Business impact prevented:** If a developer renames form controls during refactoring, these 7 tests immediately catch the breakage. Without them, users would be registered with missing names or phone numbers — a data integrity violation that would only surface during manual QA or, worse, in production.

### 6.2 File Upload Loss (Severity: Medium)

| Defect | Impact | Test That Prevents It |
|--------|--------|-----------------------|
| User selects profile photo → opens file dialog → clicks Cancel → previous photo erased | User unknowingly submits registration without their avatar | `should preserve the previously selected file if the user cancels selection` |

**Root cause:** Browsers fire a `change` event with an empty `FileList` when the user cancels the file dialog. A naive implementation (`this.selectedFile = event.target.files[0]`) would set `selectedFile` to `undefined`, erasing the previous selection. The component's `if (file)` guard prevents this.

### 6.3 Race Condition — Duplicate Account Creation (Severity: High)

| Defect | Impact | Test That Prevents It |
|--------|--------|-----------------------|
| User spam-clicks "Create Account" → multiple API calls fire → duplicate account or 409 error | Degraded UX, potential duplicate records if backend race condition exists | `should not call authService.register when already loading` |

**Business impact prevented:** Without the `if (this.loading) return;` guard, rapid double-clicks could send multiple registration requests before the first response arrives. This test ensures the guard exists and works.

### 6.4 Cryptic Error Messages (Severity: Medium)

| Defect | Impact | Test That Prevents It |
|--------|--------|-----------------------|
| Backend returns `{ error: { message: '...' } }` but UI shows `[object Object]` | User sees meaningless error, cannot self-resolve | `should display err.error.message when err.error is an object with message` |
| Backend returns unexpected error format → UI shows nothing or crashes | Silent failure, user stuck | `should display fallback message when error has no recognizable format` |

**Business impact prevented:** The error handler has 4 branches for different backend response shapes. If any branch is broken, users see either `[object Object]`, an empty error banner, or the component crashes entirely. These 6 tests ensure all error paths produce human-readable messages.

### 6.5 Premature Validation Errors (Severity: Low / UX)

| Defect | Impact | Test That Prevents It |
|--------|--------|-----------------------|
| User lands on page → immediately sees 7 red "required" errors | Hostile UX, user intimidated before typing | `should not show validation errors for untouched fields` |

**Business impact prevented:** Validation errors should only appear after user interaction. This negative test ensures the UX remains friendly on initial page load.

---

## 7. Detailed Test Descriptions

### 7.1 Complete Test Suite Map

| # | Describe Block                         | Tests | Category        |
|---|----------------------------------------|-------|-----------------|
| 1 | Component creation                     | 1     | Smoke           |
| 2 | Form Initialization                    | 8     | State           |
| 3 | firstName Validation                   | 4     | Validation      |
| 4 | lastName Validation                    | 4     | Validation      |
| 5 | Email Validation                       | 4     | Validation      |
| 6 | Phone Validation                       | 5     | Validation      |
| 7 | Address Validation                     | 4     | Validation      |
| 8 | Password Validation                    | 4     | Validation      |
| 9 | Confirm Password Validation            | 2     | Validation      |
| 10 | Password Match Validator (cross-field) | 4     | Validation      |
| 11 | Whole Form Validity                   | 3     | Integration     |
| 12 | onFileSelected                        | 3     | Behavior        |
| 13 | Password Visibility Toggles           | 4     | Behavior        |
| 14 | f getter                              | 1     | Accessor        |
| 15 | onSubmit — Guard Clauses              | 4     | Behavior        |
| 16 | onSubmit — Successful Registration    | 7     | Integration     |
| 17 | onSubmit — DTO Field Mapping          | 7     | Data Integrity  |
| 18 | onSubmit — Error Handling             | 6     | Error Paths     |
| 19 | Template Rendering                    | 8     | UI/DOM          |
| 20 | Template — Validation Error Messages  | 14    | UI/DOM          |
| 21 | Template — Submit Button              | 3     | UI/DOM          |
| 22 | Template — Password Input Types       | 4     | UI/DOM          |
|   | **TOTAL**                              | **104** |               |

### 7.2 Cross-Field Password Match Validator (4 tests)

Tests the custom `passwordMatchValidator` which operates at the `FormGroup` level:

| Test | Scenario | Expected |
|------|----------|----------|
| Passwords differ | `Sifra123!` vs `DrugaSifra1!` | `passwordMismatch` error is set |
| Passwords match | `Sifra123!` vs `Sifra123!` | No error |
| Password empty | `''` vs `Nesto123!` | No error (validator bails early) |
| ConfirmPassword empty | `Sifra123!` vs `''` | No error (validator bails early) |

**Why this matters:** The validator intentionally skips comparison when either field is empty (to avoid premature mismatch errors while the user is still typing). This behavior is verified exhaustively.

### 7.3 onSubmit — In-Flight Loading State (fakeAsync)

```
Timeline:
  t=0ms    → onSubmit() called → loading=true, button disabled, text="Creating Account..."
  t=0-999ms→ INTERMEDIATE STATE: user sees spinner, button is unclickable
  t=1000ms → Response arrives → finalize() → loading=false, button re-enabled
```

**Why this test is special:** Using `of({})` alone completes synchronously, so `loading` flips to `true` and immediately back to `false` before any assertion can catch the intermediate state. The `delay(1000)` + `fakeAsync`/`tick` approach lets us **freeze time** and assert the in-flight state — exactly what the user sees while waiting for the backend response.

### 7.4 onSubmit — DTO Field Mapping (7 tests)

Each form→DTO field mapping has its own dedicated test:

| # | Form Field | DTO Field | Value Verified |
|---|------------|-----------|----------------|
| 1 | `firstName` | `name` | `'Milan'` |
| 2 | `lastName` | `surname` | `'Pleb'` |
| 3 | `phone` | `phoneNumber` | `'0694523210'` |
| 4 | `email` | `email` | `'milanthepleb@gmail.com'` |
| 5 | `address` | `address` | `'Beogradska 23, Petrovaradin'` |
| 6 | `password` | `password` | `'Sifra123!'` |
| 7 | `confirmPassword` | `confirmPassword` | `'Sifra123!'` |

### 7.5 Template — Validation Error Messages (14 tests)

| # | Field | Condition | Expected Text |
|---|-------|-----------|---------------|
| 1 | `firstName` | touched + empty | `"First name is required"` |
| 2 | `firstName` | touched + `'M'` | `"Must be at least 2 characters"` |
| 3 | `lastName` | touched + empty | `"Last name is required"` |
| 4 | `email` | touched + empty | `"Email is required"` |
| 5 | `email` | touched + `'bad-email'` | `"Invalid email address"` |
| 6 | `phone` | touched + empty | `"Phone number is required"` |
| 7 | `phone` | touched + `'abc'` | `"Invalid phone number format"` |
| 8 | `address` | touched + empty | `"Address is required"` |
| 9 | `address` | touched + `'Zmaj'` | `"Address is too short"` |
| 10 | `password` | touched + empty | `"Password is required"` |
| 11 | `password` | touched + `'short'` | `"At least 8 characters"` |
| 12 | `confirmPassword` | touched + empty | `"Confirmation is required"` |
| 13 | Both passwords | touched + mismatch | `"Passwords do not match"` |
| 14 | All fields | **untouched** | None of the error messages appear |

---

## 8. How to Run Tests

### Execute Test Suite

```bash
cd web
npx ng test --include="**/register.component.spec.ts" --no-watch --browsers=ChromeHeadless
```

**Expected output:**
```
Chrome Headless X.X.X (Windows 10): Executed 104 of 104 SUCCESS (X.XXX secs / X.XXX secs)
TOTAL: 104 SUCCESS
```

### Generate Coverage Report

```bash
npx ng test --include="**/register.component.spec.ts" --no-watch --code-coverage
```

### Run with Visible Browser (Debugging)

```bash
npx ng test --include="**/register.component.spec.ts" --no-watch --browsers=Chrome
```

---

## 9. Test Data Reference

All test data uses Serbian / Novi Sad-themed values:

| Purpose                  | Value                           |
|--------------------------|---------------------------------|
| First name               | `Milan`                         |
| Last name                | `Pleb`                          |
| Email                    | `milanthepleb@gmail.com`        |
| Phone (local)            | `0694523210`                    |
| Phone (international)    | `+381 69 452-3210`              |
| Address                  | `Beogradska 23, Petrovaradin`   |
| Password                 | `Sifra123!`                     |
| Mismatched password      | `DrugaSifra1!`, `Pogresna1!`    |
| Short address (boundary) | `Zmaj` (4 chars — fails), `NS 21` (5 chars — passes) |
| Short password (boundary)| `Sifra12!` (8 chars — passes), `Sif1!` (5 chars — fails) |
| Profile image            | `profilna-slika.png`            |

---

## 10. Conclusion

The `RegisterComponent` has **passed all 104 unit tests**. The component is deemed **stable and ready for integration testing**.

The test suite provides a high degree of confidence that:

1. **Invalid data cannot be submitted** — all 7 fields are validated with boundary analysis, and the form blocks submission until every constraint is satisfied.
2. **Valid data is transformed correctly for the API** — the critical `firstName→name`, `lastName→surname`, `phone→phoneNumber` mapping is individually tested to prevent silent data corruption.
3. **The user is protected from UI glitches during network requests** — the `fakeAsync` in-flight test proves the loading spinner, disabled button, and text change all work during the pending HTTP request.
4. **The application handles server errors gracefully** — all 4 error response shapes from the backend are parsed into human-readable messages, and the `finalize` operator ensures recovery.

---

**Sign-off:**

| Developer                     | Project                     |
|-------------------------------|-----------------------------|
| Milan Sazdov                  | Lucky3 Ride-Sharing Project |
