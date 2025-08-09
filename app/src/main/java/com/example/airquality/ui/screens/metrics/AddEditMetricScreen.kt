package com.example.airquality.ui.screens.metrics

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import com.example.airquality.ui.navigation.Screen
import com.google.android.gms.maps.model.LatLng
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditMetricScreen(
    navController: NavController,
    viewModel: AddEditMetricViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(uiState.saveSuccess, uiState.saveError) {
        if (uiState.saveSuccess) {
            Toast.makeText(context, "Medição salva com sucesso!", Toast.LENGTH_SHORT).show()
            navController.popBackStack(Screen.Home.route, inclusive = false)
        }
        uiState.saveError?.let { error ->
            Toast.makeText(context, "Erro: $error", Toast.LENGTH_LONG).show()
        }
        viewModel.onSaveHandled()
    }

    LaunchedEffect(navController.currentBackStackEntry?.savedStateHandle) {
        val savedStateHandle = navController.currentBackStackEntry?.savedStateHandle

        savedStateHandle?.get<String>("captured_image_uri")?.let { uriString ->
            viewModel.onImageCaptured(context, uriString.toUri())
            savedStateHandle.remove<String>("captured_image_uri")
        }

        savedStateHandle?.get<LatLng>("selected_location")?.let { latLng ->
            viewModel.onLocationSelected(latLng.latitude, latLng.longitude)
            savedStateHandle.remove<LatLng>("selected_location")
        }
    }


    Scaffold(
        topBar = {
            TopAppBar(title = { Text(uiState.screenTitle) })
        }
    ) { paddingValues ->
        if (uiState.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .padding(paddingValues)
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = uiState.nomeLocal,
                    onValueChange = viewModel::onNomeLocalChange,
                    label = { Text("Nome do Local *") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Column {
                    Button(
                        onClick = { navController.navigate(Screen.MapSelection.route) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(imageVector = Icons.Default.LocationOn, contentDescription = null)
                        Spacer(modifier = Modifier.size(8.dp))
                        Text("Selecionar Local no Mapa *")
                    }

                    if (uiState.latitude != null && uiState.longitude != null) {
                        Row(
                            modifier = Modifier.padding(top = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF008000))
                            Spacer(modifier = Modifier.size(8.dp))
                            Text(
                                text = String.format(Locale.getDefault(), "Lat: %.6f, Lon: %.6f", uiState.latitude, uiState.longitude),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    } else {
                        Text(
                            text = "Nenhum local selecionado.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }

                Column {
                    // Determina qual imagem mostrar (a nova em bytes ou a antiga pela URL)
                    val showImage = uiState.fotoByteArray != null || uiState.fotoUrl != null

                    if (showImage) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth()) {
                            AsyncImage(
                                // A lógica de qual modelo usar é decidida aqui dentro
                                model = uiState.fotoByteArray ?: uiState.fotoUrl,
                                contentDescription = "Foto da medição",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    Button(
                        onClick = { navController.navigate(Screen.Camera.route) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // A condição para o texto do botão agora é explícita e segura
                        Text(if (!showImage) "Capturar Foto" else "Capturar Nova Foto")
                    }
                }

                Text("Dados do Sensor (Opcional)", style = MaterialTheme.typography.titleMedium)

                OutlinedTextField(value = uiState.co2, onValueChange = viewModel::onCo2Change, label = { Text("CO₂ (ppm)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = uiState.tvoc, onValueChange = viewModel::onTvocChange, label = { Text("TVOC (mg/m³)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = uiState.hcho, onValueChange = viewModel::onHchoChange, label = { Text("HCHO (mg/m³)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = uiState.temperatura, onValueChange = viewModel::onTemperaturaChange, label = { Text("Temperatura (°C)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = uiState.umidade, onValueChange = viewModel::onUmidadeChange, label = { Text("Umidade (%)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())

                OutlinedTextField(value = uiState.descricao, onValueChange = viewModel::onDescricaoChange, label = { Text("Descrição (Opcional)") }, modifier = Modifier.fillMaxWidth(), minLines = 3)

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = viewModel::saveOrUpdateMedicao,
                    enabled = uiState.isFormValid && !uiState.isSaving,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    if (uiState.isSaving) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(24.dp))
                    } else {
                        Text("Salvar Medição")
                    }
                }
            }
        }
    }
}