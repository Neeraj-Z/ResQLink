# ResQLink — Disaster Management Platform (Android)

A production-style, offline-first disaster management app built with Kotlin,
Jetpack Compose, and Material Design 3. This repository is a real, buildable
Android Studio project scaffold — not pseudocode — implementing the MVVM +
Clean Architecture layering described below, with working code for the Home
Dashboard, Interactive Map, SOS, First Aid, Hazard Information, Analytics,
and Settings modules, plus the infrastructure (Room, Retrofit, Hilt,
WorkManager) that the future-ready modules will plug into.

## 1. Architecture at a glance

```
presentation/   Compose UI + ViewModels (MVVM). One package per module.
domain/         Pure Kotlin: models, repository interfaces, use cases.
                No Android imports here — keeps business logic testable
                and reusable if you ever add a Wear OS or widget surface.
data/           Room (local, source of truth) + Retrofit (remote) +
                repository implementations that merge the two.
di/             Hilt modules wiring data -> domain -> presentation.
worker/         WorkManager jobs for background sync (SOS flush, hazard refresh).
```

**Offline-first rule, applied consistently:** every write goes to Room
first and returns immediately; every read is served from Room via a
`Flow`; network calls only refresh the cache or best-effort push data,
wrapped in `runCatching` so a dead network never crashes or blocks the UI.
This is why `SosRepositoryImpl.triggerSos()` and `FirstAidRepositoryImpl`
are written the way they are — study those two files first.

## 2. How to open and run this project

