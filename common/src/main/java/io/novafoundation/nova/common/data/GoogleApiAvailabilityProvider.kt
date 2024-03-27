package io.novafoundation.nova.common.data

import android.content.Context
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability

interface GoogleApiAvailabilityProvider {

    fun isAvailable(): Boolean
}

internal class RealGoogleApiAvailabilityProvider(
    val context: Context
): GoogleApiAvailabilityProvider {

    override fun isAvailable(): Boolean {
        val googleApiAvailability = GoogleApiAvailability.getInstance()
        val resultCode = googleApiAvailability.isGooglePlayServicesAvailable(context)
        return resultCode == ConnectionResult.SUCCESS
    }
}
