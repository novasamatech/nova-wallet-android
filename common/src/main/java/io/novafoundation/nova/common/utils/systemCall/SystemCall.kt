package io.novafoundation.nova.common.utils.systemCall

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity

interface SystemCall<T> {

    class Request(
        val intent: Intent,
        val requestCode: Int,
    )

    fun createRequest(activity: AppCompatActivity): Request

    fun parseResult(requestCode: Int, resultCode: Int, intent: Intent?): Result<T>
}
