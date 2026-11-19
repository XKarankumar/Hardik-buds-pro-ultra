package com.example.ui.navigation

import kotlinx.serialization.Serializable

@Serializable
sealed interface AppRoute {
    @Serializable data object Dashboard : AppRoute
    @Serializable data object AudioStudio : AppRoute
    @Serializable data object SmartControls : AppRoute
    @Serializable data object FindMy : AppRoute
    @Serializable data object Security : AppRoute
    @Serializable data object Settings : AppRoute
    @Serializable data object DeviceCompatibility : AppRoute
    @Serializable data object AiAssistant : AppRoute
}
