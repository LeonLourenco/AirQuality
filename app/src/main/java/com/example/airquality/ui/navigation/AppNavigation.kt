package com.example.airquality.ui.navigation

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.airquality.ui.components.CameraScreen
import com.example.airquality.ui.screens.home.HomeScreen
import com.example.airquality.ui.screens.map.MapScreen
import com.example.airquality.ui.screens.map.MapSelectionScreen
import com.example.airquality.ui.screens.metrics.AddEditMetricScreen
import com.example.airquality.ui.screens.metrics.MetricsListScreen
import com.google.android.gms.maps.model.LatLng

@Composable
fun AppNavigation(navController: NavHostController) {
    NavHost(navController = navController, startDestination = Screen.Home.route) {

        composable(route = Screen.Home.route) {
            HomeScreen()
        }

        composable(route = Screen.Map.route) {
            MapScreen()
        }

        composable(route = Screen.Metrics.route) {
            MetricsListScreen(navController = navController)
        }

        // Rota para Adicionar/Editar com argumento opcional
        composable(
            route = "${Screen.AddEditMetric.route}?metricId={metricId}",
            arguments = listOf(navArgument("metricId") {
                type = NavType.StringType
                nullable = true
                defaultValue = null
            })
        ) {
            AddEditMetricScreen(navController = navController)
        }

        // Rota para a Câmera
        composable(route = Screen.Camera.route) {
            CameraScreen(
                onImageCaptured = { uri: Uri ->
                    // Devolve o URI da imagem para a tela anterior
                    navController.previousBackStackEntry
                        ?.savedStateHandle
                        ?.set("captured_image_uri", uri.toString())
                    navController.popBackStack()
                }
            )
        }

        // Rota para a tela de seleção de mapa
        composable(route = Screen.MapSelection.route) {
            MapSelectionScreen(
                onLocationSelected = { latLng: LatLng ->
                    // Devolve o objeto LatLng para a tela anterior (AddEditMetricScreen)
                    // Usando o SavedStateHandle para comunicação segura entre telas.
                    navController.previousBackStackEntry
                        ?.savedStateHandle
                        ?.set("selected_location", latLng)
                    navController.popBackStack()
                },
                onCancel = {
                    // Simplesmente volta para a tela anterior sem enviar dados
                    navController.popBackStack()
                }
            )
        }
    }
}