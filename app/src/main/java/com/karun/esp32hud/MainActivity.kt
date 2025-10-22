package com.karun.esp32hud

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.LocationServices
import android.net.Uri
import android.os.PowerManager
import android.provider.Settings
import android.app.NotificationChannel
import android.app.NotificationManager
import android.widget.Switch
import android.widget.EditText
import android.view.View

class MainActivity : AppCompatActivity() {

    private lateinit var speedText: TextView
    private lateinit var statusText: TextView
    private lateinit var connectButton: Button
    private lateinit var sendSpeedButton: Button
    private lateinit var manualSpeedInput: EditText

    private val REQUEST_PERMISSIONS = 1001
    private var isDebugMode = false

    private lateinit var manualLimitInput: EditText
    private lateinit var sendLimitButton: Button

    private val REQUIRED_PERMISSIONS = mutableListOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION,
        Manifest.permission.BLUETOOTH_CONNECT,
        Manifest.permission.BLUETOOTH_SCAN
    ).apply {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            add(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
        }
    }.toTypedArray()

    // Receiver to listen for updates from HudForegroundService
    private val hudReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val status = intent?.getStringExtra("status")
            val speed = intent?.getFloatExtra("speed", -1f)

            status?.let { statusText.text = "Status: $it" }
            if (speed != null && speed >= 0) {
                speedText.text = "%.1f km/h".format(speed)
            }
        }
    }

    private fun requestNotificationPermissionAndSendTest() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
                // Ask the user for permission
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    2001
                )
            } else {
                // Already granted → send a one-time notification
                sendTestNotification()
            }
        } else {
            sendTestNotification()
        }
    }

    private fun sendTestNotification() {
        val builder = androidx.core.app.NotificationCompat.Builder(this, "HUD_CHANNEL")
            .setSmallIcon(R.drawable.ic_hud) // use your app icon
            .setContentTitle("HUD Ready")
            .setContentText("Notifications are active")
            .setPriority(androidx.core.app.NotificationCompat.PRIORITY_LOW)

        val nm = getSystemService(NotificationManager::class.java)
        nm.notify(1001, builder.build())
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if (!hasAllPermissions()) {
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_PERMISSIONS)
        }

        speedText = findViewById(R.id.speedText)
        manualLimitInput = findViewById(R.id.manualLimitInput)
        sendLimitButton = findViewById(R.id.sendLimitButton)
        statusText = findViewById(R.id.statusText)
        connectButton = findViewById(R.id.connectButton)
        manualSpeedInput = findViewById(R.id.manualSpeedInput)
        sendSpeedButton = findViewById(R.id.sendSpeedButton)

        // Notification channel for foreground service
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "HUD_CHANNEL",
                "HUD",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            channel.description = "ESP32 HUD foreground service"
            val nm = getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(channel)
        }

        // After creating the HUD_CHANNEL
        requestNotificationPermissionAndSendTest()

        // Prompt user to disable battery optimizations
        val pm = getSystemService(PowerManager::class.java)
        val pkg = packageName
        if (!pm.isIgnoringBatteryOptimizations(pkg)) {
            val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
            intent.data = Uri.parse("package:$pkg")
            startActivity(intent)
        }

        val debugSwitch = findViewById<Switch>(R.id.debugModeSwitch)

        debugSwitch.setOnCheckedChangeListener { _, checked ->
            isDebugMode = checked
            val visibility = if (checked) View.VISIBLE else View.GONE
            manualSpeedInput.visibility = visibility
            sendSpeedButton.visibility = visibility
            manualLimitInput.visibility = visibility
            sendLimitButton.visibility = visibility
        }

        connectButton.setOnClickListener {
            val intent = Intent(this, HudForegroundService::class.java).apply {
                action = "START_HUD"
                putExtra("DEBUG_MODE", isDebugMode)
                if (isDebugMode) {
                    val fakeSpeed = manualSpeedInput.text.toString().toFloatOrNull() ?: 0f
                    putExtra("DEBUG_SPEED", fakeSpeed)
                }
            }
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                startForegroundService(intent)
            } else {
                startService(intent)
            }
        }

        sendSpeedButton.setOnClickListener {
            val newSpeed = manualSpeedInput.text.toString().toFloatOrNull() ?: 0f
            val intent = Intent(this, HudForegroundService::class.java).apply {
                action = "UPDATE_DEBUG_SPEED"
                putExtra("DEBUG_SPEED", newSpeed)
            }
            startService(intent)
        }

        sendLimitButton.setOnClickListener {
            val newLimit = manualLimitInput.text.toString().toIntOrNull() ?: -1
            val intent = Intent(this, HudForegroundService::class.java).apply {
                action = "UPDATE_DEBUG_LIMIT"
                putExtra("DEBUG_LIMIT", newLimit)
            }
            startService(intent)
        }

    }

    override fun onResume() {
        super.onResume()
        // Register receiver for HUD updates
        registerReceiver(
            hudReceiver,
            IntentFilter("HUD_UPDATE"),
            RECEIVER_NOT_EXPORTED
        )
    }

    override fun onPause() {
        super.onPause()
        // Unregister receiver to avoid leaks
        unregisterReceiver(hudReceiver)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == REQUEST_PERMISSIONS) {
            val denied = permissions.zip(grantResults.toTypedArray())
                .filter { it.second != PackageManager.PERMISSION_GRANTED }
                .map { it.first }

            if (denied.isEmpty()) {
                statusText.text = "All permissions granted"
            } else {
                statusText.text = "Missing permissions: $denied"
            }
        }
    }

    private fun hasAllPermissions(): Boolean {
        return REQUIRED_PERMISSIONS.all {
            ActivityCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }
    }
}
