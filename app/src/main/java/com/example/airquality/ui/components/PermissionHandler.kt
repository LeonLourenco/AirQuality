package com.example.airquality.ui.components

import androidx.compose.runtime.Composable
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState

/**
 * Um Composable reutilizável para lidar com a lógica de permissões no Jetpack Compose.
 * Ele gerencia o pedido de permissões e exibe o conteúdo apropriado com base no resultado.
 *
 * @param permissions A lista de permissões a serem solicitadas (ex: Manifest.permission.CAMERA).
 * @param onPermissionsGranted O conteúdo Composable a ser exibido quando todas as permissões forem concedidas.
 * @param onPermissionsDenied O conteúdo Composable a ser exibido quando uma ou mais permissões forem negadas.
 * Este Composable recebe uma função lambda (`requester`) que pode ser chamada
 * pela UI (ex: um botão) para solicitar as permissões novamente.
 */
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun PermissionHandler(
    permissions: List<String>,
    onPermissionsGranted: @Composable () -> Unit,
    onPermissionsDenied: @Composable (requester: () -> Unit) -> Unit
) {
    // Hook do Accompanist que gerencia o estado das permissões.
    val permissionState = rememberMultiplePermissionsState(permissions)

    // Verifica se todas as permissões já foram concedidas.
    if (permissionState.allPermissionsGranted) {
        // Se sim, exibe o conteúdo principal.
        onPermissionsGranted()
    } else {
        // Se não, exibe a UI de "permissão negada".
        // Passamos uma função lambda que, quando chamada, inicia o pedido de permissão.
        onPermissionsDenied {
            permissionState.launchMultiplePermissionRequest()
        }
    }
}