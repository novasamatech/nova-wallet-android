package io.novafoundation.nova.app.root.presentation.common

import android.content.Context
import com.google.firebase.Firebase
import com.google.firebase.appcheck.appCheck
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory
import com.google.firebase.initialize
import io.novafoundation.nova.common.interfaces.ExternalServiceInitializer

class FirebaseServiceInitializer(private val context: Context) : ExternalServiceInitializer {

    override fun initialize() {
        Firebase.initialize(context = context)
        Firebase.appCheck.installAppCheckProviderFactory(
            PlayIntegrityAppCheckProviderFactory.getInstance(),
        )
    }
}
