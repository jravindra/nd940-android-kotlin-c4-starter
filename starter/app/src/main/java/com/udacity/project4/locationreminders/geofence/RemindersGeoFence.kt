package com.udacity.project4.locationreminders.geofence

import android.annotation.SuppressLint
import android.app.Activity
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.IntentSender
import android.provider.Settings
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationSettingsRequest
import com.google.android.gms.maps.model.LatLng
import com.google.android.material.snackbar.Snackbar
import com.udacity.project4.R


private const val REQUEST_FOREGROUND_AND_BACKGROUND_PERMISSION_RESULT_CODE = 33
private const val REQUEST_FOREGROUND_ONLY_PERMISSIONS_REQUEST_CODE = 34
private const val REQUEST_TURN_DEVICE_LOCATION_ON = 29
private const val LOCATION_PERMISSION_INDEX = 0
private const val BACKGROUND_LOCATION_PERMISSION_INDEX = 1

class RemindersGeoFence(private var context: Context, private var view: View, private var fragment: Fragment) {


    private val geofenceHelper = GeofenceHelper(context)
    private val geofencingClient = LocationServices.getGeofencingClient(context);

    private lateinit var geofence: Geofence
    private lateinit var pendingsIntent: PendingIntent

    val GEOFENCE_RADIUS = 30f

    @SuppressLint("MissingPermission")
    fun addGeofence(latLng: LatLng, geoId: String) {
        geofence = geofenceHelper.getGeofence(
            geoId,
            latLng,
            GEOFENCE_RADIUS,
            Geofence.GEOFENCE_TRANSITION_ENTER
        )
        val geofencingRequest = geofenceHelper.getGeofencingRequest(geofence)
        pendingsIntent = geofenceHelper.getPendingsIntent()!!

        checkDeviceLocationSettingsAndStartGeofence(true)
        geofencingClient.addGeofences(geofencingRequest, pendingsIntent)?.run {
            addOnSuccessListener {
                Toast.makeText(
                    context,
                    "Successfully added location to geofence",
                    Toast.LENGTH_SHORT
                )
            }
            addOnFailureListener {
                Log.e("TAG", it.stackTrace.toString())
                Toast.makeText(context, "Failed to add location to geofence", Toast.LENGTH_SHORT)
            }
        }
    }

    fun removeGeofences() {
        if (this::pendingsIntent.isInitialized) {
            geofencingClient.removeGeofences(pendingsIntent)?.run {
                addOnSuccessListener {
                    Toast.makeText(
                        context,
                        "Successfully removed geo fences",
                        Toast.LENGTH_SHORT
                    )
                }
                addOnFailureListener {
                    Toast.makeText(
                        context,
                        "Failed to remove geo fences",
                        Toast.LENGTH_SHORT
                    )
                }
            }
        }
    }


    fun checkDeviceLocationSettingsAndStartGeofence(resolve: Boolean = true) {
        val locationRequest = LocationRequest.create().apply {
            priority = LocationRequest.PRIORITY_LOW_POWER
        }
        val builder = LocationSettingsRequest.Builder().addLocationRequest(locationRequest)
        val settingsClient = LocationServices.getSettingsClient(context)
        val locationSettingsResponseTask =
            settingsClient.checkLocationSettings(builder.build())
        locationSettingsResponseTask.addOnFailureListener { exception ->

            if (exception is ResolvableApiException && resolve) {
                try {
                    fragment.startIntentSenderForResult(
                        exception.resolution.intentSender,
                        REQUEST_TURN_DEVICE_LOCATION_ON,
                        null,
                        0,
                        0,
                        0,
                        null
                    )
                } catch (sendEx: IntentSender.SendIntentException) {
                    Log.d("ReminderGeoFence", "Error getting location settings resolution: " + sendEx.message)
                    showSnackBarOk()
                }
            } else {
                showSnackBarOk()
            }
        }
        locationSettingsResponseTask.addOnCompleteListener {
            if (it.isSuccessful) {
                Toast.makeText(context, "Location services enabled!", Toast.LENGTH_SHORT)
            }
        }
    }

    private fun showSnackBarOk() {
        Snackbar.make(
            view!!,
            R.string.location_required_error, Snackbar.LENGTH_INDEFINITE
        ).setAction(android.R.string.ok) {
            checkDeviceLocationSettingsAndStartGeofence()
        }.show()
    }

    private class LocationSettingsListener(private val activity: Activity) :
        View.OnClickListener {
        override fun onClick(v: View) {
            activity.startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
        }
    }
}