# AL-Mobile – Agentic Lender borrower app

AL-Mobile is the Android app for retail borrowers in the Agentic Lender project.
Borrowers can create an account, complete a mortgage application, upload supporting
documents and track their application through each stage.

## Features
- Log in, create account, forgot password and profile screens
- 4-step mortgage application (personal & employment, financial, property, loan request)
- Review & submit, with an application reference number
- Document checklist with camera and file/PDF upload
- My applications list and application status timeline
  (Applied → AI deliberated → Underwriter approved → Funded → Closed)

## Tech
Kotlin · Jetpack Compose (Material 3) · Navigation Compose · Coil · Android Gradle Plugin 9.4

## Running
1. Open this folder in Android Studio and let Gradle sync.
2. Select the **app** run configuration and a device, then press Run.
3. Demo login: `demo@agenticlender.ie` / `password`

## Current status
Front-end prototype. Data is kept in memory on the device; it will be connected to the
AL-Core API (applications, documents, status) and the AL-AI Engine (property valuation).
All data access goes through `data/AppRepository.kt`, so only that layer needs to change.

## Project layout
```
DeedTrack/app/src/main/java/com/example/deedtrack/
  MainActivity.kt        navigation between screens
  AppViewModel.kt        app state and actions
  data/                  models, repository, mortgage calculations, document storage
  ui/screens/            all screens
  ui/components/         shared UI components
  ui/theme/              colours and theme
```
