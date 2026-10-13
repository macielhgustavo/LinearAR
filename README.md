# EnergyAR — Industrial Inspection Lab

A Kotlin Android application for inspecting a simulated compressor in 3D and augmented reality, then recording findings and follow-up actions.

The repository retains the name **LinearAR**. It is an academic simulation, not a live industrial monitoring system.

## Why this exists

A 3D model becomes useful when it supports a complete inspection workflow: explore components, identify a simulated issue, record evidence, and retain the assumptions behind an impact estimate.

## What it does

- Displays normal, leakage, and idle-operation scenarios.
- Explores a compressor through assembled, X-ray, and exploded views.
- Uses numbered component selection and a gated inspection checklist.
- Provides a camera-free 3D lab and an ARCore surface-anchored mode.
- Stores inspections and follow-up actions locally.
- Shares a textual report through Android's native share flow.

## Architecture

Compose screens use `EnergyViewModel` for inspection state. Domain calculations and completion rules live in `data/Inspection.kt`; `InspectionStore` persists history and preferences. SceneView renders the component rig in the 3D/AR lab.

| Component | Location |
| --- | --- |
| Inspection domain | `app/src/main/java/` → `data/Inspection.kt` |
| State and persistence | `EnergyViewModel.kt`, `data/InspectionStore.kt` |
| UI and navigation | `ui/` |
| Model assembly and animation | `ar/CompressorRig.kt` |
| AR anchoring and component projection | `ar/ARScreen.kt` |
| Reproducible model generation | `tools/generate_models.py` |

## Tech stack

Kotlin, Jetpack Compose, Android ViewModel, ARCore, SceneView, Gradle, JUnit and Robolectric.

## Getting started

Open the project in Android Studio with JDK 17 and Android SDK 35. Sync Gradle and run the `app` module. Minimum Android version: 7.0.

AR requires a compatible device and Google Play Services for AR. The 3D lab does not require the camera.

```bash
./gradlew :app:assembleDebug :app:testDebugUnitTest :app:lintDebug
```

The APK is written to `app/build/outputs/apk/debug/app-debug.apk`. GitHub Actions runs the checks and uploads the APK as an artifact.

## Demo journey

Select the leakage scenario, start inspection, explore X-ray/exploded modes, inspect all four components, mark the valve as the issue source, save, and share the report. Simulating a correction restores the normal scenario while preserving the original finding.

## Engineering highlights

- Completion rules prevent saving an unfinished inspection.
- Stored reports preserve the input assumptions used for estimates.
- Domain tests cover costs, scenario references, and completion conditions.
- Camera-free navigation tests use Robolectric; actual AR tracking requires hardware testing.
- Original model assets include a CC0 license.

## Technical decisions

- A camera-free mode makes the domain workflow usable without AR-capable hardware.
- Inspection history is local; there is no remote service dependency.
- Impact estimates use explicit tariff and operating-time inputs instead of claiming measured savings.

## Limitations

The app does not recognize physical machines, read sensors, or detect real leaks. Compressor behavior and corrections are simulated. Cost impact is hypothetical:

`max(0, observed power − reference power) × hours/day × days/month × tariff`.

The compressor is the interactive 3D equipment; other equipment views do not imply equivalent digital-twin functionality. Device performance and AR interactions still need verification on compatible phones.

## Roadmap

- Record a reproducible demo on a real AR-capable device.
- Keep inspection rules independent from rendering.
- Improve accessibility and device compatibility checks.
