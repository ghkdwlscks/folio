# Stock Portfolio Manager

A personal Android app for tracking your stock portfolio with real-time price updates from Yahoo Finance.

## Features

- **Multiple Accounts** - Organize holdings across different accounts (e.g., Retirement, Trading)
- **Multi-Market Support** - Track US stocks (AAPL) and Korean stocks (005930.KS)
- **Real-Time Prices** - Live quotes from Yahoo Finance API
- **Portfolio Analytics** - Sparkline charts, MDD, Volatility, Sharpe Ratio, Best/Worst Day
- **Period Returns** - View portfolio performance over 1W, 1M, 3M, 6M, 1Y
- **Benchmark Comparison** - Compare returns against S&P 500 and KOSPI
- **Interactive Charts** - Full-screen charts with benchmark overlays and touch interactions
- **Sortable Holdings** - Sort by weight, name, symbol, gain/loss %, or day change %
- **Allocation Chart** - Donut chart showing portfolio distribution
- **Currency Toggle** - Switch between USD and KRW display
- **FIRE Calculator** - Plan financial independence with sustainable spending calculations
- **Weight Analysis** - See each holding's percentage of total portfolio
- **Aggregated View** - View all accounts combined or filter by account

## Tech Stack

- Kotlin
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
git clone https://github.com/ghkdwlscks/PortfolioManager.git
cd PortfolioManager

# Build debug APK
./gradlew assembleDebug

# Run tests
./gradlew test
```

The APK will be at `app/build/outputs/apk/debug/app-debug.apk`

## Usage

1. **Add Account** - Go to Settings and create accounts
2. **Add Holding** - Tap the + button to add a stock
3. **Enter Details** - Symbol (e.g., AAPL or 005930.KS), quantity, average price
4. **View Portfolio** - Dashboard shows total value, gain/loss, and period returns
5. **Switch Currency** - Tap USD/KRW toggle in portfolio summary
6. **FIRE Planning** - Access FIRE Calculator from navigation drawer

## License

MIT License
