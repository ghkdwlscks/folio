# Folio

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
- **Holdings List**: Stock cards showing ticker above name, with customizable sorting (weight, name, symbol, gain/loss %, day change %)
- **Cash List**: Cash cards showing name, value, yield rate with edit/delete buttons
- **Stock Sparklines**: Each card shows price history chart with configurable period
- **Weight Display**: Each stock shows its percentage of total portfolio, with target if set
- **Account Filter**: Dropdown menu to view all accounts aggregated or filter by specific account
- **Multi-Account Filter**: Filter dialog to select which accounts appear in aggregated "All Accounts" view
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
- **Personal vs Group Mode**: When opened from a paired household, runs on the combined portfolio with group FIRE assumptions (return/inflation/target) synced via Firebase; personal mode keeps settings in local preferences

### Household Sharing
- **Pairing Codes**: Create or join a household with an unambiguous XXXX-XXXX code (no 0/O/1/I/L)
- **Combined Portfolio View**: Aggregates my holdings with a partner's read-only snapshot into one dashboard
- **Live Partner Snapshot**: Only the portfolio *definition* (accounts/holdings/cash) is synced via Firestore; each device fetches prices/returns/history from Yahoo Finance locally
- **Auto-Publish**: My snapshot is republished automatically whenever my holdings/cash change, so the partner always sees current values
- **Owner Labels & Read-Only Partner Data**: Partner accounts are namespaced into negative ID space, marked read-only, and labeled by owner in the aggregated view
- **Shared FIRE Settings**: Group FIRE assumptions are stored remotely and editable by both members
- **Anonymous Auth**: Firebase anonymous sign-in identifies each member; no account required

### Settings
- **Language**: English / Korean toggle (in-app, persisted); falls back to the system locale on first launch
- **Theme**: Light / Dark / System selection (in-app, persisted), independent of the OS dark-mode setting

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
- **Localization**: Full English/Korean string sets via `AppStrings` and `LocalAppStrings` composition local
- **In-App Dark Mode**: Light/Dark/System theme honored by the applied color scheme; components branch on `LocalIsDarkTheme` (the resolved in-app state), not `isSystemInDarkTheme()`
- **Heatmap Intensity**: Stock card colors based on gain/loss percentage
- **Error Handling**: Snackbar notifications for operation failures

## Tech Stack

