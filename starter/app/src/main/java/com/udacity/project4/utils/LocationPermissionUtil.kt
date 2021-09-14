package com.udacity.project4.locationreminders.geofence

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment

private const val REQUEST_LOCATION_PERMISSION = 1

private val runningQOrLater =
    android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q

class LocationPermissionUtil {

    companion object {

        fun isPermissionGrantedByRequestCode(
            requestCode: Int,
            permissions: Array<out String>,
            grantResults: IntArray
        ): Boolean {
            if (requestCode == REQUEST_LOCATION_PERMISSION) {
                return grantResults.isNotEmpty() && (grantResults[0] == PackageManager.PERMISSION_GRANTED)
            }
            return false
        }

        fun isForegroundAndBackgroundLocationPermissionApproved(context: Context?): Boolean {
            val foregroundLocationApproved = (
                    PackageManager.PERMISSION_GRANTED ==
                            context?.let {
                                ActivityCompat.checkSelfPermission(
                                    it,
                                    Manifest.permission.ACCESS_FINE_LOCATION
                                )
                            })
            val backgroundPermissionApproved =
                if (runningQOrLater) {
                    PackageManager.PERMISSION_GRANTED ==
                            context?.let {
                                ActivityCompat.checkSelfPermission(
                                    it, Manifest.permission.ACCESS_BACKGROUND_LOCATION
                                )
                            }
                } else {
                    true
                }
            return foregroundLocationApproved && backgroundPermissionApproved
        }

        fun requestFragmentForegroundPermission(fragment: Fragment) {
            fragment.requestPermissions(
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
                REQUEST_LOCATION_PERMISSION
            )
        }

        fun requestFragmentBackgroundPermission(fragment: Fragment) {
            fragment.requestPermissions(
                arrayOf(Manifest.permission.ACCESS_BACKGROUND_LOCATION),
                REQUEST_LOCATION_PERMISSION
            )
        }

        fun requestFragmentForegroundAndBackgroundPermission(fragment: Fragment) {
            fragment.requestPermissions(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.ACCESS_BACKGROUND_LOCATION
                ),
                REQUEST_LOCATION_PERMISSION
            )
        }
    }
}