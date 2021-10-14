package jp.co.soramitsu.feature_account_api.presenatation.actions

import androidx.annotation.StringRes
import androidx.lifecycle.LiveData
import jp.co.soramitsu.common.mixin.api.Browserable
import jp.co.soramitsu.common.utils.Event
import jp.co.soramitsu.runtime.multiNetwork.chain.model.Chain

interface ExternalActions : Browserable {

    class Payload(
        val type: Type,
        val chain: Chain,
        @StringRes val copyLabelRes: Int,
    )

    sealed class Type(val primaryValue: String) {

        class Address(val address: String) : Type(address)

        class Extrinsic(val hash: String) : Type(hash)

        class Event(val id: String) : Type(id)
    }

    val showExternalActionsEvent: LiveData<Event<Payload>>

    fun viewExternalClicked(explorer: Chain.Explorer, type: Type)

    fun copyAddress(address: String, messageShower: (message: String) -> Unit)

    interface Presentation : ExternalActions, Browserable.Presentation {

        fun showExternalActions(type: Type, chain: Chain)
    }
}
