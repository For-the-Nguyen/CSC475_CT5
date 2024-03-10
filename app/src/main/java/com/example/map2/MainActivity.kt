package com.example.map2

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Looper
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.map2.ui.theme.Map2Theme
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.MapsInitializer
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.CameraPositionState
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState

class MainActivity : ComponentActivity() {
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private var locationRequired: Boolean= false

    private val permissions = arrayOf(
        android.Manifest.permission.ACCESS_COARSE_LOCATION,
        android.Manifest.permission.ACCESS_FINE_LOCATION
                /**
        android.Manifest.permission.ACCESS_BACKGROUND_LOCATION,
        android.Manifest.permission.INTERNET**/
    )

    override fun onResume(){
        super.onResume()
        if(locationRequired){
            startLocationUpdates()
        }
    }

    override fun onPause(){
        super.onPause()
        locationCallback?.let{
            fusedLocationClient?.removeLocationUpdates(it)
        }
    }

    @SuppressLint("Missing Permission")
    private fun startLocationUpdates() {
        locationCallback?.let{
            val locationRequest = LocationRequest.Builder(
                Priority.PRIORITY_HIGH_ACCURACY, 200
            )
                .setWaitForAccurateLocation(false)
                .setMinUpdateIntervalMillis(3000)
                .setMaxUpdateAgeMillis(200)
                .build()

            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return
            }
            fusedLocationClient?.requestLocationUpdates(
                locationRequest,
                it,
                Looper.getMainLooper()
            )
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        MapsInitializer.initialize(this, MapsInitializer.Renderer.LATEST){

        }

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)


        setContent {

            var currentLocation by remember{
                mutableStateOf(LatLng(0.toDouble(),0.toDouble()))
            }

            val cameraPosition = rememberCameraPositionState{
                position = CameraPosition.fromLatLngZoom(
                    currentLocation,10f
                )
            }

            var cameraPositionState by remember{
                mutableStateOf(cameraPosition)
            }

            locationCallback = object: LocationCallback(){
                override fun onLocationResult(p0: LocationResult) {
                    super.onLocationResult(p0)
                    for (location in p0.locations){
                        currentLocation = LatLng(location.latitude,location.longitude)

                        cameraPositionState = CameraPositionState(
                            position = CameraPosition.fromLatLngZoom(
                                currentLocation,10f
                            )
                        )
                    }
                }
            }

            Map2Theme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    /**
                    Box(modifier=Modifier.fillMaxSize()){
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(text = "Location: ")
                            Button(onClick = { /*TODO*/ }) {
                                Text(text = "Get Location")
                            }
                        }
                    }**/
                    LocationScreen(this@MainActivity, currentLocation, cameraPositionState)
                }
            }
        }
    }

    @Composable
    private fun LocationScreen(context: Context, currentLocation: LatLng, camerapositionState: CameraPositionState) {

        val launchMultiplePermissions = rememberLauncherForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()){
            permissionMaps ->
            val areGranted = permissionMaps.values.reduce{acc, next -> acc && next}
            if(areGranted){
                locationRequired = true
                startLocationUpdates()
                Toast.makeText(context, "Permission Granted", Toast.LENGTH_SHORT).show()

            }else{
                Toast.makeText(context, "Permission Denied", Toast.LENGTH_SHORT).show()
            }
        }


        Box(modifier=Modifier.fillMaxSize()){
            GoogleMap(
                modifier = Modifier.fillMaxSize().padding(16.dp),
                cameraPositionState = camerapositionState
            ){
                Marker(
                    state = MarkerState(
                        position = currentLocation,
                    ) ,
                    title = "Map",
                    snippet = "Your Location"
                )

            }
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(text = "Location: ${currentLocation.latitude},${currentLocation.longitude}")
                Button(onClick = {
                    if(permissions.all{
                        ContextCompat.checkSelfPermission(context,it) == PackageManager.PERMISSION_GRANTED
                        }) {
                        startLocationUpdates()
                    }
                    else{
                        launchMultiplePermissions.launch(permissions)
                    }
                }) {
                    Text(text = "Get Location")
                }
            }

        }
    }
}

