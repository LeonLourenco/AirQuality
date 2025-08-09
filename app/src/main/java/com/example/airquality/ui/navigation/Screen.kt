package com.example.airquality.ui.navigation

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object Map : Screen("map")
    object Metrics : Screen("metrics")
    object AddEditMetric : Screen("add_edit_metric")
    object Login : Screen("login")
    object Register : Screen("register")
    object Camera : Screen("camera")
    object MapSelection : Screen("map_selection")
}