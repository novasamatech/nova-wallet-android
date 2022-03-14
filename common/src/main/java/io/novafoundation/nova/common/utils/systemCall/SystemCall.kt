package io.novafoundation.nova.common.utils.systemCall

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity

interface SystemCall<T> {

    sealed class Failure : Throwable() {

        class Unknown : Failure()

        class Cancelled : Failure()
    }

    class Request(
        val intent: Intent,
        val requestCode: Int,
    )

    fun createRequest(activity: AppCompatActivity): Request

    fun parseResult(requestCode: Int, resultCode: Int, intent: Intent?): Result<T>
}

inline fun <T> Result<T>.onSystemCallFailure(onFailure: (failure: Throwable) -> Unit) {
    onFailure {
        if (it !is SystemCall.Failure.Cancelled) {
            onFailure(it)
        }
    }
}
