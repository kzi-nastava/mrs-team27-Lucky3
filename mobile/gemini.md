# Gemini project context

This Android project uses Java and follows a ViewModel + LiveData + Retrofit pattern.

## Core structure

Packages:
- `Domain` — UI/presentation models used by adapters and fragments.
- `models` — API/request/response models mapped directly to backend payloads.
- `services` — Retrofit interfaces.
- `ui` — Fragments, activities, adapters, dialogs, and other UI classes.
- `utils` — Helpers such as `ClientUtils`, `SharedPreferencesManager`, `NavbarHelper`, `ListViewHelper`.
- `viewmodels` — `ViewModel` and `AndroidViewModel` classes that own state, filtering, searching, and API calls.

## Technology and conventions

- Language: Java only unless explicitly asked otherwise.
- UI: XML layouts, ViewBinding, Material components, custom drawables.
- Networking: Retrofit services accessed through `ClientUtils`.
- State: `LiveData` and `MutableLiveData`.
- Auth: bearer token from `SharedPreferencesManager`.
- Async work: Retrofit `enqueue(...)` callbacks.
- Logging: use `Log.d`, `Log.e` with a `TAG` constant.
- Fragments must use `getViewLifecycleOwner()` when observing LiveData.
- Fragments must set binding to null in `onDestroyView()`.

## Architecture rules

### ViewModels
- Keep business logic, filtering, searching, loading states, and API calls inside ViewModels.
- Expose immutable `LiveData` to UI.
- Keep `MutableLiveData` private.
- Store raw source data internally and derive filtered/displayed data from it.
- If multiple API calls are needed, manage completion carefully and update loading state only when all calls finish.

### Fragments
- Keep Fragments thin.
- Initialize adapters in `onViewCreated()`.
- Observe ViewModel state in one method like `observeViewModel()`.
- Convert backend models to domain/UI models only if that mapping is presentation-specific.
- Do not call Retrofit services directly from fragments.
- Use helper classes like `NavbarHelper` and `ListViewHelper` if they already exist in the screen.

### Adapters
- Follow the existing adapter style in the project.
- If a screen uses `ListView`, use a custom `ArrayAdapter` with the ViewHolder pattern.
- If a screen uses `RecyclerView`, use the standard `RecyclerView.Adapter` pattern.
- Do not switch ListView to RecyclerView or vice versa unless explicitly requested.
- Keep item binding null-safe and lightweight.

## Current screen pattern example: admin drivers

### ViewModel pattern
Example: `AdminDriversViewModel`
- Holds `allDrivers` as the raw source of truth.
- Holds `driverStatsMap` for extra per-item data.
- Exposes:
  - `getDisplayedDrivers()`
  - `getLoadingLiveData()`
  - `getErrorLiveData()`
  - `getToastMessage()`
- Supports:
  - `loadAllDrivers()`
  - `filterByStatus(String status)`
  - `search(String query)`
  - `createDriver(...)`

### Fragment pattern
Example: `AdminDriversFragment`
- Uses `FragmentAdminDriversBinding`.
- Sets up navbar in `onCreateView()`.
- Creates `AdminDriverAdapter` in `onViewCreated()`.
- Observes displayed data and maps `DriverResponse` objects to `DriverInfoCard`.
- Handles UI-only actions such as filter button styling, search click, and opening dialogs.

### Domain model pattern
Example: `DriverInfoCard`
- This is a presentation model for the adapter.
- It may combine multiple backend models into one UI object.
- It should stay simple and be used for displaying list/card data, not for API calls.

### Adapter pattern
Example: `AdminDriverAdapter`
- Extends `ArrayAdapter<DriverInfoCard>`.
- Uses an inner `ViewHolder` for performance.
- Inflates `R.layout.driver_card_info`.
- Binds avatar, name, email, vehicle info, status, rating, total rides, and earnings.
- Supports `setDrivers(List<DriverInfoCard>)` and refreshes via `notifyDataSetChanged()`.

## Important project rules for Gemini

### Do
- Follow the existing package structure.
- Reuse existing naming conventions.
- Generate small, focused changes.
- Keep code defensive and null-safe.
- Preserve the current UI architecture.
- Use existing helpers, drawables, and binding classes when possible.

### Do not
- Introduce Kotlin, Compose, or Navigation Component unless explicitly requested.
- Rewrite entire files when only a method or class section is needed.
- Move business logic into fragments or adapters.
- Invent new services or data layers if an existing Retrofit service can be extended.
- Change adapter type unless the user asks for it.

## When generating new code

### New ViewModel
- Extend `AndroidViewModel` if application context is needed.
- Add private `MutableLiveData` fields and public `LiveData` getters.
- Use Retrofit callbacks.
- Keep list filtering/search inside the ViewModel.

### New Fragment
- Use ViewBinding.
- Initialize adapter, listeners, and observers separately.
- Keep the fragment as a UI controller only.
- Map models to domain objects only if needed for display.

### New Adapter
- Match the list technology already used by that screen.
- Use ViewHolder pattern.
- Avoid expensive work inside `getView()` or `onBindViewHolder()`.

### New layout
- Use XML layouts.
- Follow existing naming patterns like `fragment_<name>.xml` and `item_<name>.xml`.
- Reuse existing colors, drawables, and Material styles.

## Response style expected from Gemini
- Prefer code that fits into the current codebase without refactoring unrelated parts.
- When asked for a new feature, generate only the necessary classes, methods, and XML.
- If a change affects multiple files, keep the solution minimal and consistent.
- If a detail is ambiguous, follow the patterns already used in the project.
