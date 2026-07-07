package dev.monday.domain.context

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.BatteryManager
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.monday.core.model.Connectivity
import dev.monday.core.model.ContextSnapshot
import dev.monday.core.model.TimeOfDay
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ContextEngine @Inject constructor(
    @ApplicationContext private val context: Context
) {
    fun snapshot(): ContextSnapshot = ContextSnapshot(
        currentTime = Instant.now(),
        timeOfDay = classifyTimeOfDay(),
        dayOfWeek = LocalDate.now().dayOfWeek,
        batteryLevel = getBatteryLevel(),
        isCharging = isCharging(),
        connectivity = getConnectivity()
    )

    fun classifyTimeOfDay(): TimeOfDay {
        val hour = LocalTime.now().hour
        return when {
            hour in 5..11 -> TimeOfDay.MORNING
            hour in 12..16 -> TimeOfDay.AFTERNOON
            hour in 17..20 -> TimeOfDay.EVENING
            else -> TimeOfDay.NIGHT
        }
    }

    fun getBatteryLevel(): Int {
        val batteryStatus = context.registerReceiver(
            null,
            IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        )
        val level = batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = batteryStatus?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        return if (level >= 0 && scale > 0) (level * 100 / scale) else -1
    }

    fun isCharging(): Boolean {
        val batteryStatus = context.registerReceiver(
            null,
            IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        )
        val status = batteryStatus?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        return status == BatteryManager.BATTERY_STATUS_CHARGING ||
                status == BatteryManager.BATTERY_STATUS_FULL
    }

    fun getConnectivity(): Connectivity {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = cm.activeNetwork ?: return Connectivity.OFFLINE
        val caps = cm.getNetworkCapabilities(network) ?: return Connectivity.OFFLINE
        return when {
            caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> Connectivity.WIFI
            caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> Connectivity.CELLULAR
            else -> Connectivity.OFFLINE
        }
    }

    /** Build a human-readable context string for AI prompts. */
    fun toPromptContext(): String {
        val snap = snapshot()
        return buildString {
            appendLine("Current Time: ${snap.currentTime}")
            appendLine("Time of Day: ${snap.timeOfDay.name.lowercase()}")
            appendLine("Day: ${snap.dayOfWeek.name.lowercase()}")
            appendLine("Battery: ${snap.batteryLevel}%${if (snap.isCharging) " (charging)" else ""}")
            appendLine("Connectivity: ${snap.connectivity.name.lowercase()}")
        }
    }
}
