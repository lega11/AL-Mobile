package com.example.deedtrack

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.deedtrack.data.MortgageApplication

/** Shows a phone notification when an application's status changes. Tapping it opens the application. */
object StatusNotifications {

    const val EXTRA_APPLICATION_ID = "open_application_id"
    private const val CHANNEL_ID = "application_status"

    fun createChannel(context: Context) {
        val channel = NotificationChannel(
            CHANNEL_ID, "Application updates", NotificationManager.IMPORTANCE_DEFAULT
        ).apply { description = "Tells you when your mortgage application moves to a new stage." }
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    fun canNotify(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED

    fun showStatusChanged(context: Context, app: MortgageApplication) {
        if (!canNotify(context)) return
        val open = Intent(context, MainActivity::class.java).apply {
            putExtra(EXTRA_APPLICATION_ID, app.id)
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pending = PendingIntent.getActivity(
            context, app.id.hashCode(), open,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Application ${app.status.label.lowercase()}")
            .setContentText("${app.addressLine}: ${app.status.description}")
            .setStyle(NotificationCompat.BigTextStyle().bigText("${app.addressLine} (${app.reference})\n${app.status.description}"))
            .setContentIntent(pending)
            .setAutoCancel(true)
            .build()
        try {
            NotificationManagerCompat.from(context).notify(app.id.hashCode(), notification)
        } catch (_: SecurityException) {
            // Permission was removed while the app was open – nothing to show.
        }
    }
}
