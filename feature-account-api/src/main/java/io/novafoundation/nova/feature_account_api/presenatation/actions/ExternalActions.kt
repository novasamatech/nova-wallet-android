package io.novafoundation.nova.feature_account_api.presenatation.actions

import android.graphics.drawable.Drawable
import androidx.annotation.StringRes
import androidx.lifecycle.LiveData
import io.novafoundation.nova.common.mixin.api.Browserable
import io.novafoundation.nova.common.utils.Event
import io.novafoundation.nova.feature_account_api.presenatation.chain.ChainUi
import io.novafoundation.nova.runtime.ext.addressOf
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ExplorerTemplateExtractor
import io.novasama.substrate_sdk_android.runtime.AccountId

interface ExternalActions : Browserable {

    class Payload(
        val type: Type,
        val chain: Chain,
        val chainUi: ChainUi?,
        val icon: Drawable?,
        @StringRes val copyLabelRes: Int?,
    )

    sealed class Type(
        val primaryValue: String?,
        val explorerTemplateExtractor: ExplorerTemplateExtractor,
    ) {

        object EmptyAccount : Type(null, explorerTemplateExtractor = Chain.Explorer::account)

        class Address(val address: String) : Type(address, explorerTemplateExtractor = Chain.Explorer::account)

        class Extrinsic(val hash: String) : Type(hash, explorerTemplateExtractor = Chain.Explorer::extrinsic)

        class Event(val id: String) : Type(id, explorerTemplateExtractor = Chain.Explorer::event)
    }

    val showExternalActionsEvent: LiveData<Event<Payload>>

    fun viewExternalClicked(explorer: Chain.Explorer, type: Type)

    fun copyValue(payload: Payload)

    interface Presentation : ExternalActions, Browserable.Presentation {

        suspend fun showExternalActions(type: Type, chain: Chain)
    }
}

suspend fun ExternalActions.Presentation.showAddressActions(accountId: AccountId, chain: Chain) = showAddressActions(
    address = chain.addressOf(accountId),
    chain = chain
)

suspend fun ExternalActions.Presentation.showAddressActions(address: String?, chain: Chain) {
    if (address == null) {
        showExternalActions(ExternalActions.Type.EmptyAccount, chain)
    } else {
        showExternalActions(
            type = ExternalActions.Type.Address(address),
            chain = chain
        )
    }
}
