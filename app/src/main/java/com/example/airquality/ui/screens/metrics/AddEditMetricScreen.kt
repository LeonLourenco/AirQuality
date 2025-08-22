package com.example.airquality.ui.screens.metrics

import android.widget.Toast
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toLocalDateTime
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditMetricScreen(
    navController: NavController,
    viewModel: AddEditMetricViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

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

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = uiState.dataString,
                        onValueChange = viewModel::onDataStringChange,
                        readOnly = false,
                        label = { Text("Data *") },
                        placeholder = { Text("dd/MM/aaaa") },
                        isError = uiState.isDataInvalida,
                        trailingIcon = {
                            IconButton(onClick = { showDatePicker = true }) {
                                Icon(Icons.Default.DateRange, contentDescription = "Abrir seletor de data")
                            }
                        },
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = uiState.horaString,
                        onValueChange = viewModel::onHoraStringChange,
                        readOnly = false,
                        label = { Text("Hora *") },
                        placeholder = { Text("HH:mm") },
                        isError = uiState.isHoraInvalida,
                        trailingIcon = {
                            IconButton(onClick = { showTimePicker = true }) {
                                Icon(Icons.Default.Schedule, contentDescription = "Abrir seletor de hora")
                            }
                        },
                        modifier = Modifier.weight(1f)
                    )
                }

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
                    val showImage = uiState.fotoByteArray != null || uiState.fotoUrl != null

                    if (showImage) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth()) {
                            AsyncImage(
                                model = uiState.fotoByteArray ?: uiState.fotoUrl,
                                contentDescription = "Foto da medição",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxWidth().height(200.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    Button(
                        onClick = { navController.navigate(Screen.Camera.route) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
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
                    modifier = Modifier.fillMaxWidth().height(48.dp)
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

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = uiState.dataMedicao?.atStartOfDayIn(TimeZone.UTC)?.toEpochMilliseconds()
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        val data = Instant.fromEpochMilliseconds(millis).toLocalDateTime(TimeZone.UTC).date
                        viewModel.onDataChange(data)
                    }
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancelar") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showTimePicker) {
        val timePickerState = rememberTimePickerState(
            initialHour = uiState.horaMedicao?.hour ?: java.time.LocalTime.now().hour,
            initialMinute = uiState.horaMedicao?.minute ?: java.time.LocalTime.now().minute,
            is24Hour = true
        )
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(28.dp))
        ) {
            Column(
                modifier = Modifier.padding(top = 28.dp, start = 20.dp, end = 20.dp, bottom = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                TimePicker(state = timePickerState)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = { showTimePicker = false }) { Text("Cancelar") }
                    Spacer(modifier = Modifier.size(8.dp))
                    TextButton(onClick = {
                        val selectedTime = LocalTime(timePickerState.hour, timePickerState.minute)
                        viewModel.onHoraChange(selectedTime)
                        showTimePicker = false
                    }) { Text("OK") }
                }
            }
        }
    }
}