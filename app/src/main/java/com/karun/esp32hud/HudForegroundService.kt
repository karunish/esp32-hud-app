package com.karun.esp32hud

import android.app.Service
import android.content.Intent
import android.os.IBinder
import com.google.android.gms.location.FusedLocationProviderClient

import android.Manifest
import android.os.Looper
import android.util.Log

import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket

import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority

import android.location.Location
import android.content.pm.PackageManager
import androidx.annotation.RequiresPermission

import java.io.OutputStream
import java.util.UUID

class HudForegroundService : Service() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var bluetoothSocket: BluetoothSocket? = null
    private var outputStream: OutputStream? = null
    private val uuid: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB") // classic SPP UUID
    private val deviceName = "ESP32_HUD" // adjust to your paired device name
    private val handler = android.os.Handler(Looper.getMainLooper())
    private var locationCallback: LocationCallback? = null

    // Add this Runnable implementation
    private val debugRunnable = object : Runnable {
        override fun run() {
            if (debugMode) {
                // This is where you would put the code that runs periodically in debug mode.
                // For instance, you could send the speed and broadcast an update.
                sendSpeedToESP32(debugSpeed)
                broadcastUpdate("Debug", debugSpeed)

                // Schedule the next run after 1 second
                handler.postDelayed(this, 1000)
            }
        }
    }

    private var debugMode: Boolean = false
    private var debugSpeed: Float = 0f

    override fun onCreate() {
        super.onCreate()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            "START_HUD" -> {
                debugMode = intent.getBooleanExtra("DEBUG_MODE", false)
                debugSpeed = intent.getFloatExtra("DEBUG_SPEED", 0f)
                startHud()
            }
            "UPDATE_DEBUG_SPEED" -> {
                // Update the fake speed while service is running
                debugSpeed = intent.getFloatExtra("DEBUG_SPEED", debugSpeed)
                Log.i("HUD", "Updated debug speed to $debugSpeed")
                // Immediately send once so UI/ESP32 reflect the change without waiting
                sendSpeedToESP32(debugSpeed)
                broadcastUpdate("Debug", debugSpeed)
            }
            "STOP_HUD" -> stopSelf()
        }
        return START_STICKY
    }

    //>:)

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    private fun startHud() {
        val notification = NotificationCompat.Builder(this, "HUD_CHANNEL")
            .setContentTitle("HUD Running")
            .setContentText(if (debugMode) "Debug mode active" else "Sending speed to ESP32")
            .setSmallIcon(R.drawable.ic_hud)
            .build()

        startForeground(1, notification)

        connectToESP32()

        if (debugMode) {
            Log.i("HUD", "Debug mode active, sending fake speed=$debugSpeed")
            handler.post(debugRunnable)
            broadcastUpdate("Debug", debugSpeed)
        } else {
            startLocationUpdates()
        }
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    private fun connectToESP32() {
        val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled) {
            Log.e("HUD", "Bluetooth not available or disabled")
            broadcastUpdate("Disconnected")
            return
        }

        val pairedDevices: Set<BluetoothDevice> = bluetoothAdapter.bondedDevices
        val device = pairedDevices.find { it.name == deviceName }

        if (device == null) {
            Log.e("HUD", "ESP32 not paired")
            broadcastUpdate("Disconnected")
            return
        }

        try {
            bluetoothSocket = device.createRfcommSocketToServiceRecord(uuid)
            bluetoothSocket?.connect()
            outputStream = bluetoothSocket?.outputStream
            Log.i("HUD", "Connected to ESP32")
            broadcastUpdate("Connected")
        } catch (e: Exception) {
            Log.e("HUD", "Connection failed: ${e.message}")
            broadcastUpdate("Disconnected")
        }
    }

    private fun startLocationUpdates() {
        val locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY, 1000
        ).setMinUpdateIntervalMillis(500).build()

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) {
            Log.e("HUD", "Location permission not granted")
            return
        }

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                val location: Location = locationResult.lastLocation ?: return
                val speedKph = location.speed * 3.6f
                sendSpeedToESP32(speedKph)
                broadcastUpdate("Connected", speedKph)
            }
        }

        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback!!, Looper.getMainLooper())
    }

    private fun sendSpeedToESP32(speed: Float) {
        try {
            outputStream?.write("${speed.toInt()}\n".toByteArray())
        } catch (e: Exception) {
            Log.e("HUD", "Failed to send speed: ${e.message}")
            broadcastUpdate("Disconnected")
        }
    }

    private fun broadcastUpdate(status: String, speed: Float? = null) {
        val intent = Intent("HUD_UPDATE")
        intent.putExtra("status", status)
        speed?.let { intent.putExtra("speed", it) }
        sendBroadcast(intent)
    }

    override fun onDestroy() {
        super.onDestroy()
        locationCallback?.let { fusedLocationClient.removeLocationUpdates(it) }
        try {
            outputStream?.close()
            bluetoothSocket?.close()
        } catch (e: Exception) {
            Log.e("HUD", "Cleanup failed: ${e.message}")
        }
        broadcastUpdate("Disconnected")
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
