package com.udacity.project4.locationreminders.savereminder

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.PendingIntent
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.databinding.DataBindingUtil
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.android.gms.maps.model.LatLng
import com.google.android.material.snackbar.Snackbar
import com.udacity.project4.R
import com.udacity.project4.base.BaseFragment
import com.udacity.project4.base.NavigationCommand
import com.udacity.project4.databinding.FragmentSaveReminderBinding
import com.udacity.project4.locationreminders.data.local.LocalDB
import com.udacity.project4.locationreminders.data.local.RemindersLocalRepository
import com.udacity.project4.locationreminders.geofence.GeofenceHelper
import com.udacity.project4.locationreminders.geofence.RemindersGeoFence
import com.udacity.project4.locationreminders.reminderslist.ReminderDataItem
import com.udacity.project4.utils.setDisplayHomeAsUpEnabled
import org.koin.android.ext.android.inject

private const val REQUEST_FOREGROUND_AND_BACKGROUND_PERMISSION_RESULT_CODE = 33
private const val REQUEST_FOREGROUND_ONLY_PERMISSIONS_REQUEST_CODE = 34
private const val REQUEST_TURN_DEVICE_LOCATION_ON = 29
private const val LOCATION_PERMISSION_INDEX = 0
private const val BACKGROUND_LOCATION_PERMISSION_INDEX = 1
private const val REQUEST_LOCATION_PERMISSION = 1

class SaveReminderFragment : BaseFragment() {

    private lateinit var geofencingClient: GeofencingClient
    private lateinit var geofenceHelper: GeofenceHelper
    private lateinit var geofencingRequest: GeofencingRequest

    //Get the view model this time as a single to be shared with the another fragment
    override val _viewModel: SaveReminderViewModel by inject()
    private lateinit var binding: FragmentSaveReminderBinding
    private lateinit var remindersGeoFence: RemindersGeoFence

    private lateinit var repository: RemindersLocalRepository

    private lateinit var reminder: ReminderDataItem

    private lateinit var geofence: Geofence
    private lateinit var pendingsIntent: PendingIntent

    val GEOFENCE_RADIUS = 30f


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding =
            DataBindingUtil.inflate(inflater, R.layout.fragment_save_reminder, container, false)

        setDisplayHomeAsUpEnabled(true)

        binding.viewModel = _viewModel

        return binding.root
    }

    @SuppressLint("MissingPermission")
    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        binding.lifecycleOwner = this
        binding.selectLocation.setOnClickListener {
            //            Navigate to another fragment to get the user location
            _viewModel.navigationCommand.value =
                NavigationCommand.To(SaveReminderFragmentDirections.actionSaveReminderFragmentToSelectLocationFragment())
        }

        remindersGeoFence = RemindersGeoFence(context!!, view!!, this)

        val remindersDao = LocalDB.createRemindersDao(context!!)
        repository = RemindersLocalRepository(remindersDao)

        binding.saveReminder.setOnClickListener {
            val title = _viewModel.reminderTitle.value
            val description = _viewModel.reminderDescription.value
            val location = _viewModel.reminderSelectedLocationStr.value
            val latitude = _viewModel.latitude.value
            val longitude = _viewModel.longitude.value

            //use the user entered reminder details to:
//             1) add a geofencing request
//             2) save the reminder to the local db

            reminder = ReminderDataItem(title, description, location, latitude, longitude)

            if (_viewModel.validateEnteredData(reminder)) {
                addGeofenceAndSaveReminder(LatLng(latitude!!, longitude!!), reminder.id)
            }

        }

        geofenceHelper = GeofenceHelper(context)
        geofencingClient = LocationServices.getGeofencingClient(context!!);
        pendingsIntent = geofenceHelper.getPendingsIntent()!!

        requestPermission()
        checkDeviceLocationSettingsAndStartGeofence()

    }

    fun requestPermission() {
        if (ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
            && ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
            && ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_BACKGROUND_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {

            requestPermissions(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.ACCESS_BACKGROUND_LOCATION
                ),
                1
            )
            return
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        // Check if location permissions are granted and if so enable the
        // location data layer.
        if (requestCode == REQUEST_LOCATION_PERMISSION) {
            if (grantResults.isNotEmpty() && (grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                Toast.makeText(context, "All permissions granged", Toast.LENGTH_SHORT)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        //make sure to clear the view model after destroy, as it's a single view model.
        _viewModel.onClear()
    }

    @SuppressLint("MissingPermission")
    fun addGeofenceAndSaveReminder(latLng: LatLng, geoId: String) {
        geofence = geofenceHelper.getGeofence(
            geoId,
            latLng,
            GEOFENCE_RADIUS,
            Geofence.GEOFENCE_TRANSITION_ENTER
        )
        geofencingRequest = geofenceHelper.getGeofencingRequest(geofence)
        pendingsIntent = geofenceHelper.getPendingsIntent()!!

        checkDeviceLocationSettingsAndStartGeofence(true)
        geofencingClient.addGeofences(geofencingRequest, pendingsIntent)?.run {
            addOnSuccessListener {
                _viewModel.saveReminder(reminder)
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
        val settingsClient = LocationServices.getSettingsClient(context!!)
        val locationSettingsResponseTask =
            settingsClient.checkLocationSettings(builder.build())
        locationSettingsResponseTask.addOnFailureListener { exception ->

            if (exception is ResolvableApiException && resolve) {
                try {
                    startIntentSenderForResult(
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