- **Language**: Kotlin 1.9.25
- **App Version**: 1.0.2 (versionCode 3)
- **Target SDK**: 35 (Android 15)
- **Min SDK**: 26 (Android 8.0)
- **UI**: Jetpack Compose (Compiler 1.5.15, BOM 2024.12.01) + Material 3
- **Architecture**: MVVM + Clean Architecture
- **DI**: Hilt 2.51.1 (with KSP)
- **Database**: Room 2.6.1 (version 2, explicit migrations + fallbackToDestructiveMigration safety net — see [Database Migrations](#database-migrations))
- **Networking**: Retrofit 2.9.0 + OkHttp 4.12.0 + Kotlin Serialization 1.6.0
- **Cloud Sync**: Firebase BoM 33.7.0 (Firestore + Anonymous Auth) + kotlinx-coroutines-play-services, for household sharing
- **Navigation**: Navigation Compose 2.8.5
- **Async**: Coroutines + Flow
- **Build**: Gradle Kotlin DSL, Java 17, `com.google.gms.google-services` plugin (requires `app/google-services.json`)

## Architecture

```
app/src/main/java/com/portfolio/manager/
├── MainActivity.kt          # Single activity; hosts theme/localization + NavGraph
├── PortfolioApplication.kt  # @HiltAndroidApp Application
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
│       ├── StockRepositoryImpl.kt
│       ├── SyncRepositoryImpl.kt        # Household sync orchestration (unit-tested)
│       └── FirebaseSyncDataSource.kt    # Firestore/Auth SDK boundary (excluded from coverage)
├── domain/
│   ├── model/              # Domain models
│   │   ├── Stock.kt            # Stock + StockAccountDetail
│   │   ├── CashItem.kt
│   │   ├── PortfolioItem.kt, StockHolding.kt  # Common interface and holding model
│   │   ├── Currency.kt                        # Type-safe USD/KRW enum
│   │   ├── PeriodReturn.kt    # PeriodReturn + BenchmarkReturns + TimePeriod
│   │   ├── SortOption.kt, PortfolioStats.kt
│   │   ├── FIRECalculation.kt # FIRECalculation + FIRETargetCalculation
│   │   ├── PortfolioSnapshot.kt    # Wire-format member snapshot (accounts/holdings/cash)
│   │   ├── GroupFireSettings.kt    # Shared FIRE assumptions for a household
│   │   └── HouseholdMergeResult.kt # Combined my + partner entities (negative-id namespacing)
│   ├── service/            # Domain services
│   │   ├── PortfolioCalculationService.kt # Portfolio values, period returns, cash returns
│   │   ├── PortfolioStatsCalculator.kt  # MDD, Sharpe, Volatility calculations
│   │   ├── PriceHistoryProcessor.kt     # Date alignment, forward-fill orchestration
│   │   ├── DateTimeConverter.kt         # Timestamp/date string conversion utilities
│   │   ├── TimeSeriesProcessor.kt       # Forward-fill and normalization logic
│   │   ├── ExchangeRateAdjuster.kt      # Exchange rate adjustment calculations
│   │   ├── PerAccountCache.kt           # Generic per-account caching utility
│   │   ├── PortfolioSorter.kt           # Stock/cash sorting by various criteria
│   │   ├── StockMapper.kt, CashItemMapper.kt  # Entity to domain model mapping
│   │   ├── CacheManager.kt              # JSON-based SharedPreferences caching
│   │   ├── PortfolioCache.kt            # In-memory cache for Dashboard/FIRE data sharing
│   │   ├── BenchmarkDataService.kt      # Benchmark data loading and calculation
│   │   ├── SparklineService.kt          # Portfolio sparkline computation
│   │   ├── PeriodReturnsService.kt      # Period returns calculation (parallel loading)
│   │   ├── RebalanceCalculator.kt       # Rebalance detection and item calculation
│   │   ├── HouseholdMerger.kt           # Merge my + partner snapshot into combined view
│   │   ├── HouseholdPublisher.kt        # Auto-publish my snapshot on data change
│   │   ├── HouseholdCodeGenerator.kt    # Generate/validate XXXX-XXXX pairing codes
│   │   └── SnapshotMapper.kt            # Local entities → PortfolioSnapshot
│   ├── util/               # Domain utilities
│   │   ├── CurrencyConverter.kt         # USD/KRW conversion
│   │   └── ReturnCalculator.kt          # Return percentage calculations
│   └── repository/         # Repository interfaces (incl. SyncRepository, SyncDataSource)
├── presentation/
│   ├── screen/             # Screen composables
│   │   ├── DashboardScreen.kt           # Also hosts the Settings dialog (language/theme)
│   │   ├── AddHoldingScreen.kt, AddCashScreen.kt
│   │   ├── AccountsScreen.kt
│   │   ├── FIRECalculatorScreen.kt
│   │   ├── HouseholdScreen.kt           # Combined household portfolio view
│   │   └── HouseholdShareScreen.kt      # Pairing UI (create/join household)
│   ├── component/          # Reusable UI components
│   │   ├── GlassSurface.kt                   # GlassSurface + GlassCard glassmorphism
│   │   ├── PortfolioContent.kt               # Shared dashboard/household content body
│   │   ├── PortfolioSummary.kt               # Header with total value
│   │   ├── StockCard.kt, CashCard.kt         # List item cards
│   │   ├── Sparkline.kt                      # Mini line charts
│   │   ├── InteractiveChart.kt               # Touch-interactive chart
│   │   ├── FullScreenChart.kt                # Modal chart dialog
│   │   ├── AllocationPieChart.kt             # Animated donut chart
│   │   ├── CurrencyToggle.kt                 # USD/KRW switcher
│   │   ├── AnimatedCounter.kt                # Value transition animations
│   │   ├── AutoSizeText.kt                   # Text that shrinks to fit
│   │   ├── AccountDropdown.kt                # Account filter dropdown
│   │   ├── AccountFilterDialog.kt            # Multi-account filter selection
│   │   ├── SectionHeader.kt                  # Holdings section with sort options
│   │   ├── BaseDialog.kt                     # Common dialog wrapper with animations
│   │   ├── ConfirmationDialog.kt             # Reusable confirmation dialogs
│   │   ├── RebalanceDialog.kt                # Portfolio rebalancing UI
│   │   ├── Skeleton.kt                       # Loading placeholders
│   │   ├── ErrorContent.kt                   # Error state display
│   │   └── form/                             # Form-specific reusable components
│   │       ├── AccountSelectionSection.kt    # Account chips for add forms
│   │       ├── CurrencySegmentedButton.kt    # USD/KRW segmented button
│   │       └── FormScaffold.kt               # Common form scaffold with back navigation
│   ├── viewmodel/          # ViewModels and UI states
│   │   ├── DashboardViewModel.kt, DashboardUiState.kt
│   │   ├── AddHoldingViewModel.kt, AddCashViewModel.kt
│   │   ├── AccountsViewModel.kt
│   │   ├── FIRECalculatorViewModel.kt        # Personal + group (household) FIRE modes
│   │   ├── HouseholdViewModel.kt             # Combined household portfolio state
│   │   ├── HouseholdShareViewModel.kt        # Pairing/sign-in state
│   │   └── base/                          # Base classes and interfaces
│   │       └── FormUiState.kt             # Common interface for form states
│   ├── navigation/         # NavGraph, Routes
│   ├── theme/              # Color, Type, Animation, Theme (FolioTheme + AppTheme + LocalIsDarkTheme),
│   │                       #   Localization (AppStrings, AppLanguage, LocalAppStrings)
│   └── util/               # Presentation utilities
│       ├── CurrencyFormatter.kt    # Format currency values
│       ├── InputUtils.kt           # Input filtering and formatting
│       ├── TrendIndicator.kt       # Up/down/neutral indicators
│       ├── HapticFeedback.kt       # Haptic feedback helpers
│       └── PresentationConstants.kt
├── di/                     # Hilt modules
│   ├── DatabaseModule.kt   # Provides AppDatabase + DAOs + SharedPreferences (owns migrations)
│   ├── NetworkModule.kt    # Retrofit/OkHttp (adds User-Agent + Accept headers)
│   ├── RepositoryModule.kt
│   └── SyncModule.kt       # Binds SyncRepository/SyncDataSource (Firebase)
└── util/                   # App-wide utilities
    ├── AppConstants.kt          # Global constants + PreferenceKeys
    ├── ErrorMessages.kt         # Centralized error message strings
    ├── StockExtensions.kt       # Symbol formatting helpers
    ├── JsonSerializer.kt        # Kotlinx serialization config
    └── SharedPreferencesDelegate.kt  # Property delegates for prefs
```

## Key Screens

1. **Dashboard**: Portfolio summary with sparkline and statistics, benchmark comparison (S&P 500, KOSPI), period selector (1W-1Y), full-screen interactive chart, allocation pie chart, sortable holdings list with individual sparklines, cash items list, account dropdown filter, expandable multi-account details, rebalance dialog with ideal shares display
2. **Add/Edit Holding**: Form for symbol, quantity, average price, currency selection with validation, account chips (add mode), symbol lock (edit mode)
3. **Add/Edit Cash**: Form for name, value, annual yield rate, currency selection (USD/KRW)
4. **Accounts**: Manage accounts with add, edit, delete, reorder (up/down buttons), and error snackbar
5. **FIRE Calculator**: Portfolio-based FIRE planning with sustainable spending and target tracking (personal or shared household mode)
6. **Household Share**: Create or join a household via pairing code (anonymous Firebase auth)
7. **Household**: Combined view of my portfolio plus the paired partner's read-only snapshot
8. **Settings dialog** (from Dashboard): language (English/Korean) and theme (Light/Dark/System)

## Data Flow

```
Yahoo Finance API → StockRepository → DashboardViewModel → DashboardScreen
                                    ↘         ↓
                                     PortfolioCache (in-memory)
                                              ↓
Room Database → HoldingsRepository ──→ FIRECalculatorViewModel → FIRECalculatorScreen
             → AccountRepository
             → CashRepository

Household sharing:
Room (my entities) → SnapshotMapper → HouseholdPublisher ──┐
                                                           ↓
                              SyncRepository → SyncDataSource → FirebaseSyncDataSource → Firestore
                                                           ↑
HouseholdViewModel ← HouseholdMerger(my entities + partner snapshot) ←┘
  (prices/returns still computed locally from Yahoo Finance per device)
```

## Database Migrations

**⚠️ The Room database is built in `di/DatabaseModule.provideAppDatabase` — that is the ONLY builder the app uses (Hilt-provided).** The `AppDatabase.getDatabase()` companion method is **dead code** (nothing calls it); its `addMigrations(...)` list has no effect at runtime.

When changing the schema (bumping `@Database(version=...)`, adding an entity column, etc.):

1. **Add the `Migration` to `DatabaseModule.provideAppDatabase`'s `addMigrations(...)`** — not to the dead companion.
2. Bumping the version without a matching registered migration triggers `fallbackToDestructiveMigration()`, which **silently wipes all user data** (accounts, holdings, cash). The fallback is a safety net, not a migration strategy.
3. Use additive SQL (`ALTER TABLE … ADD COLUMN …`) and match the entity's column type/nullability exactly so Room's post-migration schema validation passes.
4. `data.local` and `di` are excluded from Kover, so **migrations have no unit-test coverage** — verify on a device/emulator that real data survives.
5. **Back up the on-device DB before installing a schema-changing build:**
   ```bash
   # pull db + WAL + shm
   for f in portfolio_database portfolio_database-wal portfolio_database-shm; do
     adb exec-out run-as com.portfolio.manager cat databases/$f > /tmp/$f
   done
   # consolidate the WAL into the main file (python sqlite3): PRAGMA wal_checkpoint(TRUNCATE)
   # restore: push to /data/local/tmp, chmod 644, then `run-as … cp` into databases/
   ```

Note: `DatabaseModule` historically carries a stale `MIGRATION_10_11` even though `@Database` is on version 2 — ignore the mismatched numbering and follow the steps above.

## Code Conventions

- Kotlin idiomatic code with null safety
- Stateless Composables with state hoisting
- ViewModels expose `StateFlow<UiState>` (sealed interface pattern)
- Repository pattern for data access
- Use `takeIf`/`takeUnless` for conditional nullability
- Error handling with try-catch in ViewModel operations
- Batch queries to avoid N+1 problems (e.g., `getHoldingsCountByAccountFlow`)
- Room `@Transaction` for atomic operations
- Domain services for complex calculations (PortfolioCalculationService, PortfolioStatsCalculator, PriceHistoryProcessor, DateTimeConverter, TimeSeriesProcessor, ExchangeRateAdjuster, PerAccountCache, BenchmarkDataService, SparklineService, PeriodReturnsService, RebalanceCalculator, HouseholdMerger, HouseholdPublisher, HouseholdCodeGenerator, SnapshotMapper)
- SharedPreferences delegates for clean preference access
- CacheManager for JSON-based caching with type safety
- Currency enum for type-safe currency handling (never use "USD"/"KRW" strings in domain/presentation)
- ErrorMessages constants for consistent user-facing error strings
- User-facing strings come from `AppStrings`/`LocalAppStrings` (English + Korean) — never hardcode display text in composables
- Dark-mode branching reads `LocalIsDarkTheme.current` (the applied in-app theme), never `isSystemInDarkTheme()`, so Light/Dark/System selection stays consistent
- Firebase access is isolated behind `SyncDataSource`; `FirebaseSyncDataSource` is the only class touching the SDK (excluded from coverage), so all sync orchestration in `SyncRepositoryImpl` stays unit-testable

### Import Order

Imports must be ordered by group with blank lines between groups, alphabetically sorted within each group:

1. **Android** (`android.*`)
2. **AndroidX** (`androidx.*`)
3. **Third-party** (`dagger.*`, `javax.*`, `com.google.*`, `okhttp3.*`, `retrofit2.*`)
4. **Kotlinx** (`kotlinx.*`)
5. **Project** (`com.portfolio.manager.*`)
6. **Kotlin stdlib** (`kotlin.*`)
7. **Java stdlib** (`java.*`)

Example:
```kotlin
import android.content.SharedPreferences

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope

import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

import com.portfolio.manager.domain.model.Stock
import com.portfolio.manager.domain.repository.StockRepository

import kotlin.math.abs

import java.time.Instant
```

### Class Member Ordering

Members within classes should be ordered as follows:

1. **Companion object** (constants, factory methods)
2. **Injected dependencies** (constructor parameters)
3. **Private state variables** (mutable internal state)
4. **SharedPreferences delegates** (persisted settings)
5. **StateFlow backing fields** (`_uiState`)
6. **Public StateFlow properties** (`uiState`)
7. **Init block**
8. **Public functions**
9. **Private functions**

Example (ViewModel):
```kotlin
@HiltViewModel
class ExampleViewModel @Inject constructor(
    private val repository: ExampleRepository,
    private val sharedPreferences: SharedPreferences
) : ViewModel() {

    companion object {
        private const val TAG = "ExampleViewModel"
    }

    private var cachedData: List<Item> = emptyList()

    private var showInKrw by sharedPreferences.boolean(PreferenceKeys.SHOW_IN_KRW, true)

    private val _uiState = MutableStateFlow<ExampleUiState>(ExampleUiState.Loading)
    val uiState: StateFlow<ExampleUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    fun refresh() { /* ... */ }
    fun updateSetting(value: Boolean) { /* ... */ }

    private fun loadData() { /* ... */ }
    private fun processItems(items: List<Item>) { /* ... */ }
}
```

### Composable Parameter Ordering

Parameters in Composable functions should follow this order:

1. **Required parameters** (no default value)
2. **Optional parameters** (with default values)
3. **Event callbacks** (onClick, onValueChange, etc.)
4. **Modifier** (always last, default `Modifier`)

Example:
```kotlin
@Composable
fun StockCard(
    stock: Stock,                              // Required
    weightPercent: Double? = null,             // Optional
    targetWeight: Int? = null,                 // Optional
    onEdit: (() -> Unit)? = null,              // Callback
    onDelete: (() -> Unit)? = null,            // Callback
    modifier: Modifier = Modifier              // Modifier LAST
) {
```

### Naming Conventions

| Element | Convention | Example |
|---------|------------|---------|
| StateFlow backing field | Underscore prefix | `_uiState` |
| StateFlow public property | No prefix | `uiState` |
| Repository implementation | `*Impl` suffix | `HoldingsRepositoryImpl` |
| Extension functions file | `*Extensions.kt` | `StockExtensions.kt` |
| UI state sealed interface | `*UiState` | `DashboardUiState` |
| Screen composable | `*Screen` | `DashboardScreen` |
| Reusable component | Descriptive name | `StockCard`, `CurrencyToggle` |

### Function Ordering

Within a file, functions should be ordered:

1. **Public/Main functions first** - The primary API or main composable
2. **Private helper functions after** - Supporting implementation details

For Composables:
```kotlin
// Main composable first
@Composable
fun StockCard(stock: Stock, modifier: Modifier = Modifier) {
    // ...
}

// Private helper composables after
@Composable
private fun StockCardHeader(name: String, symbol: String) {
    // ...
}

@Composable
private fun StockCardDetails(price: Double, change: Double) {
    // ...
}
```

For services/utilities:
```kotlin
object PortfolioCalculator {
    // Public API
    fun calculateReturn(values: List<Double>): Double { /* ... */ }
    fun calculateVolatility(returns: List<Double>): Double { /* ... */ }

    // Private helpers
    private fun normalize(values: List<Double>): List<Double> { /* ... */ }
    private fun validateInput(values: List<Double>): Boolean { /* ... */ }
}
```

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
    // App preferences
    const val APP_LANGUAGE = "app_language"   // "en" | "ko"
    const val APP_THEME = "app_theme"         // "light" | "dark" | "system"

    // Dashboard preferences
    const val DASHBOARD_SHOW_IN_KRW = "dashboard_show_in_krw"
    const val DASHBOARD_CACHED_STOCKS_JSON = "dashboard_cached_stocks_json"
    const val DASHBOARD_CACHED_CASH_ITEMS_JSON = "dashboard_cached_cash_items_json"
    const val DASHBOARD_CACHED_ACCOUNTS_JSON = "dashboard_cached_accounts_json"
    const val DASHBOARD_CACHED_EXCHANGE_RATE = "dashboard_cached_exchange_rate"
    const val DASHBOARD_CACHED_PORTFOLIO_SPARKLINE = "dashboard_cached_portfolio_sparkline"
    const val DASHBOARD_CACHED_SPARKLINE_TIMESTAMPS = "dashboard_cached_sparkline_timestamps"
    const val DASHBOARD_CACHED_PORTFOLIO_STATS = "dashboard_cached_portfolio_stats"
    const val DASHBOARD_CACHED_PERIOD_RETURNS = "dashboard_cached_period_returns"
    const val DASHBOARD_CACHED_BENCHMARK_SPARKLINES = "dashboard_cached_benchmark_sparklines"
    const val DASHBOARD_CACHED_BENCHMARK_TIMESTAMPS = "dashboard_cached_benchmark_timestamps"
    const val DASHBOARD_CACHED_BENCHMARK_RETURNS = "dashboard_cached_benchmark_returns"
    const val STOCK_SPARKLINE_PERIOD = "stock_sparkline_period"
    const val PORTFOLIO_SUMMARY_PERIOD = "portfolio_summary_period"
    const val SORT_OPTION = "sort_option"
    const val DASHBOARD_ACCOUNT_FILTER = "dashboard_account_filter"

    // FIRE calculator preferences
    const val FIRE_ANNUAL_RETURN = "fire_annual_return"
    const val FIRE_ANNUAL_INFLATION = "fire_annual_inflation"
    const val FIRE_TARGET_MONTHLY_SPENDING = "fire_target_monthly_spending"
    const val FIRE_TARGET_SPENDING_IN_KRW = "fire_target_spending_in_krw"
    const val FIRE_SHOW_IN_KRW = "fire_show_in_krw"

    // Household sharing preferences
    const val HOUSEHOLD_CODE = "household_code"
    const val HOUSEHOLD_MY_UID = "household_my_uid"
    const val HOUSEHOLD_MY_LABEL = "household_my_label"
    const val HOUSEHOLD_PARTNER_SYMBOLS_JSON = "household_partner_symbols_json"
    const val HOUSEHOLD_SUMMARY_PERIOD = "household_summary_period"
    const val HOUSEHOLD_SPARKLINE_PERIOD = "household_sparkline_period"
    const val HOUSEHOLD_SORT_OPTION = "household_sort_option"
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
$ANDROID_HOME/platform-tools/adb install -r app/build/outputs/apk/debug/folio-debug.apk
```

## Testing Strategy (100% Coverage)

### Test Structure

```
app/src/test/java/com/portfolio/manager/
├── data/
│   ├── remote/               # YahooFinanceApiTest
│   └── repository/           # Repository tests (Holdings, Account, Cash, Stock, Sync)
├── domain/
│   ├── model/                # Model tests (Stock, CashItem, StockHolding, PortfolioStats, FIRECalculation, SortOption, BenchmarkReturns, PortfolioSnapshot, GroupFireSettings)
│   ├── service/              # Service tests (PortfolioCalculationService, PortfolioStatsCalculator, PriceHistoryProcessor, DateTimeConverter, TimeSeriesProcessor, ExchangeRateAdjuster, PerAccountCache, PortfolioSorter, StockMapper, CashItemMapper, CacheManager, PortfolioCache, BenchmarkDataService, SparklineService, PeriodReturnsService, RebalanceCalculator, HouseholdMerger, HouseholdPublisher, HouseholdCodeGenerator, SnapshotMapper)
│   └── util/                 # Domain utility tests (CurrencyConverter, ReturnCalculator)
├── presentation/
│   ├── viewmodel/            # ViewModel tests (Dashboard, AddHolding, AddCash, Accounts, FIRECalculator, Household, HouseholdShare)
│   └── util/                 # Presentation utility tests (CurrencyFormatter, InputUtils, TrendIndicator)
└── util/                     # App utility tests (StockExtensions, AppConstants, SharedPreferencesDelegate, ErrorMessages)
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
- Firebase SDK boundary (`*FirebaseSyncDataSource*`)

## Claude Instructions

- Commit after every change without asking
- Always use `git commit -s` (sign-off) for all commits
- Before every commit: run `./gradlew test koverVerify` to ensure all tests pass and coverage is 100%
- When user requests additional changes to a previous task, use `git commit --amend` instead of creating a new commit

### Commit Message Convention

Format: `scope: description`

Scope rules (test files are excluded when determining scope):
- **Single file changed**: Use the file name without extension (e.g., `TrendIndicator: fix zero value color`)
- **Few files in one package**: Use the package/directory name (e.g., `component: extract reusable UI parts`)
- **Many files across packages**: Use `app:` (e.g., `app: add sparkline charts to stock cards`)
- **Main file introduced/changed**: Use the main file name even if other files are touched (e.g., `PortfolioCache: share data` when introducing PortfolioCache.kt with related changes in other files)
- **Only CLAUDE.md**: Use `CLAUDE:` (e.g., `CLAUDE: document recent improvements`)
- **Only README.md**: Use `README:` (e.g., `README: add project documentation`)
- **Both CLAUDE.md and README.md**: Use `docs:` (e.g., `docs: update CLAUDE.md and README.md with recent changes`)

Note: If changes touch multiple top-level packages (e.g., `data/` + `di/`, or `presentation/` + `util/`), use `app:` unless there's a clear main file being introduced.
