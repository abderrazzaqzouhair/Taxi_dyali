package com.app.taxi_dyali

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.os.Looper
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.*
import org.osmdroid.views.MapView
import org.osmdroid.config.Configuration
import org.osmdroid.views.overlay.Marker
import org.osmdroid.util.GeoPoint
import android.location.Location
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.material.bottomnavigation.BottomNavigationView
import pub.devrel.easypermissions.EasyPermissions
import java.text.DecimalFormat

class HomeActivity : AppCompatActivity(), EasyPermissions.PermissionCallbacks {

    private lateinit var mapView: MapView
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback

    private var totalDistance = 0.0 // in km
    private var totalTimeSeconds = 0 // in seconds
    private var isTracking = false
    private var lastLocation: GeoPoint? = null
    private var fare = 0.0

    private var rideTimerJob: Job? = null // Coroutine Job for timer
    private val LOCATION_PERMISSION_CODE = 123 // Permission code

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Configure osmdroid
        Configuration.getInstance().load(
            applicationContext,
            androidx.preference.PreferenceManager.getDefaultSharedPreferences(applicationContext)
        )

        setContentView(R.layout.activity_home)

        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottom_navigatn)
        bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    Intent(this, TripActivity::class.java).also { startActivity(it) }
                    true
                }
                R.id.nav_leaderboard -> {
                    //Intent(this, HomeActivity::class.java).also { startActivity(it) }
                    true
                }
                R.id.nav_quiz -> {
                    Intent(this, HistoryActivity::class.java).also { startActivity(it) }
                    true
                }

                R.id.nav_profile -> {
                    Intent(this, ProfileActivity::class.java).also { startActivity(it) }
                    true
                }

                else -> false
            }
        }



        // UI elements
        mapView = findViewById(R.id.mapView)
        val distanceText: TextView = findViewById(R.id.distanceText)
        val timeText: TextView = findViewById(R.id.timeText)
        val fareText: TextView = findViewById(R.id.fareText)
        val startButton: Button = findViewById(R.id.startButton)

        // Setup MapView
        mapView.setMultiTouchControls(true)
        mapView.controller.setZoom(15.0)
        val initialPoint = GeoPoint(0.0, 0.0) // Default location
        mapView.controller.setCenter(initialPoint)

        // Setup location services
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        // Check for permissions
        if (hasLocationPermission()) {
            setupStartButton(startButton, timeText, distanceText, fareText)
        } else {
            requestLocationPermission()
        }
    }

    private fun hasLocationPermission(): Boolean {
        return EasyPermissions.hasPermissions(this, Manifest.permission.ACCESS_FINE_LOCATION)
    }

    private fun requestLocationPermission() {
        EasyPermissions.requestPermissions(
            this,
            "This app needs location permissions to function.",
            LOCATION_PERMISSION_CODE,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
    }

    private fun setupStartButton(
        startButton: Button,
        timeText: TextView,
        distanceText: TextView,
        fareText: TextView
    ) {
        startButton.setOnClickListener {
            if (!isTracking) {
                isTracking = true
                totalDistance = 0.0
                totalTimeSeconds = 0
                lastLocation = null
                fare = 0.0
                startLocationUpdates()
                startTimer(timeText, fareText)
                Toast.makeText(this, "Ride Started!", Toast.LENGTH_SHORT).show()
                startButton.text = "End Ride"
            } else {
                isTracking = false
                stopLocationUpdates()
                stopTimer()
                Toast.makeText(this, "Ride Ended!", Toast.LENGTH_SHORT).show()
                fareText.text = "${DecimalFormat("#.##").format(fare)} DH"
                startButton.text = "Start Ride"
            }
        }
    }

    private fun startTimer(timeText: TextView, fareText: TextView) {
        rideTimerJob = CoroutineScope(Dispatchers.Main).launch {
            while (isTracking) {
                delay(1000) // Delay 1 second
                totalTimeSeconds++
                val minutes = totalTimeSeconds / 60
                val seconds = totalTimeSeconds % 60
                timeText.text = "${String.format("%02d:%02d", minutes, seconds)}"
                fare = calculateFare(totalDistance, totalTimeSeconds / 60)
                fareText.text = "${DecimalFormat("#.##").format(fare)} DH"
            }
        }
    }

    private fun stopTimer() {
        rideTimerJob?.cancel() // Cancel the timer coroutine
    }

    private fun startLocationUpdates() {
        val locationRequest = LocationRequest.create().apply {
            interval = 1000
            fastestInterval = 500
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        }

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                val location = locationResult.lastLocation ?: return
                val currentPoint = GeoPoint(location.latitude, location.longitude)

                val marker = Marker(mapView)
                marker.position = currentPoint
                marker.title = "Current Location"
                mapView.overlays.add(marker)
                mapView.controller.animateTo(currentPoint)

                // Calculate distance
                if (lastLocation != null) {
                    val results = FloatArray(1)
                    Location.distanceBetween(
                        lastLocation!!.latitude, lastLocation!!.longitude,
                        currentPoint.latitude, currentPoint.longitude,
                        results
                    )
                    totalDistance += results[0]
                }

                // Update distance
                findViewById<TextView>(R.id.distanceText).text =
                    "${DecimalFormat("#.##").format(totalDistance)} m"
                lastLocation = currentPoint
            }
        }

        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            Looper.getMainLooper()
        )
    }

    private fun stopLocationUpdates() {
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }

    private fun calculateFare(distanceKm: Double, timeMinutes: Int): Double {
        val baseFare = 2.5
        val perKmRate = 1.5
        val perMinuteRate = 0.5
        return baseFare + (distanceKm * perKmRate) + (timeMinutes * perMinuteRate)
    }

    override fun onPermissionsGranted(requestCode: Int, perms: List<String>) {
        if (requestCode == LOCATION_PERMISSION_CODE) {
            setupStartButton(
                findViewById(R.id.startButton),
                findViewById(R.id.timeText),
                findViewById(R.id.distanceText),
                findViewById(R.id.fareText)
            )
        }
    }

    override fun onPermissionsDenied(requestCode: Int, perms: List<String>) {
        if (requestCode == LOCATION_PERMISSION_CODE) {
            Toast.makeText(this, "Location permissions are required for this app.", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this)
    }
}
