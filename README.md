# SmartCalc — Android calculator + math camera

SmartCalc is a native Android calculator with a modern dark Material 3 interface, an extended expression engine, and camera/gallery OCR for solving examples from paper.

## What is included

- Android-style Material 3 UI
- DEG/RAD modes
- Arithmetic, powers, percent and factorial
- Constants: `pi`, `tau`, `e`, `phi`
- Trigonometry and inverse trigonometry
- Hyperbolic functions
- `sqrt`, `cbrt`, `abs`, `floor`, `ceil`, `round`, `frac`, `exp`
- `log10(x)` and `log(base,x)`
- `root(n,x)`, `pow(x,y)`, `hypot(x,y)`, `mod(x,y)`
- `gcd`, `lcm`, `ncr`, `npr`, `fib`, `isprime`
- `sigmoid` and other less-common functions
- Simple linear equation solving with `x`, e.g. `2x+3=11`
- Camera OCR and gallery OCR
- OCR cleanup for common math symbols (`× ÷ − √ ² ³`)
- GitHub Actions workflow that builds a debug APK automatically

## Important OCR note

ML Kit is excellent for printed text, but handwritten mathematical notation is inherently harder. Good lighting, focus, a straight page and large characters improve results. The app intentionally shows the recognized expression so it can be corrected before calculating.

## Build without a PC

You can do everything from an Android phone.

### 1. Create a GitHub repository

On github.com in Chrome (or the GitHub mobile app), create a new empty repository, for example:

`smartcalc-android`

### 2. Upload this project

Unzip this archive on your phone.

Upload the **contents** of the `SmartCalcAndroid` folder to the repository so that these files are at the repository root:

- `settings.gradle.kts`
- `build.gradle.kts`
- `app/...`
- `.github/workflows/build-apk.yml`

### 3. Let GitHub build the APK

Open your repository on GitHub:

**Actions → Build Android APK → Run workflow**

After the workflow finishes:

**Actions → Build Android APK → the latest run → Artifacts → SmartCalc-debug-apk**

Download the artifact ZIP, extract it, and install `app-debug.apk`.

### 4. GitHub Pages is not required

GitHub Actions is the part that compiles the Android project. GitHub Pages does not build Android APKs.

## Local build command

If you later use an Android development environment:

```bash
gradle assembleDebug
```

The APK is produced at:

`app/build/outputs/apk/debug/app-debug.apk`

## Ideas for version 2

- handwritten math recognition specialized for equations
- graphing
- matrix operations
- unit conversion
- history with reusable expressions
- expression sharing
- custom themes
- symbolic algebra
- equation systems
