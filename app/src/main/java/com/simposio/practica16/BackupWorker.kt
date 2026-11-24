package com.simposio.practica16

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import kotlinx.coroutines.delay


class BackupWorker(
    context: Context,
    workerParameters: WorkerParameters
) : CoroutineWorker(context, workerParameters) {

    override suspend fun doWork(): Result {
        val fileName = inputData.getString("FILE_NAME") ?: "general_backup.dp"
        Log.d("WORK_MANAGER", "Starting backup of: $fileName")

        for (step in 1..5) {
            delay(1000L)
            val percent = step * 20

            setProgress(workDataOf("PROGRESS" to percent))
            Log.d("WORK_MANAGER", "Progress of $fileName: $percent%")
        }

        showFinalNotification(
            title = "Respaldo Completado",
            message = "El archivo $fileName se guardo en segundo plano"
        )

        val outputData = workDataOf(
            "RESULT" to "Backup of '$fileName' 100% finished."
        )

        return Result.success(outputData)
    }

    private fun showFinalNotification( title: String, message: String) {
        val channelId = "workmanager_channel_16"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Tareas en Segundo Plano",
                NotificationManager.IMPORTANCE_HIGH
            )

            val manager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }

        val hasPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                applicationContext,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }

        if (hasPermission) {
            val notification = NotificationCompat.Builder(applicationContext, channelId)
                .setSmallIcon(android.R.drawable.stat_sys_upload_done)
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .build()

            NotificationManagerCompat.from(applicationContext).notify(201, notification)
        }
    }
}