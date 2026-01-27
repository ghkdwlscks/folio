# Stock Portfolio Manager

A personal Android app for manually tracking your stock portfolio and cash savings with real-time price updates.

## Current Features

### Portfolio Management
- **Multiple Accounts**: Create and manage multiple portfolio accounts (e.g., "Retirement", "Trading")
- **Manual Holdings Entry**: Add stocks with symbol, quantity, average price, and currency
- **Cash Savings**: Add cash/savings items with name, value, annual yield rate, and currency
- **Edit/Delete**: Modify or remove holdings and cash items with confirmation dialogs
- **Duplicate Prevention**: Same stock symbol cannot be added twice in the same account
- **Input Validation**: Quantity (1-1,000,000) and price (0.0001-1,000,000,000) bounds enforced
- **Target Weight**: Set target percentage for each holding, preserved on edit

### Dashboard
- **Portfolio Summary**: Total value (large font), gain/loss % (includes cash), invested amount (stocks + cash), day change %
- **Exchange Rate Display**: Live USD/KRW rate shown next to currency toggle
- **Portfolio Sparkline**: Weighted portfolio performance chart with logarithmic scaling (includes cash value)
- **Full-Screen Chart**: Interactive chart dialog with benchmark overlays, period selector, and statistics
- **Benchmark Comparison**: S&P 500 and KOSPI returns displayed alongside portfolio returns
- **Portfolio Statistics**: MDD, Volatility, Sharpe Ratio, Best/Worst Day (daily returns within period)
- **Period Returns**: All time periods (1W, 1M, 3M, 6M, 1Y) show returns simultaneously, loaded in parallel on startup
- **Allocation Pie Chart**: Animated donut chart showing all stocks and cash items individually (no "Others" grouping)
- **Currency Toggle**: View totals in USD or KRW with animated sliding indicator (default: KRW)
- **Holdings List**: Stock cards with customizable sorting (weight, name, symbol, gain/loss %, day change %)
- **Cash List**: Cash cards showing name, value, yield rate with edit/delete buttons
- **Stock Sparklines**: Each card shows price history chart with configurable period
- **Weight Display**: Each stock shows its percentage of total portfolio, with target if set
- **Account Filter**: Dropdown menu to view all accounts aggregated or filter by specific account
- **Multi-Account Details**: Expandable stock cards showing per-account holdings breakdown
- **Annual Income**: Shows combined dividend income from stocks and yield income from cash
- **Delete Confirmation**: Reusable dialogs for holdings and cash items
- **Refresh Indicator**: Loading spinner in refresh button during price updates
- **Persisted Settings**: Period, currency, and sort preferences saved across app restarts
- **Portfolio Caching**: Fast cold start with cached stocks, cash items, accounts, and period returns

### FIRE Calculator
- **Portfolio Value Display**: Shows total portfolio value (stocks + cash) with currency toggle
- **Settings Configuration**: Adjustable annual return rate and annual inflation rate sliders
- **Real Return Calculation**: Displays real return (annual return - inflation)
- **Sustainable Spending**: Shows monthly and annual sustainable spending based on real return
- **FIRE Target Tracking**: Target monthly spending input with required portfolio calculation
- **Progress Visualization**: Progress bar showing percentage toward FIRE goal with remaining amount
- **Persisted Settings**: Annual return, inflation, and target spending saved across restarts

### Stock Data
- **Real-time Prices**: Fetched from Yahoo Finance API (parallel async requests)
- **Multi-market Support**: US stocks (AAPL) and Korean stocks (005930.KS)
- **Korean Stock Auto-Detection**: 6-digit codes auto-append .KS suffix
- **Day Change**: Shows daily price change (displayed even when zero)
- **Name Resolution**: Uses longName → shortName → symbol fallback
- **Dynamic Exchange Rate**: Live USD/KRW rate from Yahoo Finance API (1-hour cache, fallback to AppConstants)
- **Price History Cache**: Historical prices cached locally with daily refresh
- **Date-based Alignment**: Handles different market trading hours (US vs Korea) using date keys
- **Forward-fill Logic**: Missing dates use last known price; excludes dates before first data point
- **Dividend Data**: Trailing annual dividend rate and yield from Yahoo Finance

