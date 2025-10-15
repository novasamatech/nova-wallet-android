package io.novafoundation.nova.common.data.providers.deviceid

import android.annotation.SuppressLint
import android.content.Context
import android.provider.Settings.Secure

class AndroidDeviceIdProvider(
    private val context: Context
) : DeviceIdProvider {

    @SuppressLint("HardwareIds")
    override fun getDeviceId(): String {
        return Secure.getString(context.contentResolver, Secure.ANDROID_ID)
    }
}
