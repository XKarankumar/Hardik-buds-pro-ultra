package com.example.domain.model

enum class AppThemeStyle(val label: String, val subtitle: String) {
    LIQUID_GLASS(
        label = "iOS Liquid Glass",
        subtitle = "Translucent specular glass layers & atmospheric glow"
    ),
    BLACK_SILVER_LUXURY(
        label = "Black + Silver Luxury",
        subtitle = "Obsidian chassis with brushed platinum & titanium trim"
    )
}

enum class ThemeMode(val label: String) {
    SYSTEM("System"),
    DARK("Dark"),
    LIGHT("Light")
}

enum class AccentPalette(val label: String, val colorHex: Long) {
    PLATINUM_SILVER("Platinum Silver", 0xFFE2E7F0),
    LIQUID_CYAN("Liquid Cyan", 0xFF4BE2FF),
    TITANIUM_GOLD("Titanium Gold", 0xFFD9B46F),
    EMERALD_PULSE("Emerald Pulse", 0xFF34D399),
    CRIMSON_PRO("Crimson Pro", 0xFFFF5C7A)
}

enum class AppLanguage(val code: String, val displayName: String) {
    ENGLISH("en", "English"),
    HINDI("hi", "हिन्दी (Hindi)"),
    SPANISH("es", "Español (Spanish)")
}