### UI/UX
- **Material 3 Design**: Modern Android design language with dynamic colors (Android 12+)
- **Glassmorphism**: Semi-transparent glass surfaces with gradient highlights and borders
- **Premium Color Palette**: Rich indigo primary, vibrant teal secondary, warm amber accents
- **Top Bar Actions**: Fire icon (FIRE Calculator), refresh, and settings buttons
- **Pull to Refresh**: Manual price refresh with hidden center indicator
- **Account Reordering**: Up/down buttons to reorder account priority
- **Skeleton Loading**: Shimmer animation placeholders during data loading
- **Card Elevation**: Subtle shadows for visual depth hierarchy
- **Press Animations**: Scale effect (0.98x) on card tap for tactile feedback
- **List Animations**: Smooth item placement animations
- **Chart Animations**: Animated donut segments and legend bars
- **Animated Counters**: Smooth value transitions for currency and percentage displays
- **Horizontal FABs**: Rebalance button (left) and Add button (right) side by side
- **Crossfade Transitions**: Smooth transitions between loading and content states
- **Colored Gain/Loss**: Green for gains, red for losses throughout the UI
- **Heatmap Intensity**: Stock card colors based on gain/loss percentage
- **Error Handling**: Snackbar notifications for operation failures

## Tech Stack

- **Language**: Kotlin 1.9.25
- **Target SDK**: 35 (Android 15)
- **Min SDK**: 26 (Android 8.0)
- **UI**: Jetpack Compose (Compiler 1.5.15, BOM 2024.12.01) + Material 3
- **Architecture**: MVVM + Clean Architecture
- **DI**: Hilt 2.51.1 (with KSP)
- **Database**: Room 2.6.1 (version 1, fallbackToDestructiveMigration)
- **Networking**: Retrofit 2.9.0 + OkHttp 4.12.0 + Kotlin Serialization 1.6.0
- **Navigation**: Navigation Compose 2.8.5
- **Async**: Coroutines + Flow
- **Build**: Gradle Kotlin DSL, Java 17

## Architecture

