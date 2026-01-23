# Stock Portfolio Manager

A personal Android app for manually tracking your stock portfolio with real-time price updates.

## Current Features

### Portfolio Management
- **Multiple Accounts**: Create and manage multiple portfolio accounts (e.g., "Retirement", "Trading")
- **Manual Holdings Entry**: Add stocks with symbol, quantity, average price, and currency
- **Edit/Delete Holdings**: Modify or remove existing holdings with confirmation dialog
- **Duplicate Prevention**: Same stock symbol cannot be added twice in the same account (checked in both add and edit modes)
- **Input Validation**: Quantity (1-1,000,000) and price (0.0001-1,000,000,000) bounds enforced

### Dashboard
- **Portfolio Summary**: Total value, gain/loss with percentage, invested amount
- **Portfolio Sparkline**: Weighted portfolio performance chart with proper aspect ratio
- **Portfolio Statistics**: MDD, Volatility, Sharpe Ratio, Best/Worst Day (period-specific)
- **Period Returns**: Selectable time periods (1W, 1M, 6M, 1Y) showing weighted portfolio returns (default: 1Y)
- **Allocation Pie Chart**: Donut chart showing stock distribution with top 5 + "Others" legend
- **Currency Toggle**: View totals in USD or KRW with animated sliding indicator
- **Holdings List**: Stock cards sorted by weight (largest positions first)
- **Stock Sparklines**: Each card shows price history chart with configurable period
- **Weight Display**: Each stock shows its percentage of total portfolio
- **Account Filter**: View all accounts aggregated or filter by specific account with scroll position preserved
- **Delete Confirmation**: Dialog confirms before deleting any holding
- **Refresh Indicator**: Loading spinner in refresh button during price updates
- **Persisted Settings**: Period selections and currency preferences saved across app restarts

### FIRE Calculator
- **Portfolio Value Display**: Shows total portfolio value with currency toggle (USD/KRW)
- **Settings Configuration**: Adjustable annual return rate and annual inflation rate inputs
- **Real Return Calculation**: Displays real return (annual return - inflation)
- **Sustainable Spending**: Shows monthly and annual sustainable spending based on real return
- **FIRE Target Tracking**: Target monthly spending input with required portfolio calculation
- **Progress Visualization**: Progress bar showing percentage toward FIRE goal with remaining amount
- **Persisted Settings**: Annual return, inflation, and target spending saved across restarts

### Stock Data
- **Real-time Prices**: Fetched from Yahoo Finance API
- **Multi-market Support**: US stocks (AAPL) and Korean stocks (005930.KS)
- **Day Change**: Shows daily price change and percentage
- **Name Resolution**: Uses longName → shortName → symbol fallback
- **Dynamic Exchange Rate**: Live USD/KRW rate from Yahoo Finance API (1-hour cache, fallback to AppConstants)
- **Price History Cache**: Historical prices cached locally with daily refresh
- **Date-based Alignment**: Handles different market trading hours (US vs Korea) using date keys
- **Forward-fill Logic**: Missing dates use last known price; excludes dates before first data point

### UI/UX
- **Material 3 Design**: Modern Android design language with dynamic colors
- **Navigation Drawer**: Menu button in top bar for feature navigation
- **Pull to Refresh**: Manual price refresh with loading indicator
- **Swipe Actions**: Edit and delete holdings
- **Account Reordering**: Up/down buttons to reorder account priority
- **Skeleton Loading**: Shimmer animation placeholders during data loading
- **Card Elevation**: Subtle shadows for visual depth hierarchy
- **Press Animations**: Scale effect on card tap for tactile feedback
- **List Animations**: Smooth item placement animations
- **FAB Scroll Behavior**: Floating action button hides on scroll down, shows on scroll up
- **Crossfade Transitions**: Smooth transitions between loading and content states
- **Animated Period Returns**: Slide and fade transitions when switching periods
- **Error Handling**: Snackbar notifications for operation failures

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
│   │   ├── HoldingDao.kt, HoldingEntity.kt, AccountHoldingCount
│   │   └── PriceHistoryDao.kt, PriceHistoryEntity.kt
│   ├── remote/             # Yahoo Finance API
│   │   ├── YahooFinanceApi.kt
│   │   └── dto/            # Response DTOs
│   └── repository/         # Repository implementations
├── domain/
│   ├── model/              # Stock, StockAccountDetail, PeriodReturn, TimePeriod, PortfolioStats, FIRECalculation
│   ├── service/            # PortfolioStatsCalculator, PriceHistoryProcessor
│   └── repository/         # Repository interfaces
├── presentation/
│   ├── screen/             # DashboardScreen, AddHoldingScreen, AccountsScreen, FIRECalculatorScreen
│   ├── component/          # PortfolioSummary, StockCard, Sparkline, AllocationPieChart, Skeleton
│   ├── viewmodel/          # DashboardViewModel, AddHoldingViewModel, AccountsViewModel, FIRECalculatorViewModel
│   ├── navigation/         # NavGraph
│   ├── theme/              # Color, Theme
│   └── util/               # CurrencyFormatter, CurrencyConverter, TrendIndicator
├── di/                     # Hilt modules (Database, Network, Repository)
└── util/                   # AppConstants, StockExtensions
```

## Key Screens

1. **Dashboard**: Portfolio summary with sparkline and statistics, period selector (1W-1Y), allocation pie chart, holdings list with individual sparklines, account filter
2. **Add/Edit Holding**: Form for symbol, quantity, average price, currency selection with validation
3. **Accounts**: Manage accounts with add, edit, delete, reorder (up/down buttons), and error snackbar
4. **FIRE Calculator**: Portfolio-based FIRE planning with sustainable spending and target tracking

## Data Flow

```
Yahoo Finance API → StockRepository → DashboardViewModel → DashboardScreen
                                    ↘                    ↓
Room Database → HoldingsRepository ──→ FIRECalculatorViewModel → FIRECalculatorScreen
             → AccountRepository
```

## Code Conventions

- Kotlin idiomatic code with null safety
- Stateless Composables with state hoisting
- ViewModels expose `StateFlow<UiState>` (sealed interface pattern)
- Repository pattern for data access
- Use `takeIf`/`takeUnless` for conditional nullability
- Error handling with try-catch in ViewModel operations
- Batch queries to avoid N+1 problems (e.g., `getHoldingsCountByAccountFlow`)
- Room `@Transaction` for atomic operations
- Domain services for complex calculations (PortfolioStatsCalculator, PriceHistoryProcessor)

## AppConstants

```kotlin
object AppConstants {
    const val ALL_ACCOUNTS_ID = -1L
    const val KRW_TO_USD_RATE = 1400.0  // Fallback when API unavailable
    const val MIN_QUANTITY = 1
    const val MAX_QUANTITY = 1_000_000
    const val MIN_PRICE = 0.0001
    const val MAX_PRICE = 1_000_000_000.0
    const val DEFAULT_ACCOUNT_NAME = "Default"
}
```

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
├── domain/
│   ├── model/                # Model tests
│   └── service/              # Service tests (PortfolioStatsCalculator, PriceHistoryProcessor)
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
- Domain services
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
- When user requests additional changes to a previous task, use `git commit --amend` instead of creating a new commit
