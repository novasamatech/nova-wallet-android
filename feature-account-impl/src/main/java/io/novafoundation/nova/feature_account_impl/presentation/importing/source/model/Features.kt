package io.novafoundation.nova.feature_account_impl.presentation.importing.source.model

import android.net.Uri
import androidx.lifecycle.LiveData
import io.novafoundation.nova.common.utils.Event

typealias RequestCode = Int

interface FileRequester {
    val chooseJsonFileEvent: LiveData<Event<RequestCode>>

    fun fileChosen(uri: Uri)
}
