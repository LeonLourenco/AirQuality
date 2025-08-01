package com.example.airquality.ui.screens.metrics

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import com.example.airquality.ui.navigation.Screen

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
            navController.popBackStack()
        }
        uiState.saveError?.let { error ->
            Toast.makeText(context, "Erro: $error", Toast.LENGTH_LONG).show()
        }
        viewModel.onSaveHandled()
    }

    LaunchedEffect(uiState.locationError) {
        uiState.locationError?.let { error ->
            Toast.makeText(context, error, Toast.LENGTH_LONG).show()
        }
    }

    val savedStateHandle = navController.currentBackStackEntry?.savedStateHandle
    LaunchedEffect(savedStateHandle) {
        savedStateHandle?.get<String>("captured_image_uri")?.let { uriString ->
            viewModel.onImageCaptured(context, uriString.toUri())
            savedStateHandle.remove<String>("captured_image_uri")
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text(uiState.screenTitle) }) // Título dinâmico
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
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = uiState.nomeLocal,
                    onValueChange = viewModel::onNomeLocalChange,
                    label = { Text("Nome do Local *") },
                    modifier = Modifier.fillMaxWidth()
                )

                // Mostra a imagem nova (se capturada) ou a antiga (se existir)
                val imageModel = uiState.fotoByteArray ?: uiState.fotoUrl
                imageModel?.let {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth()) {
                        AsyncImage(
                            model = it,
                            contentDescription = "Foto da medição",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp)
                        )
                    }
                }

                Button(onClick = { navController.navigate(Screen.Camera.route) }, modifier = Modifier.fillMaxWidth()) {
                    Text(if (uiState.fotoByteArray == null && uiState.fotoUrl == null) "Capturar Foto" else "Capturar Nova Foto")
                }

                Text("Dados do Sensor (Opcional)", style = MaterialTheme.typography.titleMedium)

                OutlinedTextField(value = uiState.co2, onValueChange = viewModel::onCo2Change, label = { Text("CO₂ (ppm)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = uiState.tvoc, onValueChange = viewModel::onTvocChange, label = { Text("TVOC (mg/m³)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = uiState.hcho, onValueChange = viewModel::onHchoChange, label = { Text("HCHO (mg/m³)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = uiState.temperatura, onValueChange = viewModel::onTemperaturaChange, label = { Text("Temperatura (°C)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = uiState.umidade, onValueChange = viewModel::onUmidadeChange, label = { Text("Umidade (%)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())

                OutlinedTextField(value = uiState.descricao, onValueChange = viewModel::onDescricaoChange, label = { Text("Descrição (Opcional)") }, modifier = Modifier.fillMaxWidth(), minLines = 3)

                Button(onClick = viewModel::onLocationCaptured, modifier = Modifier.fillMaxWidth(), enabled = !uiState.isFetchingLocation) {
                    if (uiState.isFetchingLocation) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(24.dp))
                    } else {
                        Text(if (uiState.latitude == null) "Capturar Localização *" else "Localização Capturada!")
                    }
                }

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
