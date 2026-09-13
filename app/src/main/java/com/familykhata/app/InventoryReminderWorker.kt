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
import com.familykhata.app.data.InventoryDatabase
import java.util.Calendar
import java.util.Locale
import java.util.concurrent.TimeUnit

private const val INVENTORY_WORK = "hisabi-khata-inventory-reminders"
private const val INVENTORY_CHANNEL = "inventory_alerts"
private const val SETTINGS_PREFS = "hisabi_khata_v14_settings"
private const val DAY_MS = 86_400_000L

object InventoryReminderScheduler {
    fun schedule(context: Context) {
        val request = PeriodicWorkRequestBuilder<InventoryReminderWorker>(24, TimeUnit.HOURS).build()
        WorkManager.getInstance(context.applicationContext).enqueueUniquePeriodicWork(
            INVENTORY_WORK,
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }
}

class InventoryReminderWorker(appContext: Context, params: WorkerParameters) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        if (Build.VERSION.SDK_INT >= 33 && applicationContext.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            return Result.success()
        }
        createChannel()
        val prefs = applicationContext.getSharedPreferences(SETTINGS_PREFS, Context.MODE_PRIVATE)
        val lowStockEnabled = prefs.getBoolean("inventory_low_stock", true)
        val expiryDays = buildSet {
            if (prefs.getBoolean("expiry_30", true)) add(30)
            if (prefs.getBoolean("expiry_15", false)) add(15)
            if (prefs.getBoolean("expiry_7", true)) add(7)
            if (prefs.getBoolean("expiry_3", true)) add(3)
            if (prefs.getBoolean("expiry_0", true)) add(0)
        }
        val dao = InventoryDatabase.get(applicationContext).dao()
        val products = dao.getAllProducts()
        val batches = dao.getAllBatches().groupBy { it.productId }
        val today = startOfDay(System.currentTimeMillis())

        products.forEach { product ->
            val productBatches = batches[product.id].orEmpty().filter { it.quantity > 0 }
            val totalStock = productBatches.sumOf { it.quantity }
            if (lowStockEnabled && totalStock <= product.lowStockLevel) {
                val title = if (totalStock <= 0) "${product.name} • Out of stock" else "${product.name} • Low stock"
                val text = if (totalStock <= 0) "স্টক শেষ" else "বর্তমান স্টক $totalStock টি"
                notify((product.id % 100000).toInt() + 700000, title, text)
            }
            productBatches.forEach { batch ->
                val expiry = batch.expiryDate ?: return@forEach
                val days = ((startOfDay(expiry) - today) / DAY_MS).toInt()
                if (days < 0) {
                    notify(
                        ((batch.id % 100000).toInt() * 31 + 900000),
                        "${product.name} • Expired",
                        "${batch.quantity} টি পণ্য মেয়াদোত্তীর্ণ"
                    )
                } else if (days in expiryDays) {
                    val whenText = when (days) {
                        0 -> "আজ মেয়াদ শেষ"
                        else -> "$days দিন পর মেয়াদ শেষ"
                    }
                    notify(
                        ((batch.id % 100000).toInt() * 31 + days + 800000),
                        "${product.name} • $whenText",
                        "স্টকে ${batch.quantity} টি • ${formatDate(expiry)}"
                    )
                }
            }
        }
        return Result.success()
    }

    private fun notify(id: Int, title: String, text: String) {
        val notification = NotificationCompat.Builder(applicationContext, INVENTORY_CHANNEL)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
        NotificationManagerCompat.from(applicationContext).notify(id.coerceAtLeast(1), notification)
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            applicationContext.getSystemService(NotificationManager::class.java).createNotificationChannel(
                NotificationChannel(INVENTORY_CHANNEL, "পণ্য ও স্টক নোটিফিকেশন", NotificationManager.IMPORTANCE_DEFAULT).apply {
                    description = "Low stock এবং expiry reminder"
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

    private fun formatDate(value: Long): String = java.text.SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(java.util.Date(value))
}
