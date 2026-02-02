package com.azime.aitestapp.tools

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.util.Log

/**
 * Tool for checking device battery status.
 *
 * ## Usage:
 * 
 * When the user asks about battery (e.g., "What's my battery level?"),
 * this tool is invoked to get real-time battery information.
 *
 * ## Information Provided:
 * - Battery percentage (0-100%)
 * - Charging status (Charging/Discharging/Full/Not Charging)
 * - Charge source (AC/USB/Wireless/None)
 * - Battery health (Good/Overheat/Dead/etc.)
 * - Battery temperature
 */
class BatteryTool(private val context: Context) : Tool {

    companion object {
        private const val TAG = "BatteryTool"
    }

    override val name: String = "battery_status"
    
    override val description: String = "Check device battery level, charging status, and health"
    
    override val triggerKeywords: List<String> = listOf(
        "battery",
        "charge",
        "charging",
        "power",
        "battery level",
        "battery status",
        "battery health",
        "how much battery",
        "battery percentage"
    )

    override suspend fun execute(params: Map<String, String>): ToolResult {
        return try {
            val batteryStatus = getBatteryStatus()
            Log.d(TAG, "Battery status retrieved: $batteryStatus")
            ToolResult.Success(batteryStatus)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get battery status", e)
            ToolResult.Error("Unable to retrieve battery status: ${e.message}")
        }
    }

    /**
     * Get comprehensive battery status information.
     */
    private fun getBatteryStatus(): String {
        val intentFilter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        val batteryStatus: Intent? = context.registerReceiver(null, intentFilter)
        
        if (batteryStatus == null) {
            return "Battery information unavailable"
        }

        // Battery level percentage
        val level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
        val batteryPct = if (level >= 0 && scale > 0) {
            (level * 100 / scale)
        } else {
            -1
        }

        // Charging status
        val status = batteryStatus.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
        val chargingStatus = when (status) {
            BatteryManager.BATTERY_STATUS_CHARGING -> "Charging"
            BatteryManager.BATTERY_STATUS_DISCHARGING -> "Discharging"
            BatteryManager.BATTERY_STATUS_FULL -> "Full"
            BatteryManager.BATTERY_STATUS_NOT_CHARGING -> "Not Charging"
            else -> "Unknown"
        }

        // Charge source
        val plugged = batteryStatus.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1)
        val chargeSource = when (plugged) {
            BatteryManager.BATTERY_PLUGGED_AC -> "AC Adapter"
            BatteryManager.BATTERY_PLUGGED_USB -> "USB"
            BatteryManager.BATTERY_PLUGGED_WIRELESS -> "Wireless"
            else -> "Not Plugged In"
        }

        // Battery health
        val health = batteryStatus.getIntExtra(BatteryManager.EXTRA_HEALTH, -1)
        val healthStatus = when (health) {
            BatteryManager.BATTERY_HEALTH_GOOD -> "Good"
            BatteryManager.BATTERY_HEALTH_OVERHEAT -> "Overheating"
            BatteryManager.BATTERY_HEALTH_DEAD -> "Dead"
            BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE -> "Over Voltage"
            BatteryManager.BATTERY_HEALTH_COLD -> "Cold"
            BatteryManager.BATTERY_HEALTH_UNSPECIFIED_FAILURE -> "Failure"
            else -> "Unknown"
        }

        // Temperature (in tenths of a degree Celsius)
        val temperature = batteryStatus.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -1)
        val tempCelsius = if (temperature > 0) temperature / 10.0 else null

        // Build result string
        return buildString {
            append("📱 Battery Status:\n")
            append("• Level: $batteryPct%\n")
            append("• Status: $chargingStatus\n")
            append("• Power Source: $chargeSource\n")
            append("• Health: $healthStatus")
            if (tempCelsius != null) {
                append("\n• Temperature: ${tempCelsius}°C")
            }
        }
    }
}
