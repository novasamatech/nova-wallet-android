package io.novafoundation.nova.common.utils

import android.os.Bundle
import android.os.Parcelable
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

fun <T> BaseFragment<*, *>.payload(): T {
    return requireArguments().getParcelable(KEY_PAYLOAD)!!
}
