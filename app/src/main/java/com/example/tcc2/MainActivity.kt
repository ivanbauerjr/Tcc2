package com.example.tcc2

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import com.example.tcc2.models.LocationViewModel
import com.example.tcc2.ui.theme.Tcc2Theme
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.material.snackbar.Snackbar

class MainActivity : ComponentActivity() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        val locationViewModel: LocationViewModel by viewModels()

        requestPermissions(locationViewModel)

        setContent {
            Tcc2Theme {
                Navigation(
                    locationViewModel = locationViewModel,
                    onGetUserLocation = { callback ->
                        getUserLocation(callback, locationViewModel)
                    }
                )
            }
        }
    }

    private fun requestPermissions(locationViewModel: LocationViewModel) {
        val requestPermissionLauncher =
            registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
                if (!isGranted) {
                    showPermissionDeniedMessage()
                } else {
                    checkLocationServicesEnabled(locationViewModel)
                }
            }

        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        } else {
            checkLocationServicesEnabled(locationViewModel)
        }
    }

    private fun getUserLocation(callback: (Double, Double) -> Unit, locationViewModel: LocationViewModel) {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                if (location != null) {
                    locationViewModel.setLocation(location.latitude, location.longitude)
                    callback(location.latitude, location.longitude)
                } else {
                    Snackbar.make(
                        findViewById(android.R.id.content),
                        "Unable to retrieve location.",
                        Snackbar.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    private fun showPermissionDeniedMessage() {
        Snackbar.make(
            findViewById(android.R.id.content),
            "Location permission is required for this feature.",
            Snackbar.LENGTH_LONG
        ).setAction("Settings") {
            startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", packageName, null)
            })
        }.show()
    }

    private fun checkLocationServicesEnabled(locationViewModel: LocationViewModel) {
        val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            Snackbar.make(
                findViewById(android.R.id.content),
                "Location services are disabled. Please enable them in settings.",
                Snackbar.LENGTH_LONG
            ).setAction("Settings") {
                startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
            }.show()
        } else {
            // If GPS is enabled, fetch the location
            getUserLocation({ lat, lon -> locationViewModel.setLocation(lat, lon) }, locationViewModel)
        }
    }
}