1. Install **Android Studio Koala (2024.1)** or newer, with JDK 17.
2. Open the `ResQLink/` folder as an existing project (not `app/`, the root).
3. Get a **Google Maps SDK API key** (Google Cloud Console → enable "Maps
   SDK for Android") and export it before building:
   `export MAPS_API_KEY=your_key_here` (or set it in Android Studio's Run
   Configuration environment variables). It's read in `app/build.gradle.kts`.
4. **Complete the Firebase setup in section 8 below before syncing Gradle** --
   the project will not build without a real `app/google-services.json`
   file, since the `google-services` plugin hard-fails the build otherwise.
5. Sync Gradle, then Run on an emulator/device (minSdk 26).

The backend is Firebase/Firestore (see section 8) -- there is no separate
server to stand up. Once step 4 is done, SOS alerts, hazards, and first-aid
guides all read from and write to your own Firestore project.

## 3. Stepwise plan for your 3rd-year project

Treat this as your semester roadmap. Each phase produces something
demoable, which matters a lot for progress reviews.

### Phase 1 — Foundations (Weeks 1–2) ✅ done in this scaffold
- Get the scaffold building and running on an emulator.
- Fill in `MainActivity`/theme, confirm bottom nav + all 7 screens render.
- Seed the Room database with fake data (`DatabaseSeeder`, runs once in
  `ResQLinkApp.onCreate` under `if (BuildConfig.DEBUG)`) so screens aren't
  empty during development.
- **Deliverable:** app installs, navigates between all modules, shows seed data.

### Phase 2 — Core offline-first flows (Weeks 3–5) ✅ done in this scaffold
- SOS flow wired end-to-end: real GPS (`FusedLocationRepository`), persisted
  to Room immediately, mesh-relayed via `NearbyMeshManager`, synced to
  Firestore via `SosSyncWorker`.
- First Aid search wired end-to-end against Room.
- Hazard Information list + severity color coding wired end-to-end.
- **Deliverable:** SOS works with zero connectivity (airplane mode demo) --
  test this yourself before moving on, it's the app's core value prop.

### Phase 3 — Map + backend integration (Weeks 6–8) — backend ✅, map still open
- ~~Stand up a minimal backend~~ Done: Firestore (see section 8) -- your job
  here is completing the one-time Firebase Console setup, not writing a server.
- Wire `MapScreen` to show hazard-zone circles and SOS markers using
  `maps-compose`; add a `MapViewModel` following the same pattern as
  `HazardViewModel`. **This part is still open** -- `MapScreen` currently
  just renders a bare map with no markers.
- `SosSyncWorker`/`WorkScheduler` are wired and functional against Firestore.
- **Deliverable:** live demo — trigger SOS offline, reconnect, watch it sync.

### Phase 4 — Analytics + Settings polish (Week 9)
- Add real charts to `AnalyticsScreen` (the `vico` chart library is already
  in `build.gradle.kts`) — alerts over time, hazard severity breakdown.
- Finish `SettingsScreen`: theme persists across restarts (DataStore is
  already wired), language switching (see Phase 5).
- **Deliverable:** polished dashboards for your evaluators.

### Phase 5 — Pick 2–3 "future-ready" modules to actually build (Weeks 10–13)
Don't try to build all of these — pick what's realistic and impressive for
a 3rd-year project. Suggested order of effort-to-impact:
1. **Multilingual support** (easiest, high visible payoff): move all UI
   strings into `strings.xml` + `strings-hi.xml`/etc., wire
   `AppCompatDelegate.setApplicationLocales` from `SettingsScreen`.
2. **Offline QR identity**: generate a QR (ZXing, already in gradle) encoding
   the `User` domain model (name, blood group, emergency contact) so it's
   scannable by rescue teams with zero connectivity.
3. **Bluetooth/Wi-Fi Direct mesh SOS relay**: use the Nearby Connections API
   (already in gradle) so that when a device with no internet triggers SOS,
   it broadcasts to nearby ResQLink devices, one of which relays it once it
   regains connectivity. This is the most technically impressive module and
   makes a great capstone demo — start it early since it has the most
   unknowns (needs 2+ physical devices to test).
4. **AI damage assessment from images**: CameraX capture (already wired) +
   call a hosted vision model (or a small on-device TFLite classifier) to
   tag photo severity; store result alongside the SOS alert.
5. **AI emergency assistant**: a simple chat screen calling an LLM API with
   a system prompt scoped to disaster guidance — reuses your Retrofit setup.
6. Family safety tracking / volunteer mode / smart resource allocation are
   larger, more backend-heavy features — good "future work" slide items if
   time runs out, since the domain models (`FamilyMember`, `UserRole`) and
   DB tables already exist to demonstrate the schema is ready for them.

### Phase 6 — Testing, docs, defense prep (Week 14)
- Unit test the `domain/usecase` classes (pure Kotlin, no Android deps —
  fast to test) and repository implementations with fake DAOs.
- Write a short architecture doc (this README is a starting point) and a
  demo script: airplane-mode SOS → reconnect sync → map view → first aid
  search → analytics — this sequence tells a coherent story to evaluators.

## 4. Where to add new features (so the architecture stays clean)

Adding a new module (e.g., "Volunteer Mode") means, in order:
1. `domain/model` — plain data class.
2. `data/local/entity` + DAO + add it to `AppDatabase`.
3. `domain/repository` interface + `data/repository` impl; bind in
   `di/RepositoryModule.kt`.
4. `domain/usecase` if there's real business logic beyond CRUD.
5. `presentation/<module>` — `UiState`, `ViewModel` (`@HiltViewModel`), `Screen` composable.
6. Add a `Screen` entry + `composable(...)` route in `ResQLinkNavGraph.kt`.

Following this order for every module is what keeps a "production-quality"
codebase from turning into spaghetti as you add the future-ready features.

## 5. Mesh relay module (Bluetooth/Wi-Fi Direct offline SOS)

Implemented in `data/mesh/NearbyMeshManager.kt`, backed by Google's Nearby
Connections API. Read that file's class doc first -- it explains the
store-and-forward design in full. Short version:

- Every device simultaneously advertises itself and discovers others
  (`Strategy.P2P_CLUSTER`), so nearby ResQLink phones connect automatically
  with zero pairing UI.
- `TriggerSosUseCase` writes the alert to Room (as always) **and** calls
  `MeshRepository.broadcast()` -- two independent delivery paths racing each
  other, network and mesh, with whichever succeeds first delivering the alert.
- Incoming relayed alerts are deduplicated via `SosDao.exists()` and capped
  at `MeshSosPayload.MAX_HOPS` (6) hops, so the flood can't loop forever.
- Received alerts are stored through the exact same offline-first path as a
  locally-triggered one, so `SosSyncWorker` delivers them to the backend
  automatically the moment *any* device in the relay chain gets connectivity
  -- the sync layer doesn't know or care where an alert originated.
- The SOS screen requests the needed runtime permissions (`MeshPermissions.kt`
  handles the API-level differences), then starts `SosForegroundService` --
  a persistent foreground service, declared with `foregroundServiceType="connectedDevice"`
  -- which owns the actual advertising/discovery lifecycle. This means mesh
  relay keeps running after the person leaves the SOS screen or backgrounds
  the app, matching real disaster behavior rather than only working while a
  screen is open. The ongoing notification has a "Stop relay" action if the
  person wants to end it manually; otherwise it keeps running until the
  process is killed. `MeshViewModel` only *observes* `MeshRepository.relayState`
  for the screen's status pill -- it doesn't own start/stop anymore.

**To test it, you need two physical devices** (the Nearby Connections API
doesn't work in the emulator) with Bluetooth and Wi-Fi on. Open the SOS
screen once on each device (to grant permissions and start the relay
service), then you can back out to Home -- relay keeps running. Trigger SOS
on one device in airplane mode; watch it appear on the second device's
Analytics screen as "mesh relayed" once it propagates, then sync once the
second device's airplane mode is off.

## 6. Google Play's 16 KB page size requirement

Since November 1, 2025, Google Play has required apps targeting Android 15+
(API 35) to support 16 KB memory pages, and the one-time extension expired
May 31, 2026 — this is no longer optional for any update you submit. It
matters because some native (.so) libraries shipped by dependencies were
built assuming 4 KB memory pages; on a 16 KB-page device those libraries
fail to load, and Google Play will flag or reject the build outright.

What's already handled in this project:
- `compileSdk`/`targetSdk` are set to **35** (the requirement only applies
  at 35+, and Play increasingly requires 35 regardless).
- **AGP is 8.6.1** (8.5.1+ is the floor for automatic 16 KB alignment, but
  note `compileSdk = 35` itself separately requires AGP 8.6.0+ -- see
  https://d.android.com/r/tools/api-level-support -- which is why this
  project is on 8.6.1 specifically, not just any 8.5.1+ version).
- `packaging { jniLibs.useLegacyPackaging = false }` is set explicitly in
  `app/build.gradle.kts`, ensuring native libraries are packaged uncompressed
  and page-aligned rather than relying only on the AGP default.
- **CameraX is pinned to 1.4.2** (not 1.3.x) — versions before 1.4.0 shipped
  `libimage_processing_util_jni.so` with 4 KB alignment, which fails the
  check outright. `com.google.mlkit:barcode-scanning:17.3.0` was verified
  compliant; older ML Kit versions (16.x) were not.

**This list can go stale** — dependency compliance is an ongoing thing, not
a one-time fix, since new versions of Play Services (maps, location,
nearby), Room, or anything else with native code can regress. Before every
release:
1. **Build → Analyze APK** in Android Studio, inspect `lib/arm64-v8a/*.so`
   entries for any flagged as non-16KB-aligned.
2. Run a release build through **Play Console's pre-launch report** or App
   Bundle Explorer, which explicitly reports "Supports 16 KB" or flags
   which library is the problem.
3. If something's flagged, it's almost always an outdated dependency
   version — search "`<library name>` 16 KB page size" and update it, the
   same way the CameraX fix above was found.
4. Test on a real 16 KB-page device or the 16 KB system image in the Android
   Emulator (Device Manager → create device → look for a "16 KB" system
   image variant) if you want to catch a regression before Play Console does.

Note also: you'll need **Android SDK Platform 35** installed locally (SDK
Manager → SDK Platforms tab) for the `compileSdk = 35` bump to build at all.

## 8. Backend setup: Firebase / Firestore

The backend is **Firebase (Firestore)**, on the free Spark plan -- no credit
card required, and the free daily quota (50,000 reads / 20,000 writes /
1 GiB storage) is far more than a coursework project needs. There is no
custom server: the Android app talks to Firestore directly using Google's
client SDK (`data/remote/firestore/`), which is a fully supported, standard
way to build a Firebase-backed app without writing any server code.

**⚠️ This project will not build until you complete steps 1–3 below.**
Applying the `google-services` Gradle plugin (already done in
`app/build.gradle.kts`) means the build now requires a real
`google-services.json` config file — without it, Gradle fails immediately
with "File google-services.json is missing from module root folder", not a
runtime error. This is a hard requirement, not optional polish.

1. **Create a Firebase project.** Go to https://console.firebase.google.com,
   click "Add project", name it anything (e.g. "ResQLink"), and you can
   disable Google Analytics for it (not needed here) to keep setup faster.
2. **Register the Android app.** In the new project, click the Android icon
   ("Add app"). For the package name, enter exactly `com.resqlink.app` (must
   match `namespace`/`applicationId` in `app/build.gradle.kts` precisely, or
   the config won't resolve). Nickname and SHA-1 are optional — skip them.
3. **Download `google-services.json`** from that setup flow and place it at
   `app/google-services.json` (i.e., inside the `app/` folder, next to
   `build.gradle.kts` — not the project root). Then sync Gradle; the build
   error from the warning above should be gone.
4. **Create the Firestore database.** In the Firebase Console sidebar:
   Build → Firestore Database → Create database. Choose **Production mode**
   (not test mode — you'll paste real rules in the next step regardless) and
   any region close to you.
5. **Paste the security rules.** Open `firestore.rules` in this repo, copy
   its contents, and paste them into Firebase Console → Firestore Database →
   Rules tab, then click Publish. Read that file's own comments — it
   explains what's intentionally open (SOS creation, needs no login) vs.
   blocked (reading the full alert list), and that it needs tightening once
   this project has real user authentication.
6. **Add some hazard and first-aid data.** The app now reads these from
   Firestore (`HazardFirestoreDataSource`, `FirstAidFirestoreDataSource`)
   rather than the local seeder alone. In Firebase Console → Firestore
   Database → Data, manually add a few documents to a `hazards` collection
   and a `first_aid_guides` collection, matching the field names in
   `HazardEntity`/`FirstAidEntity` (see those files, or just copy the shape
   from `DatabaseSeeder.kt`'s sample data — same fields, just typed into the
   console instead of Kotlin). This is manual and a little tedious by
   design — a real admin panel for this is a good future project extension,
   not something worth building into the app itself for a coursework demo.

**What actually happens now, end to end:** triggering SOS writes to Room
immediately (offline-first, unchanged), then attempts a Firestore write via
`SosFirestoreDataSource`. If that fails (no connectivity), `SosSyncWorker`
retries later, and the mesh relay module can also carry the alert to another
device that might have connectivity — exactly the same offline-first
contract as before, just with a real destination instead of a
`api.resqlink.example.com` placeholder that never existed.

**What this replaced:** earlier versions of this project scaffolded a
Retrofit/OkHttp REST client (`ResQLinkApiService`) against a backend that was
never actually built. That code has been removed entirely now that Firestore
is the real backend, rather than leaving two competing, half-finished
"backend" abstractions in the codebase.

## 9. Known gaps to be transparent about

This scaffold prioritizes a correct, working architecture over 100% feature
completeness — that's intentional so you can build the rest as your actual
project work. Not yet implemented: AI modules, QR generation/scanning UI,
offline map tiles, and real user authentication (Firestore rules currently
allow anyone to create an SOS alert with no login, which is fine for a
demo but is explicitly flagged as needing Firebase Authentication before any
real deployment — see `firestore.rules`). Mesh relay (Bluetooth/Wi-Fi
Direct), real GPS, the database seeder, and the Firestore backend are all
implemented — see sections 5, 6, and 8 above. Each remaining gap is scoped
with exactly where it plugs into the existing layers.
