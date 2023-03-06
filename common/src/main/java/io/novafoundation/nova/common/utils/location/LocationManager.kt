package io.novafoundation.nova.common.utils.location

import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import io.novafoundation.nova.common.resources.ContextManager
import io.novafoundation.nova.common.utils.whenStarted
import android.location.LocationManager as NativeLocationManager

interface LocationManager {

    fun enableLocation()

    fun isLocationEnabled(): Boolean
}

class RealLocationManager(private val contextManager: ContextManager) : LocationManager {

    private val locationManager = contextManager.getApplicationContext().getSystemService(Context.LOCATION_SERVICE) as NativeLocationManager

    override fun enableLocation() {
        val activity = contextManager.getActivity()!!
        activity.lifecycle.whenStarted {
            if (!locationManager.isProviderEnabled(NativeLocationManager.GPS_PROVIDER)) {
                val enableLocationIntent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                contextManager.getActivity()
                    ?.startActivityForResult(enableLocationIntent, 0)
            }
        }
    }

    override fun isLocationEnabled(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            locationManager.isLocationEnabled
        } else {
            locationManager.isProviderEnabled(NativeLocationManager.GPS_PROVIDER)
        }
    }
}
