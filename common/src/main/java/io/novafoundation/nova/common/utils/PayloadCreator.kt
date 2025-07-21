package io.novafoundation.nova.common.utils

import android.os.Bundle
import android.os.Parcelable
import androidx.fragment.app.Fragment
import io.novafoundation.nova.common.base.BaseFragment

const val KEY_PAYLOAD = "KEY_PAYLOAD"

interface PayloadCreator<T : Parcelable?> {

    fun createPayload(payload: T): Bundle
}

class FragmentPayloadCreator<T : Parcelable> : PayloadCreator<T> {

    override fun createPayload(payload: T): Bundle {
        return Bundle().apply {
            putParcelable(KEY_PAYLOAD, payload)
        }
    }
}

fun <T> Fragment.payload(): T {
    return requireArguments().getParcelable(KEY_PAYLOAD)!!
}

fun <T> Fragment.payloadOrNull(): T? {
    return arguments?.getParcelable(KEY_PAYLOAD) as? T
}

fun <T> Fragment.payloadOrElse(fallback: () -> T): T {
    return payloadOrNull() ?: fallback()
}
