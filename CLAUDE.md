# Stock Portfolio Manager

A personal Android app for manually tracking your stock portfolio with real-time price updates.

## Current Features

### Portfolio Management
- **Multiple Accounts**: Create and manage multiple portfolio accounts (e.g., "Retirement", "Trading")
- **Manual Holdings Entry**: Add stocks with symbol, quantity, average price, and currency
- **Edit/Delete Holdings**: Modify or remove existing holdings
- **Duplicate Prevention**: Same stock symbol cannot be added twice in the same account

### Dashboard
- **Portfolio Summary**: Total value, invested amount, gain/loss with percentage
- **Period Returns**: Selectable time periods (1D, 1W, 1M, 6M, 1Y) showing weighted portfolio returns
- **Currency Toggle**: View totals in USD or KRW
- **Holdings List**: Stock cards sorted by weight (largest positions first)
- **Weight Display**: Each stock shows its percentage of total portfolio
- **Account Filter**: View all accounts aggregated or filter by specific account

### Stock Data
- **Real-time Prices**: Fetched from Yahoo Finance API
- **Multi-market Support**: US stocks (AAPL) and Korean stocks (005930.KS)
- **Day Change**: Shows daily price change and percentage
- **Name Resolution**: Uses longName → shortName → symbol fallback

### UI/UX
- **Material 3 Design**: Modern Android design language
- **Pull to Refresh**: Manual price refresh
- **Swipe Actions**: Edit and delete holdings
- **Reorderable Accounts**: Drag to reorder account priority

## Tech Stack

- **Language**: Kotlin
- **Min SDK**: 26 (Android 8.0)
- **UI**: Jetpack Compose + Material 3
- **Architecture**: MVVM + Clean Architecture
- **DI**: Hilt
- **Database**: Room
- **Networking**: Retrofit + OkHttp + Kotlin Serialization
- **Async**: Coroutines + Flow
- **Build**: Gradle Kotlin DSL

## Architecture

```
app/src/main/java/com/portfolio/manager/
├── data/
│   ├── local/              # Room DB, DAOs, Entities
│   │   ├── AppDatabase.kt
│   │   ├── AccountDao.kt, AccountEntity.kt
│   │   └── HoldingDao.kt, HoldingEntity.kt
│   ├── remote/             # Yahoo Finance API
│   │   ├── YahooFinanceApi.kt
│   │   └── dto/            # Response DTOs
│   └── repository/         # Repository implementations
├── domain/
│   ├── model/              # Stock, PeriodReturn, TimePeriod
│   └── repository/         # Repository interfaces
├── presentation/
│   ├── screen/             # DashboardScreen, AddHoldingScreen, AccountsScreen
│   ├── component/          # PortfolioSummary, StockCard
│   ├── viewmodel/          # DashboardViewModel, AddHoldingViewModel, AccountsViewModel
│   ├── navigation/         # NavGraph
│   ├── theme/              # Color, Theme
│   └── util/               # CurrencyFormatter
├── di/                     # Hilt modules (Database, Network, Repository)
└── util/                   # AppConstants, StockExtensions
```

## Key Screens

1. **Dashboard**: Portfolio summary, period returns selector, holdings list with account filter
2. **Add/Edit Holding**: Form for symbol, quantity, average price, currency selection
3. **Accounts**: Manage accounts with add, edit, delete, and reorder

## Data Flow

```
Yahoo Finance API → StockRepository → DashboardViewModel → DashboardScreen
                                                        ↓
Room Database → HoldingsRepository ──────────────────────┘
             → AccountRepository
```

## Code Conventions

- Kotlin idiomatic code with null safety
- Stateless Composables with state hoisting
- ViewModels expose `StateFlow<UiState>` (sealed interface pattern)
- Repository pattern for data access
- Use `takeIf`/`takeUnless` for conditional nullability

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
./gradlew test koverVerify   # Run tests and verify 100% coverage

# Install to device (requires ADB setup above)
export ADB_SERVER_SOCKET=tcp:$(ip route | grep default | awk '{print $3}'):5037
$ANDROID_HOME/platform-tools/adb install -r app/build/outputs/apk/debug/app-debug.apk
```

## Testing Strategy (100% Coverage)

### Test Structure

```
app/src/test/java/com/portfolio/manager/
├── data/repository/          # Repository tests
├── domain/model/             # Model tests
├── presentation/
│   ├── viewmodel/            # ViewModel tests
│   └── util/                 # Utility tests
└── util/                     # Extension tests
```

### Test Tools
- **JUnit 4**: Test framework
- **MockK**: Mocking library
- **Truth**: Assertions
- **Coroutines Test**: `runTest`, `UnconfinedTestDispatcher`
- **Kover**: Coverage verification (100% required)

### Test Naming

Use backticks: `` `subject - scenario - expected result` ``
```kotlin
@Test fun `saveHolding - duplicate symbol - returns false`()
@Test fun `selectPeriod - calculates weighted return`()
```

### Coverage Rules

**Must have 100% coverage:**
- Domain models
- Repository implementations
- ViewModels
- Utility functions

**Excluded (Kover config):**
- Hilt/Dagger generated classes
- BuildConfig
- DI modules

## Claude Instructions

- Commit after every change without asking
- Always use `git commit -s` (sign-off) for all commits
- Commit message format: `scope: description` (e.g., `app: add period returns display`)
- Before every commit: run `./gradlew test koverVerify` to ensure all tests pass and coverage is 100%
