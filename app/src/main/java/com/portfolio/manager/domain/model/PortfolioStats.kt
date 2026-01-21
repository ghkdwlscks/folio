package com.portfolio.manager.domain.model

data class PortfolioStats(
    val maxDrawdown: Double = 0.0,      // MDD as percentage
    val volatility: Double = 0.0,        // Daily return std dev %
    val sharpeRatio: Double = 0.0,       // Risk-adjusted return (annualized)
    val bestDay: Double = 0.0,           // Best day return %
    val worstDay: Double = 0.0           // Worst day return %
)
