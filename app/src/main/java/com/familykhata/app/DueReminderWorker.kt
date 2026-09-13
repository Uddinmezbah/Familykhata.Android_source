package com.familykhata.app

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.familykhata.app.data.AppDatabase
import com.familykhata.app.data.BakiEntryEntity
import java.util.Calendar
import java.util.Locale
import java.util.concurrent.TimeUnit

private const val REMINDER_WORK = "hisabi-khata-due-reminders"
private const val CHANNEL_ID = "due_reminders"
private const val V14_PREFS_WORKER = "hisabi_khata_v14_settings"
private const val DAY_MS_WORKER = 86_400_000L

object DueReminderScheduler {
    fun schedule(context: Context) {
        val request = PeriodicWorkRequestBuilder<DueReminderWorker>(24, TimeUnit.HOURS).build()
        WorkManager.getInstance(context.applicationContext).enqueueUniquePeriodicWork(
            REMINDER_WORK,
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }
}

class DueReminderWorker(appContext: Context, params: WorkerParameters) : CoroutineWorker(appContext, params) {
    private data class Lot(val entry: BakiEntryEntity, var remaining: Double)

    override suspend fun doWork(): Result {
        if (Build.VERSION.SDK_INT >= 33 && applicationContext.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            return Result.success()
        }
        val prefs = applicationContext.getSharedPreferences(V14_PREFS_WORKER, Context.MODE_PRIVATE)
        val enabled = buildSet {
            if (prefs.getBoolean("reminder_30", false)) add(30)
            if (prefs.getBoolean("reminder_15", false)) add(15)
            if (prefs.getBoolean("reminder_7", true)) add(7)
            if (prefs.getBoolean("reminder_3", true)) add(3)
            if (prefs.getBoolean("reminder_0", true)) add(0)
        }
        if (enabled.isEmpty()) return Result.success()

        createChannel()
        val dao = AppDatabase.get(applicationContext).dao()
        val people = dao.getAllPeople()
        val entries = dao.getAllBakiEntries().groupBy { it.personId }
        val today = startOfDay(System.currentTimeMillis())
        val symbol = prefs.getString("currency_symbol", "৳") ?: "৳"

        people.forEach { person ->
            val lots = mutableListOf<Lot>()
            entries[person.id].orEmpty().sortedWith(compareBy<BakiEntryEntity> { it.createdAt }.thenBy { it.id }).forEach { entry ->
                when (entry.action) {
                    "GAVE" -> lots += Lot(entry, entry.amount)
                    "RECEIVED_BACK" -> {
                        var left = entry.amount
                        for (lot in lots) {
                            if (left <= 0.0) break
                            if (lot.remaining <= 0.0) continue
                            val used = minOf(left, lot.remaining)
                            lot.remaining -= used
                            left -= used
                        }
                    }
                }
            }
            lots.filter { it.remaining > 0.0001 && it.entry.dueAt != null }.forEach { lot ->
                val days = ((startOfDay(lot.entry.dueAt!!) - today) / DAY_MS_WORKER).toInt()
                if (days !in enabled) return@forEach
                val whenText = when (days) {
                    0 -> "আজ পরিশোধের তারিখ"
                    3 -> "৩ দিন পর পরিশোধের তারিখ"
                    7 -> "৭ দিন পর পরিশোধের তারিখ"
                    15 -> "১৫ দিন পর পরিশোধের তারিখ"
                    30 -> "৩০ দিন পর পরিশোধের তারিখ"
                    else -> "$days দিন পর পরিশোধের তারিখ"
                }
                val amount = if (lot.remaining % 1.0 == 0.0) lot.remaining.toLong().toString() else String.format(Locale.US, "%.2f", lot.remaining)
                val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
                    .setSmallIcon(android.R.drawable.ic_dialog_info)
                    .setContentTitle("${person.name} • $whenText")
                    .setContentText("পাওনা $symbol $amount")
                    .setStyle(NotificationCompat.BigTextStyle().bigText("${person.name}-এর কাছে পাওনা $symbol $amount। $whenText।"))
                    .setAutoCancel(true)
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                    .build()
                val id = ((lot.entry.id % 100000L).toInt() * 37 + days + 31).coerceAtLeast(1)
                NotificationManagerCompat.from(applicationContext).notify(id, notification)
            }
        }
        return Result.success()
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            applicationContext.getSystemService(NotificationManager::class.java).createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "বাকি পরিশোধের রিমাইন্ডার", NotificationManager.IMPORTANCE_DEFAULT).apply {
                    description = "বাকি/পাওনার নির্ধারিত তারিখের আগে লোকাল রিমাইন্ডার"
                }
            )
        }
    }

    private fun startOfDay(value: Long): Long = Calendar.getInstance().apply {
        timeInMillis = value
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis
}
