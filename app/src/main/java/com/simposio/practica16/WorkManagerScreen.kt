package com.simposio.practica16

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.workDataOf
import kotlinx.coroutines.flow.flowOf
import java.util.UUID
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkManagerScreen() {
    val context = LocalContext.current
    val workManager = remember { WorkManager.getInstance(context) }

    var workId by remember { mutableStateOf<UUID?>(null) }
    var requireInternet by remember { mutableStateOf(false) }
    var requireCharging by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { }
    )

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val status = ContextCompat.checkSelfPermission(
                context, Manifest.permission.POST_NOTIFICATIONS
            )

            if (status != PackageManager.PERMISSION_GRANTED) {
                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    val workInfo by remember(workId) {
        if (workId != null) {
            workManager.getWorkInfoByIdFlow(workId!!)
        } else {
            flowOf(null)
        }
    }.collectAsState(initial = null)

    val textStatus = when (workInfo?.state) {
        WorkInfo.State.ENQUEUED -> "EN ESPERA (EN COLA)"
        WorkInfo.State.RUNNING -> "EN EJECUCION..."
        WorkInfo.State.SUCCEEDED -> "COMPLETADO CON EXITO"
        WorkInfo.State.FAILED -> "FALLO LA TAREA"
        WorkInfo.State.BLOCKED -> "BLOQUEADO"
        WorkInfo.State.CANCELLED -> "CANCELADO"
        null -> "SIN TAREA ACTIVA"
    }

    val progress = if (workInfo?.state == WorkInfo.State.SUCCEEDED) {
        100
    } else {
        workInfo?.progress?.getInt("PROGRESO", 0) ?: 0
    }

    val resultMessage = workInfo?.outputData?.getString("RESULTADO")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("WorkManager - Respaldo")},
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Estado de la tarea: $textStatus",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    LinearProgressIndicator(
                        progress = { progress / 100f },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text(
                        text = "Progreso: $progress%",
                        style = MaterialTheme.typography.bodyMedium
                    )

                    if (resultMessage != null) {
                        Text(
                            text = resultMessage,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Requerir conexion a Internet",
                    style = MaterialTheme.typography.bodyLarge
                )

                Switch(
                    checked = requireInternet,
                    onCheckedChange = { requireInternet = it }
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Solo mientras esta cargando",
                    style = MaterialTheme.typography.bodyLarge
                )

                Switch(
                    checked = requireCharging,
                    onCheckedChange = { requireCharging = it }
                )
            }

            HorizontalDivider()

            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    val restrictions = Constraints.Builder()
                        .setRequiredNetworkType(
                            if (requireInternet) NetworkType.CONNECTED else NetworkType.NOT_REQUIRED
                        )
                        .setRequiresCharging(requireCharging)
                        .build()

                    val inputData = workDataOf("FILE_NAME" to "fotos_practica16.zip")

                    val uniqueRequest = OneTimeWorkRequestBuilder<BackupWorker>()
                        .setConstraints(restrictions)
                        .setInputData(inputData)
                        .addTag("unique_backup")
                        .build()

                    workManager.enqueue(uniqueRequest)
                    workId = uniqueRequest.id
                }
            ) {
                Text("Iniciar Respaldo Unico (OneTime)")
            }

            OutlinedButton(
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    val restrictions = Constraints.Builder()
                        .setRequiredNetworkType(
                            if (requireInternet) NetworkType.CONNECTED else NetworkType.NOT_REQUIRED
                        )
                        .setRequiresCharging(requireCharging)
                        .build()

                    val inputData = workDataOf("FILE_NAME" to "automatic_backup.db")

                    val periodicRequest = PeriodicWorkRequestBuilder<BackupWorker>(
                        15, TimeUnit.MINUTES
                    )
                        .setConstraints(restrictions)
                        .setInputData(inputData)
                        .build()

                    workManager.enqueueUniquePeriodicWork(
                        "periodic_backup_work",
                        ExistingPeriodicWorkPolicy.UPDATE,
                        periodicRequest
                    )

                    workId = periodicRequest.id
                }
            ) {
                Text("Programar Respaldo Periodico (15 min)")
            }

            TextButton(
                onClick = {
                    workManager.cancelAllWork()
                }
            ) {
                Text(
                    text = "Cancelar Todas las Tareas",
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}