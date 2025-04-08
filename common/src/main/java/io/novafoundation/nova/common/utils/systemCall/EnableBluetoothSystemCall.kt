package io.novafoundation.nova.common.utils.systemCall

import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity

private const val REQUEST_CODE = 733

class EnableBluetoothSystemCall : SystemCall<Boolean> {

    override fun createRequest(activity: AppCompatActivity): SystemCall.Request {
        val intent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)

        return SystemCall.Request(
            intent = intent,
            requestCode = REQUEST_CODE
        )
    }

    override fun parseResult(requestCode: Int, resultCode: Int, intent: Intent?): Result<Boolean> {
        return when {
            resultCode == Activity.RESULT_OK -> Result.success(true)
            else -> Result.success(false)
        }
    }
}
