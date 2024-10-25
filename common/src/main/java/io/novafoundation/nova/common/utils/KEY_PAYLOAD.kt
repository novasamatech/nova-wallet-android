package io.novafoundation.nova.common.utils

import android.os.Bundle
import android.os.Parcelable
import androidx.fragment.app.Fragment

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

interface PayloadHolder<T : Parcelable> {

    val payload: T
}

interface FragmentPayloadHolder<T : Parcelable> : PayloadHolder<T> {

    override val payload: T
        get() {
            require(this is Fragment)
            return requireArguments().getParcelable(KEY_PAYLOAD)!!
        }
}
