# Stock Portfolio Manager

A personal Android app for viewing and tracking your stock portfolio synced from your brokerage.

## Vision

A portfolio viewer that connects to your securities company API to fetch real holdings data. Display portfolio value, performance, and analytics in a clean interface.

## Core Features

### Portfolio Sync
- Fetch current holdings from securities company API
- Auto-refresh or manual sync
- Support for multiple brokerages (future)

### Stock Data
- Current prices from brokerage API or free APIs
- Multi-market support: US, Korea

### Analytics
- Portfolio performance charts
- Total value and gain/loss tracking
- Dividend tracking
- Currency conversion

### History
- Track portfolio value over time
- View past performance

## Tech Stack

- **Language**: Kotlin
- **Min SDK**: 26 (Android 8.0)
- **UI**: Jetpack Compose + Material 3
- **Architecture**: MVVM + Clean Architecture
- **DI**: Hilt
- **Database**: Room (cache holdings, store history)
- **Networking**: Retrofit + OkHttp + Kotlin Serialization
- **Async**: Coroutines + Flow
- **Charts**: Vico or MPAndroidChart
- **Build**: Gradle Kotlin DSL

## Architecture

```
app/src/main/java/com/portfolio/manager/
├── data/
│   ├── local/          # Room DB, DAOs, Entities
│   ├── remote/         # Brokerage API, price APIs
│   └── repository/     # Repository implementations
├── domain/
│   ├── model/          # Domain models
│   ├── repository/     # Repository interfaces
│   └── usecase/        # Business logic
├── presentation/
│   ├── screen/         # Composable screens
│   ├── component/      # Reusable UI components
│   ├── viewmodel/      # ViewModels
│   └── navigation/     # Nav graph
├── di/                 # Hilt modules
└── util/               # Extensions, helpers
```

## Data Sources

### Brokerage API
- Securities company API for holdings data
- API credentials stored securely on device

### Price APIs (Free)
- Alpha Vantage / Yahoo Finance as backup
- Korean market: KRX/Naver Finance

### Local Storage
- Room database for caching and historical data

## Key Screens

1. **Dashboard**: Total value, holdings list, daily change
2. **Stock Detail**: Price chart, holding info, dividends
3. **Settings**: API credentials, currency, sync preferences

## Code Conventions

- Kotlin idiomatic code with null safety
- Stateless Composables with state hoisting
- ViewModels expose `StateFlow<UiState>`
- Sealed classes for UI states and events
- Repository pattern for data access

## Build Environment (WSL2)

### Prerequisites
- **JAVA_HOME**: `/home/jinchan/java/jdk-17.0.10`
- **ANDROID_HOME**: `/home/jinchan/android-sdk`
- **ADB**: Uses Windows ADB server via TCP socket

### ADB Setup (WSL2 to Windows)
1. Start ADB server on Windows (PowerShell): `C:\platform-tools\adb.exe devices`
2. In WSL2, set socket to Windows host:
```bash
export ADB_SERVER_SOCKET=tcp:$(ip route | grep default | awk '{print $3}'):5037
```

### Build Commands

```bash
# Set environment
export JAVA_HOME=/home/jinchan/java/jdk-17.0.10
export ANDROID_HOME=/home/jinchan/android-sdk

# Build
./gradlew assembleDebug      # Build debug APK
./gradlew test               # Run unit tests

# Install to device (requires ADB setup above)
export ADB_SERVER_SOCKET=tcp:$(ip route | grep default | awk '{print $3}'):5037
$ANDROID_HOME/platform-tools/adb install -r app/build/outputs/apk/debug/app-debug.apk
$ANDROID_HOME/platform-tools/adb shell am start -n com.portfolio.manager/.MainActivity

# Other commands
./gradlew connectedAndroidTest  # Run instrumented tests
./gradlew koverHtmlReport    # Generate coverage report
./gradlew koverVerify        # Verify 100% coverage
```

## Testing Strategy (100% Coverage)

### Test Structure

```
app/src/test/java/com/portfolio/manager/     # Unit tests (JVM)
├── data/                                     # Repository, API, DAO tests
├── domain/                                   # Model and UseCase tests
├── presentation/viewmodel/                   # ViewModel tests
└── util/                                     # Utility tests

app/src/androidTest/java/com/portfolio/manager/  # Instrumented tests
├── data/local/                               # Room integration tests
└── presentation/screen/                      # Compose UI tests
```

