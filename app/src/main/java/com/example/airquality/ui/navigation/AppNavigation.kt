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
import com.example.airquality.ui.screens.metrics.AddEditMetricScreen
import com.example.airquality.ui.screens.metrics.MetricsListScreen

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
            // CORREÇÃO: Usar a sintaxe de query parameter para o argumento opcional
            route = "${Screen.AddEditMetric.route}?metricId={metricId}",
            arguments = listOf(navArgument("metricId") {
                type = NavType.StringType
                nullable = true
                defaultValue = null // Definir o valor padrão como nulo
            })
        ) {
            // O ViewModel dentro de AddEditMetricScreen irá lidar com a lógica
            // de buscar a medição se um metricId for passado.
            AddEditMetricScreen(navController = navController)
        }

        // Rota para a Câmera
        composable(route = Screen.Camera.route) {
            CameraScreen(
                onImageCaptured = { uri: Uri ->
                    // Devolve o URI da imagem para a tela anterior (AddEditMetricScreen)
                    // usando o SavedStateHandle para garantir a comunicação segura.
                    navController.previousBackStackEntry
                        ?.savedStateHandle
                        ?.set("captured_image_uri", uri.toString())
                    navController.popBackStack()
                }
            )
        }
    }
}
