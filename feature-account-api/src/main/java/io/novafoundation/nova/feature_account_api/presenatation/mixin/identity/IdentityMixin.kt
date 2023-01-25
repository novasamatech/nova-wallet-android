package io.novafoundation.nova.feature_account_api.presenatation.mixin.identity

import androidx.lifecycle.LiveData
import io.novafoundation.nova.common.mixin.api.Browserable
import io.novafoundation.nova.common.utils.Event

interface IdentityMixin : Browserable {

    val openEmailEvent: LiveData<Event<String>>

    fun emailClicked()

    fun twitterClicked()

    fun webClicked()

    interface Presentation: IdentityMixin {

        fun setIdentity(identity: IdentityModel?)
    }

    interface Factory {

        fun create(): Presentation
    }
}