### Test Dependencies

```kotlin
// build.gradle.kts (app module)
dependencies {
    // Unit Testing
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
    testImplementation("io.mockk:mockk:1.13.8")
    testImplementation("app.cash.turbine:turbine:1.0.0")
    testImplementation("com.google.truth:truth:1.1.5")
    testImplementation("androidx.arch.core:core-testing:2.2.0")
    testImplementation("com.squareup.okhttp3:mockwebserver:4.12.0")
    testImplementation("androidx.room:room-testing:2.6.1")
    testImplementation("com.google.dagger:hilt-android-testing:2.48")
    kaptTest("com.google.dagger:hilt-android-compiler:2.48")

    // Instrumented Testing
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    androidTestImplementation("com.google.dagger:hilt-android-testing:2.48")
    kaptAndroidTest("com.google.dagger:hilt-android-compiler:2.48")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
```

### Coverage Configuration (Kover)

```kotlin
// build.gradle.kts (project root)
plugins {
    id("org.jetbrains.kotlinx.kover") version "0.7.4"
}

// build.gradle.kts (app module)
kover {
    reports {
        verify {
            rule { minBound(100) }
        }
        filters {
            excludes {
                classes("*_Factory", "*_HiltModules*", "*Hilt_*", "*_Impl",
                        "*BuildConfig", "*_MembersInjector", "*.di.*")
                packages("dagger.hilt.*")
            }
        }
    }
}
```

### Testing by Layer

| Layer | Test Type | Tools | What to Test |
|-------|-----------|-------|--------------|
| Domain models | Unit | Truth | Computed properties, validation, edge cases |
| UseCases | Unit | MockK, runTest | Business logic with mocked repositories |
| Repositories | Unit | Fakes | Data flow between local/remote sources |
| APIs | Unit | MockWebServer | Response parsing, error handling |
| DAOs | Instrumented | Room in-memory | CRUD operations, Flow emissions |
| ViewModels | Unit | Turbine, MockK | State transitions, event handling |
| Composables | Instrumented | Compose Test | User interactions, navigation |

### Test Patterns

```kotlin
// Domain model test
@Test
fun `totalValue calculates quantity times price`() {
    val stock = Stock(symbol = "AAPL", quantity = 10, currentPrice = 150.0)
    assertThat(stock.totalValue).isEqualTo(1500.0)
}

// UseCase test with coroutines
@Test
fun `returns portfolio when repository succeeds`() = runTest {
    coEvery { repository.getPortfolio() } returns flowOf(Result.success(portfolio))
    val result = useCase().first()
    assertThat(result.isSuccess).isTrue()
}

// ViewModel test with Turbine
@Test
fun `emits Loading then Success`() = runTest {
    viewModel.uiState.test {
        assertThat(awaitItem()).isEqualTo(UiState.Loading)
        assertThat(awaitItem()).isEqualTo(UiState.Success(data))
    }
}

// MainDispatcherRule (required for ViewModel tests)
class MainDispatcherRule(
    private val dispatcher: TestDispatcher = UnconfinedTestDispatcher()
) : TestWatcher() {
    override fun starting(description: Description) = Dispatchers.setMain(dispatcher)
    override fun finished(description: Description) = Dispatchers.resetMain()
}
```

### Test Naming

Use backticks: `` `subject - scenario - expected result` ``
```kotlin
@Test fun `calculateGain - price exceeds purchase - returns positive`()
@Test fun `sync - network unavailable - returns cached data`()
```

### Coverage Rules

**Must have 100% coverage:**
- Domain models and UseCases
- Repository implementations
- ViewModels
- Utility functions

**Excluded (generated code only):**
- Hilt/Dagger generated classes
- BuildConfig
- DI modules

## Claude Instructions

- Commit after every change without asking
- Always use `git commit -s` (sign-off) for all commits
- Commit message format: `FILENAME: description` (filename without extension, e.g., `README: add project title`)
- Follow TDD (Test-Driven Development): write tests first, then implement code to pass the tests
- Before every commit: run `./gradlew test koverVerify` to ensure all tests pass and coverage is 100%
