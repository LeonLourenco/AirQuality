package com.example.airquality.ui.screens.map

import android.Manifest
import android.annotation.SuppressLint
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.rememberCameraPositionState
import kotlinx.coroutines.launch
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
    val coroutineScope = rememberCoroutineScope() // Escopo para lançar corrotinas
    val focusManager = LocalFocusManager.current

    var currentLocation by remember { mutableStateOf<LatLng?>(null) }
    var showConfirmationDialog by remember { mutableStateOf(false) }

    var latInput by remember { mutableStateOf("") }
    var lonInput by remember { mutableStateOf("") }

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
        position = CameraPosition.fromLatLngZoom(currentLocation ?: LatLng(-14.2350, -51.9253), 4f)
    }

    LaunchedEffect(currentLocation) {
        currentLocation?.let {
            cameraPositionState.animate(CameraUpdateFactory.newLatLngZoom(it, 16f))
        }
    }

    LaunchedEffect(cameraPositionState.position.target) {
        if (!cameraPositionState.isMoving) {
            val target = cameraPositionState.position.target
            latInput = String.format(Locale.US, "%.6f", target.latitude)
            lonInput = String.format(Locale.US, "%.6f", target.longitude)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Selecione a Localização") },
                navigationIcon = {
                    IconButton(onClick = onCancel) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
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
                    uiSettings = MapUiSettings(zoomControlsEnabled = true, myLocationButtonEnabled = false)
                )
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = "Marcador Central",
                    tint = Color.Red,
                    modifier = Modifier.align(Alignment.Center).size(48.dp).padding(bottom = 24.dp)
                )

                Card(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(16.dp)
                        .fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = latInput,
                            onValueChange = { latInput = it },
                            label = { Text("Latitude") },
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = lonInput,
                            onValueChange = { lonInput = it },
                            label = { Text("Longitude") },
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                            singleLine = true
                        )
                        Button(
                            onClick = {
                                focusManager.clearFocus()
                                val lat = latInput.toDoubleOrNull()
                                val lon = lonInput.toDoubleOrNull()
                                if (lat != null && lon != null && lat in -90.0..90.0 && lon in -180.0..180.0) {
                                    // Lança a corrotina para chamar a função suspend
                                    coroutineScope.launch {
                                        cameraPositionState.animate(CameraUpdateFactory.newLatLng(LatLng(lat, lon)))
                                    }
                                } else {
                                    Toast.makeText(context, "Coordenadas inválidas!", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.height(56.dp)
                        ) {
                            Text("Ir")
                        }
                    }
                }

                Column(
                    modifier = Modifier.fillMaxSize().padding(16.dp),
                    verticalArrangement = Arrangement.Bottom,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Button(
                        onClick = { showConfirmationDialog = true },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        enabled = !cameraPositionState.isMoving
                    ) {
                        Text("Marcar este local", fontSize = 16.sp)
                    }
                }
                FloatingActionButton(
                    onClick = {
                        currentLocation?.let {
                            coroutineScope.launch {
                                cameraPositionState.animate(CameraUpdateFactory.newLatLng(it))
                            }
                        }
                    },
                    modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp)
                ) {
                    Icon(Icons.Default.MyLocation, contentDescription = "Minha Localização")
                }


            } else {
                Column(
                    modifier = Modifier.fillMaxSize().padding(24.dp).background(MaterialTheme.colorScheme.background),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Permissão de Localização Necessária", style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Para usar o mapa, precisamos da sua permissão para acessar a localização.", textAlign = TextAlign.Center)
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