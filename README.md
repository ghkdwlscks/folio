# Stock Portfolio Manager

A personal Android app for tracking your stock portfolio and cash savings with real-time price updates from Yahoo Finance.

## Features

### Portfolio Management
- **Multiple Accounts** - Organize holdings across different accounts (e.g., Retirement, Trading)
- **Stock Holdings** - Track stocks with symbol, quantity, average price, and target weight
- **Cash Savings** - Track cash/savings accounts with custom yield rates (e.g., "Emergency Fund" at 4%)
- **Multi-Market Support** - US stocks (AAPL) and Korean stocks (005930.KS)
- **Real-Time Prices** - Live quotes from Yahoo Finance API

### Dashboard
- **Portfolio Summary** - Total value, gain/loss %, invested amount, day change %
- **Period Returns** - All periods (1W, 1M, 3M, 6M, 1Y) visible simultaneously
- **Benchmark Comparison** - Compare returns against S&P 500 and KOSPI
- **Portfolio Analytics** - MDD, Volatility, Sharpe Ratio, Best/Worst Day
- **Interactive Charts** - Full-screen charts with benchmark overlays and touch interactions
- **Allocation Chart** - Animated donut chart showing portfolio distribution (stocks + cash)
- **Sortable Holdings** - Sort by weight, name, symbol, gain/loss %, or day change %
- **Currency Toggle** - Switch between USD and KRW with live exchange rate display
- **Dividend & Income Tracking** - Stock dividends plus cash savings income

### FIRE Calculator
- **Portfolio Value** - Includes both stocks and cash savings
- **Sustainable Spending** - Calculate safe withdrawal based on real return
- **FIRE Target Tracking** - Progress bar toward financial independence goal

### Modern UI
- **Glassmorphism Design** - Semi-transparent glass surfaces with gradient effects
- **Premium Color Palette** - Rich indigo and teal theme with amber accents
- **Smooth Animations** - Animated charts, counters, and transitions
- **Material 3** - Modern Android design language with dynamic colors (Android 12+)
- **Heatmap Cards** - Visual intensity based on gain/loss percentage
- **Skeleton Loading** - Shimmer placeholders during data loading

### Additional Features
- **Rebalancing** - Set target percentages and calculate adjustments needed
- **Aggregated View** - View all accounts combined or filter by account
- **Fast Cold Start** - Cached dashboard for instant display
- **Offline Support** - View cached data when offline

## Tech Stack

- Kotlin 1.9.25
- Jetpack Compose + Material 3
- MVVM + Clean Architecture
- Hilt (Dependency Injection)
- Room (Local Database)
- Retrofit + Kotlin Serialization
- Coroutines + Flow

## Requirements

- Android 8.0 (API 26) or higher
- Internet connection for price updates

## Building

```bash
# Clone the repository
git clone https://github.com/user/portfolio_manager.git
cd portfolio_manager

# Build debug APK
./gradlew assembleDebug

# Run tests
./gradlew test koverVerify
```

The APK will be at `app/build/outputs/apk/debug/app-debug.apk`

## Usage

1. **Add Account** - Go to Accounts screen and create accounts
2. **Add Stock** - Tap + button and select "Add Stock"
3. **Add Cash** - Tap + button and select "Add Cash" for savings accounts
4. **Enter Details** - Symbol (e.g., AAPL or 005930.KS), quantity, average price
5. **View Portfolio** - Dashboard shows total value including stocks and cash
6. **Switch Currency** - Tap USD/KRW toggle in portfolio summary
7. **View Charts** - Tap portfolio sparkline for full-screen interactive chart
8. **FIRE Planning** - Tap fire icon in top bar for FIRE Calculator
9. **Rebalance** - Tap rebalance button to see target vs actual allocation

## Screenshots

The app features a modern glassmorphism design with:
- Glass-effect cards with gradient highlights
- Animated allocation donut chart
- Interactive portfolio charts with benchmark overlays
- Heatmap-colored stock cards based on performance
- Smooth value transitions and animations

## License

MIT License
