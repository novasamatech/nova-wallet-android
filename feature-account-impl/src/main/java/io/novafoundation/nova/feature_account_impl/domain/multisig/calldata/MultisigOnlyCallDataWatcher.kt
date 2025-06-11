package io.novafoundation.nova.feature_account_impl.domain.multisig.calldata

import android.util.Log
import io.novafoundation.nova.common.utils.shareInBackground
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_account_api.domain.model.LightMetaAccount.Type
import io.novafoundation.nova.feature_account_impl.data.multisig.MultisigRepository
import io.novafoundation.nova.runtime.extrinsic.visitor.extrinsic.api.ExtrinsicWalk
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.runtime.repository.EventsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.mapLatest

class MultisigOnlyCallDataWatcher(
    private val eventsRepository: EventsRepository,
    private val extrinsicWalk: ExtrinsicWalk,
    private val chainRegistry: ChainRegistry,
    private val multisigRepository: MultisigRepository,
    private val accountRepository: AccountRepository,
    private val coroutineScope: CoroutineScope,
) : RealtimeCallDataWatcher, CoroutineScope by coroutineScope {

    private val delegate = accountRepository.hasMetaAccountsCountOfTypeFlow(Type.MULTISIG).mapLatest { hasMultisigs ->
        if (hasMultisigs) {
            Log.d("MultisigOnlyCallDataWatcher", "User has multisig wallets - starting to sync realtime call-data")

            val scope = CoroutineScope(coroutineContext)
            val delegate = EventsRealtimeCallDataWatcher(eventsRepository, extrinsicWalk, chainRegistry, multisigRepository, scope)
            delegate
        } else {
            Log.d("MultisigOnlyCallDataWatcher", "User has no multisig wallets - not syncing call-data")

            NoOpRealtimeCallDataWatcher
        }
    }.shareInBackground()

    override val realtimeCallData = delegate.flatMapLatest { it.realtimeCallData }

    override val newMultisigEvents: Flow<MultiChainMultisigEvent> = delegate.flatMapLatest { it.newMultisigEvents }
}
