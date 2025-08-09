package com.example.airquality.ui.screens.map

import android.Manifest
import android.annotation.SuppressLint
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.rememberCameraPositionState
import java.util.Locale

@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
@SuppressLint("MissingPermission")
@Composable
fun MapSelectionScreen(
    onLocationSelected: (LatLng) -> Unit,
    onCancel: () -> Unit
) {
    val context = LocalContext.current
    val locationPermission = rememberPermissionState(permission = Manifest.permission.ACCESS_FINE_LOCATION)
    val fusedLocationProviderClient = remember { LocationServices.getFusedLocationProviderClient(context) }

    var currentLocation by remember { mutableStateOf<LatLng?>(null) }
    var showConfirmationDialog by remember { mutableStateOf(false) }

    LaunchedEffect(locationPermission.status) {
        if (locationPermission.status.isGranted) {
            fusedLocationProviderClient.lastLocation.addOnSuccessListener { location ->
                location?.let {
                    currentLocation = LatLng(it.latitude, it.longitude)
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        locationPermission.launchPermissionRequest()
    }

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(
            currentLocation ?: LatLng(-14.2350, -51.9253),
            4f
        )
    }

    LaunchedEffect(currentLocation) {
        currentLocation?.let {
            cameraPositionState.position = CameraPosition.fromLatLngZoom(it, 16f)
        }
    }

    // [MODIFICADO] Usando Scaffold para adicionar uma TopAppBar
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Selecione a Localização") },
                navigationIcon = {
                    IconButton(onClick = onCancel) { // Ação de cancelar é chamada aqui
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Voltar"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(Modifier.fillMaxSize().padding(paddingValues)) {
            if (locationPermission.status.isGranted) {
                GoogleMap(
                    modifier = Modifier.fillMaxSize(),
                    cameraPositionState = cameraPositionState,
                    uiSettings = MapUiSettings(zoomControlsEnabled = false)
                )

                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = "Marcador Central",
                    tint = Color.Red,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(48.dp)
                        .padding(bottom = 24.dp)
                )

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.SpaceBetween,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    AnimatedVisibility(
                        visible = !cameraPositionState.isMoving,
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                        ) {
                            Text(
                                text = String.format(
                                    Locale.getDefault(),
                                    "Lat: %.6f, Lon: %.6f",
                                    cameraPositionState.position.target.latitude,
                                    cameraPositionState.position.target.longitude
                                ),
                                modifier = Modifier.padding(16.dp),
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    Button(
                        onClick = { showConfirmationDialog = true },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        enabled = !cameraPositionState.isMoving
                    ) {
                        Text("Marcar este local", fontSize = 16.sp)
                    }
                }

            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp)
                        .background(MaterialTheme.colorScheme.background),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Permissão de Localização Necessária", style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Para selecionar um local no mapa, precisamos da sua permissão para acessar a localização.", textAlign = TextAlign.Center)
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(onClick = { locationPermission.launchPermissionRequest() }) {
                        Text("Conceder Permissão")
                    }
                }
            }

            if (showConfirmationDialog) {
                val selectedLocation = cameraPositionState.position.target
                AlertDialog(
                    onDismissRequest = { showConfirmationDialog = false },
                    title = { Text("Confirmar Localização?") },
                    text = {
                        Text(String.format(Locale.getDefault(), "Deseja confirmar o local em:\n\nLat: %.6f\nLon: %.6f", selectedLocation.latitude, selectedLocation.longitude))
                    },
                    confirmButton = {
                        Button(onClick = {
                            onLocationSelected(selectedLocation)
                            showConfirmationDialog = false
                        }) {
                            Text("Confirmar")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showConfirmationDialog = false }) {
                            Text("Cancelar")
                        }
                    }
                )
            }
        }
    }
}