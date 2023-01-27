package io.novafoundation.nova.feature_account_api.presenatation.mixin.identity

import androidx.lifecycle.LiveData
import io.novafoundation.nova.common.mixin.api.Browserable
import io.novafoundation.nova.common.utils.Event
import io.novafoundation.nova.feature_account_api.data.model.OnChainIdentity
import kotlinx.coroutines.flow.Flow

interface IdentityMixin : Browserable {

    val openEmailEvent: LiveData<Event<String>>

    val identityFlow: Flow<IdentityModel?>

    fun emailClicked()

    fun twitterClicked()

    fun webClicked()

    interface Presentation : IdentityMixin {

        fun setIdentity(identity: IdentityModel?)

        fun setIdentity(identity: OnChainIdentity?)
    }

    interface Factory {

        fun create(): Presentation
    }
}
