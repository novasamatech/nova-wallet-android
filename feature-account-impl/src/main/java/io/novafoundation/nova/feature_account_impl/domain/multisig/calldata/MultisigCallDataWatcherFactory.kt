package io.novafoundation.nova.feature_account_impl.domain.multisig.calldata

import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_account_impl.data.multisig.MultisigRepository
import io.novafoundation.nova.feature_account_api.data.multisig.repository.MultisigOperationLocalCallRepository
import io.novafoundation.nova.runtime.extrinsic.visitor.extrinsic.api.ExtrinsicWalk
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.runtime.repository.EventsRepository
import kotlinx.coroutines.CoroutineScope
import javax.inject.Inject

@FeatureScope
class MultisigCallDataWatcherFactory @Inject constructor(
    private val eventsRepository: EventsRepository,
    private val extrinsicWalk: ExtrinsicWalk,
    private val chainRegistry: ChainRegistry,
    private val multisigRepository: MultisigRepository,
    private val accountRepository: AccountRepository,
    private val multisigOperationLocalCallRepository: MultisigOperationLocalCallRepository,
) {

    fun createOnlyMultisig(coroutineScope: CoroutineScope): MultisigCallDataWatcher {
        return MultisigOnlyCallDataWatcher(
            eventsRepository = eventsRepository,
            extrinsicWalk = extrinsicWalk,
            chainRegistry = chainRegistry,
            multisigRepository = multisigRepository,
            accountRepository = accountRepository,
            multisigOperationLocalCallRepository = multisigOperationLocalCallRepository,
            coroutineScope = coroutineScope
        )
    }
}
