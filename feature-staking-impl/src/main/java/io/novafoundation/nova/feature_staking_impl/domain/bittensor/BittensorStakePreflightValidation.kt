package io.novafoundation.nova.feature_staking_impl.domain.bittensor

import io.novafoundation.nova.common.validation.Validation
import io.novafoundation.nova.common.validation.ValidationStatus
import io.novafoundation.nova.common.validation.ValidationSystem
import io.novafoundation.nova.feature_account_api.data.model.Fee
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_account_api.domain.model.requireAccountIdIn
import io.novafoundation.nova.feature_staking_impl.data.bittensor.BittensorStakePreflightRepository
import io.novafoundation.nova.feature_wallet_api.domain.interfaces.WalletRepository
import io.novafoundation.nova.feature_wallet_api.domain.model.amountFromPlanks
import io.novafoundation.nova.runtime.ext.utilityAsset
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novasama.substrate_sdk_android.runtime.AccountId
import java.math.BigInteger
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first

data class BittensorStakePreflightPayload(
    val chain: Chain,
    val metaId: Long,
    val netuid: BittensorNetuid,
    val hotkey: AccountId,
    val amount: BigInteger,
    val fee: Fee,
    val quote: BittensorSubnetBuyQuote? = null,
    val checkRootHold: Boolean = false,
    val priceImpactAccepted: Boolean = false
)

typealias BittensorStakePreflightSystem = ValidationSystem<BittensorStakePreflightPayload, BittensorStakeValidationFailure>

class BittensorStakePreflightValidation(
    private val preflightRepository: BittensorStakePreflightRepository,
    private val accountRepository: AccountRepository,
    private val walletRepository: WalletRepository,
    private val stakeValidationSystem: BittensorStakeValidationSystem,
    private val bittensorSubnetInteractor: BittensorSubnetInteractor
) : Validation<BittensorStakePreflightPayload, BittensorStakeValidationFailure> {

    override suspend fun validate(value: BittensorStakePreflightPayload): ValidationStatus<BittensorStakeValidationFailure> = coroutineScope {
        val account = accountRepository.getMetaAccount(value.metaId)
        val preflight = async {
            preflightRepository.load(value.chain.id, value.netuid, account.requireAccountIdIn(value.chain), value.hotkey)
        }
        val latestQuote = async {
            value.quote?.let { bittensorSubnetInteractor.quote(it.subnetId, it.totalTaoInPlanks) }
        }
        val asset = walletRepository.assetFlow(value.metaId, value.chain.utilityAsset)
            .first()
        val currentQuote = latestQuote.await()
        val payload = BittensorStakeValidationPayload(
            asset = asset,
            amount = asset.token.amountFromPlanks(value.amount),
            fee = value.fee,
            preflight = preflight.await(),
            quote = value.quote,
            netuid = value.netuid,
            checkRootHold = value.checkRootHold,
            priceImpactAccepted = value.priceImpactAccepted,
            checkedAtMillis = System.currentTimeMillis(),
            latestQuote = currentQuote
        )
        stakeValidationSystem.validate(payload)
            .getOrThrow()
    }
}
