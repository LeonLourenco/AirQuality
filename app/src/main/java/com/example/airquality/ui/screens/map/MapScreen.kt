package com.example.airquality.ui.screens.map

import android.Manifest
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil3.compose.AsyncImage
import com.example.airquality.data.model.Medicao
import com.example.airquality.ui.components.PermissionHandler
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(
    viewModel: MapViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    PermissionHandler(
        permissions = listOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ),
        onPermissionsGranted = {
            MapContent(
                uiState = uiState,
                isMyLocationEnabled = true,
                onMarkerClick = { medicao -> viewModel.selectMedicao(medicao) },
                onDismissBottomSheet = { viewModel.selectMedicao(null) },
                onRefresh = { viewModel.refresh() }
            )
        },
        onPermissionsDenied = { requester ->
            Box(modifier = Modifier.fillMaxSize()) {
                MapContent(
                    uiState = uiState,
                    isMyLocationEnabled = false,
                    onMarkerClick = { medicao -> viewModel.selectMedicao(medicao) },
                    onDismissBottomSheet = { viewModel.selectMedicao(null) },
                    onRefresh = { viewModel.refresh() }
                )
                Button(
                    onClick = requester,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp)
                ) {
                    Text("Ativar Localização")
                }
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MapContent(
    uiState: MapUiState,
    isMyLocationEnabled: Boolean,
    onMarkerClick: (Medicao) -> Unit,
    onDismissBottomSheet: () -> Unit,
    onRefresh: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val defaultLocation = LatLng(-23.5505, -46.6333)
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(defaultLocation, 10f)
    }

    LaunchedEffect(uiState.medicoes) {
        uiState.medicoes.firstOrNull { it.latitude != null && it.longitude != null }?.let {
            val firstLocation = LatLng(it.latitude!!, it.longitude!!)
            cameraPositionState.position = CameraPosition.fromLatLngZoom(firstLocation, 12f)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            properties = MapProperties(isMyLocationEnabled = isMyLocationEnabled)
        ) {
            // --- CORREÇÃO: Filtra apenas medições com localização válida ---
            uiState.medicoes.forEach { medicao ->
                if (medicao.latitude != null && medicao.longitude != null) {
                    Marker(
                        state = MarkerState(position = LatLng(medicao.latitude, medicao.longitude)),
                        title = medicao.nomeLocal,
                        snippet = "CO₂: ${medicao.co2Ppm ?: "N/A"}",
                        onClick = {
                            onMarkerClick(medicao)
                            true
                        }
                    )
                }
            }
            // -------------------------------------------------------------
        }

        FloatingActionButton(
            onClick = onRefresh,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        ) {
            if (uiState.isLoading) {
                CircularProgressIndicator(modifier = Modifier.padding(12.dp).size(24.dp))
            } else {
                Icon(Icons.Default.Refresh, contentDescription = "Atualizar Mapa")
            }
        }

        if (uiState.selectedMedicao != null) {
            ModalBottomSheet(
                onDismissRequest = onDismissBottomSheet,
                sheetState = sheetState
            ) {
                MedicaoDetailSheet(medicao = uiState.selectedMedicao)
            }
        }
    }
}


@Composable
fun MedicaoDetailSheet(medicao: Medicao) {
    val dataHoraFormatada = remember(medicao.momentoMedicao) {
        medicao.momentoMedicao?.let {
            val data = "${it.date.dayOfMonth}/${it.date.monthNumber}/${it.date.year}"
            val hora = "${it.time.hour.toString().padStart(2, '0')}:${it.time.minute.toString().padStart(2, '0')}"
            "$data às $hora"
        } ?: "Data indisponível"
    }

    Column(
        modifier = Modifier
            .padding(horizontal = 16.dp, vertical = 24.dp)
            .fillMaxWidth()
    ) {
        Text(medicao.nomeLocal, style = MaterialTheme.typography.headlineSmall)
        Spacer(modifier = Modifier.height(8.dp))

        medicao.fotoUrl?.let { url ->
            AsyncImage(
                model = url,
                contentDescription = "Foto do local: ${medicao.nomeLocal}",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                contentScale = ContentScale.Crop
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        Text("Data: $dataHoraFormatada", style = MaterialTheme.typography.bodyLarge)

        // --- CORREÇÃO: Exibe localização apenas se disponível ---
        if (medicao.latitude != null && medicao.longitude != null) {
            Text("Localização: Lat ${medicao.latitude}, Lon ${medicao.longitude}", style = MaterialTheme.typography.bodyMedium)
        }

        Spacer(modifier = Modifier.height(16.dp))
        Text("CO₂: ${medicao.co2Ppm ?: "N/A"} ppm", style = MaterialTheme.typography.bodyMedium)
        Text("HCHO: ${medicao.hchoMgM3 ?: "N/A"} mg/m³", style = MaterialTheme.typography.bodyMedium)
        Text("TVOC: ${medicao.tvocMgM3 ?: "N/A"} mg/m³", style = MaterialTheme.typography.bodyMedium)
        Text("Temperatura: ${medicao.temperaturaC ?: "N/A"} °C", style = MaterialTheme.typography.bodyMedium)
        Text("Humidade: ${medicao.umidadePercent ?: "N/A"} %", style = MaterialTheme.typography.bodyMedium)
        medicao.descricao?.let {
            if (it.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text("Descrição: $it", style = MaterialTheme.typography.bodyMedium)
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
    }
}