```
app/src/main/java/com/portfolio/manager/
├── data/
│   ├── local/              # Room DB, DAOs, Entities
│   │   ├── AppDatabase.kt
│   │   ├── AccountDao.kt, AccountEntity.kt
│   │   ├── HoldingDao.kt, HoldingEntity.kt
│   │   ├── CashItemDao.kt, CashItemEntity.kt
│   │   ├── PriceHistoryDao.kt, PriceHistoryEntity.kt
│   │   └── StockNameDao.kt, StockNameEntity.kt
│   ├── remote/             # Yahoo Finance API
│   │   ├── YahooFinanceApi.kt
│   │   └── dto/            # YahooChartResponse, YahooQuoteResponse
│   └── repository/         # Repository implementations
│       ├── AccountRepositoryImpl.kt
│       ├── HoldingsRepositoryImpl.kt
│       ├── CashRepositoryImpl.kt
│       └── StockRepositoryImpl.kt
├── domain/
│   ├── model/              # Domain models
│   │   ├── Stock.kt, CashItem.kt, StockAccountDetail.kt
│   │   ├── PeriodReturn.kt, BenchmarkReturns.kt, TimePeriod.kt
│   │   ├── SortOption.kt, PortfolioStats.kt
│   │   └── FIRECalculation.kt
│   ├── service/            # Domain services
│   │   ├── PortfolioStatsCalculator.kt  # MDD, Sharpe, Volatility calculations
│   │   ├── PriceHistoryProcessor.kt     # Date alignment, forward-fill logic
│   │   ├── PortfolioSorter.kt           # Stock/cash sorting by various criteria
│   │   ├── StockMapper.kt               # Entity to domain model mapping
│   │   └── CacheManager.kt              # JSON-based SharedPreferences caching
│   └── repository/         # Repository interfaces
├── presentation/
│   ├── screen/             # Screen composables
│   │   ├── DashboardScreen.kt
│   │   ├── AddHoldingScreen.kt, AddCashScreen.kt
│   │   ├── AccountsScreen.kt
│   │   └── FIRECalculatorScreen.kt
│   ├── component/          # Reusable UI components
│   │   ├── GlassSurface.kt, GlassCard.kt     # Glassmorphism surfaces
│   │   ├── PortfolioSummary.kt               # Header with total value
│   │   ├── StockCard.kt, CashCard.kt         # List item cards
│   │   ├── Sparkline.kt                      # Mini line charts
│   │   ├── InteractiveChart.kt               # Touch-interactive chart
│   │   ├── FullScreenChart.kt                # Modal chart dialog
│   │   ├── AllocationPieChart.kt             # Animated donut chart
│   │   ├── CurrencyToggle.kt                 # USD/KRW switcher
│   │   ├── AnimatedCounter.kt                # Value transition animations
│   │   ├── AccountDropdown.kt                # Account filter dropdown
│   │   ├── SectionHeader.kt                  # Holdings section with sort options
│   │   ├── ConfirmationDialog.kt             # Reusable confirmation dialogs
│   │   ├── RebalanceDialog.kt                # Portfolio rebalancing UI
│   │   ├── Skeleton.kt                       # Loading placeholders
│   │   └── ErrorContent.kt                   # Error state display
│   ├── viewmodel/          # ViewModels and UI states
│   │   ├── DashboardViewModel.kt, DashboardUiState.kt
│   │   ├── AddHoldingViewModel.kt, AddCashViewModel.kt
│   │   ├── AccountsViewModel.kt
│   │   └── FIRECalculatorViewModel.kt
│   ├── navigation/         # NavGraph, Routes
│   ├── theme/              # Color, Theme, Type
│   └── util/               # Presentation utilities
│       ├── CurrencyFormatter.kt    # Format currency values
│       ├── CurrencyConverter.kt    # USD/KRW conversion
│       ├── TrendIndicator.kt       # Up/down/neutral indicators
│       └── PresentationConstants.kt
├── di/                     # Hilt modules
│   ├── DatabaseModule.kt
│   ├── NetworkModule.kt
│   └── RepositoryModule.kt
└── util/                   # App-wide utilities
    ├── AppConstants.kt          # Global constants
    ├── StockExtensions.kt       # Symbol formatting helpers
    ├── JsonSerializer.kt        # Kotlinx serialization config
    └── SharedPreferencesDelegate.kt  # Property delegates for prefs
```

## Key Screens

1. **Dashboard**: Portfolio summary with sparkline and statistics, benchmark comparison (S&P 500, KOSPI), period selector (1W-1Y), full-screen interactive chart, allocation pie chart, sortable holdings list with individual sparklines, cash items list, account dropdown filter, expandable multi-account details
2. **Add/Edit Holding**: Form for symbol, quantity, average price, currency selection with validation, account chips (add mode), symbol lock (edit mode)
3. **Add/Edit Cash**: Form for name, value, annual yield rate, currency selection (USD/KRW)
4. **Accounts**: Manage accounts with add, edit, delete, reorder (up/down buttons), and error snackbar
5. **FIRE Calculator**: Portfolio-based FIRE planning with sustainable spending and target tracking

## Data Flow

```
Yahoo Finance API → StockRepository → DashboardViewModel → DashboardScreen
                                    ↘                    ↓
Room Database → HoldingsRepository ──→ FIRECalculatorViewModel → FIRECalculatorScreen
             → AccountRepository
             → CashRepository
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
- SharedPreferences delegates for clean preference access
- CacheManager for JSON-based caching with type safety

## AppConstants & PreferenceKeys

```kotlin
object AppConstants {
    const val ALL_ACCOUNTS_ID = -1L
    const val KRW_TO_USD_RATE = 1400.0  // Fallback when API unavailable
    const val MIN_QUANTITY = 1
    const val MAX_QUANTITY = 1_000_000
    const val MIN_PRICE = 0.0001
    const val MAX_PRICE = 1_000_000_000.0
    const val DEFAULT_ACCOUNT_NAME = "Default"
    const val BENCHMARK_SP500 = "^GSPC"
    const val BENCHMARK_KOSPI = "^KS11"
}

object PreferenceKeys {
    // Dashboard preferences
    const val DASHBOARD_SHOW_IN_KRW = "dashboard_show_in_krw"
    const val DASHBOARD_CACHED_STOCKS_JSON = "dashboard_cached_stocks_json"
    const val DASHBOARD_CACHED_CASH_ITEMS_JSON = "dashboard_cached_cash_items_json"
    const val DASHBOARD_CACHED_ACCOUNTS_JSON = "dashboard_cached_accounts_json"
    const val DASHBOARD_CACHED_EXCHANGE_RATE = "dashboard_cached_exchange_rate"
    const val DASHBOARD_CACHED_PORTFOLIO_SPARKLINE = "dashboard_cached_portfolio_sparkline"
    const val DASHBOARD_CACHED_PORTFOLIO_STATS = "dashboard_cached_portfolio_stats"
    const val DASHBOARD_CACHED_PERIOD_RETURNS = "dashboard_cached_period_returns"
    const val STOCK_SPARKLINE_PERIOD = "stock_sparkline_period"
    const val PORTFOLIO_SUMMARY_PERIOD = "portfolio_summary_period"
    const val SORT_OPTION = "sort_option"

    // FIRE calculator preferences
    const val FIRE_ANNUAL_RETURN = "fire_annual_return"
    const val FIRE_ANNUAL_INFLATION = "fire_annual_inflation"
    const val FIRE_TARGET_MONTHLY_SPENDING = "fire_target_monthly_spending"
    const val FIRE_SHOW_IN_KRW = "fire_show_in_krw"
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
├── data/
│   ├── remote/               # YahooFinanceApiTest
│   └── repository/           # Repository tests (Holdings, Account, Cash, Stock)
├── domain/
│   ├── model/                # Model tests (Stock, CashItem, PortfolioStats, FIRECalculation, SortOption, BenchmarkReturns)
│   └── service/              # Service tests (PortfolioStatsCalculator, PriceHistoryProcessor, PortfolioSorter, StockMapper, CacheManager)
├── presentation/
│   ├── viewmodel/            # ViewModel tests (Dashboard, AddHolding, AddCash, Accounts, FIRECalculator)
│   └── util/                 # Utility tests (CurrencyFormatter, CurrencyConverter, TrendIndicator)
└── util/                     # Extension tests (StockExtensions, AppConstants, SharedPreferencesDelegate)
```

### Test Tools
- **JUnit 4**: Test framework
- **MockK**: Mocking library
- **Truth**: Assertions
- **Coroutines Test**: `runTest`, `UnconfinedTestDispatcher`
- **OkHttp MockWebServer**: API testing
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
- Remote API interface

**Excluded (Kover config):**
- Hilt/Dagger generated classes (`*_Factory`, `*_HiltModules*`, `*Hilt_*`, `*_Impl`, `*_MembersInjector`)
- BuildConfig
- DI modules (`*.di.*`)
- UI components (`*.presentation.screen.*`, `*.presentation.component.*`)
- Theme (`*.presentation.theme.*`)
- Navigation (`*.presentation.navigation.*`)
- MainActivity (`*.MainActivity*`)
- Room/Local data (`*.data.local.*`)

## Claude Instructions

- Commit after every change without asking
- Always use `git commit -s` (sign-off) for all commits
- Commit message format: `scope: description` (e.g., `app: add period returns display`)
- Before every commit: run `./gradlew test koverVerify` to ensure all tests pass and coverage is 100%
- When user requests additional changes to a previous task, use `git commit --amend` instead of creating a new commit
