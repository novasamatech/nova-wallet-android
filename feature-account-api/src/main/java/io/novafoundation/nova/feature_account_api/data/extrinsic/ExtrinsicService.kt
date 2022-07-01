package io.novafoundation.nova.feature_account_api.data.extrinsic

import io.novafoundation.nova.common.data.network.runtime.model.FeeResponse
import io.novafoundation.nova.common.data.secrets.v2.SecretStoreV2
import io.novafoundation.nova.common.data.secrets.v2.getAccountSecrets
import io.novafoundation.nova.common.utils.orZero
import io.novafoundation.nova.common.utils.takeWhileInclusive
import io.novafoundation.nova.common.utils.tip
import io.novafoundation.nova.feature_account_api.data.secrets.keypair
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount
import io.novafoundation.nova.feature_account_api.domain.model.accountIdIn
import io.novafoundation.nova.feature_account_api.domain.model.cryptoTypeIn
import io.novafoundation.nova.runtime.extrinsic.ExtrinsicBuilderFactory
import io.novafoundation.nova.runtime.extrinsic.ExtrinsicStatus
import io.novafoundation.nova.runtime.extrinsic.create
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import io.novafoundation.nova.runtime.network.rpc.RpcCalls
import jp.co.soramitsu.fearless_utils.encrypt.keypair.Keypair
import jp.co.soramitsu.fearless_utils.extensions.toHexString
import jp.co.soramitsu.fearless_utils.runtime.definitions.types.fromHex
import jp.co.soramitsu.fearless_utils.runtime.definitions.types.generics.Extrinsic
import jp.co.soramitsu.fearless_utils.runtime.extrinsic.ExtrinsicBuilder
import kotlinx.coroutines.flow.Flow
import java.math.BigInteger

class ExtrinsicService(
    private val rpcCalls: RpcCalls,
    private val accountRepository: AccountRepository,
    private val secretStoreV2: SecretStoreV2,
    private val extrinsicBuilderFactory: ExtrinsicBuilderFactory,
) {

    suspend fun submitExtrinsic(
        chain: Chain,
        formExtrinsic: suspend ExtrinsicBuilder.() -> Unit,
    ): Result<*> {
        val account = accountRepository.getSelectedMetaAccount()
        val accountId = account.accountIdIn(chain)!!

        return submitExtrinsic(chain, accountId, formExtrinsic)
    }

    suspend fun submitAndWatchExtrinsic(
        chain: Chain,
        formExtrinsic: suspend ExtrinsicBuilder.() -> Unit,
    ): Flow<ExtrinsicStatus> {
        val account = accountRepository.getSelectedMetaAccount()
        val accountId = account.accountIdIn(chain)!!

        return submitAndWatchExtrinsic(chain, accountId, formExtrinsic)
    }

    suspend fun submitExtrinsic(
        chain: Chain,
        accountId: ByteArray,
        formExtrinsic: suspend ExtrinsicBuilder.() -> Unit,
    ): Result<String> = runCatching {
        val extrinsic = buildExtrinsic(chain, accountId, formExtrinsic)

        rpcCalls.submitExtrinsic(chain.id, extrinsic)
    }

    suspend fun submitAndWatchExtrinsic(
        chain: Chain,
        accountId: ByteArray,
        formExtrinsic: suspend ExtrinsicBuilder.() -> Unit,
    ): Flow<ExtrinsicStatus> {
        val extrinsic = buildExtrinsic(chain, accountId, formExtrinsic)

        return rpcCalls.submitAndWatchExtrinsic(chain.id, extrinsic)
            .takeWhileInclusive { !it.terminal }
    }

    private suspend fun buildExtrinsic(
        chain: Chain,
        accountId: ByteArray,
        formExtrinsic: suspend ExtrinsicBuilder.() -> Unit,
    ): String {
        val metaAccount = accountRepository.findMetaAccount(accountId) ?: error("No meta account found accessing ${accountId.toHexString()}")
        val keypair = secretStoreV2.getKeypairFor(metaAccount, chain, accountId)

        val extrinsicBuilder = extrinsicBuilderFactory.create(chain, keypair, metaAccount.cryptoTypeIn(chain))

        extrinsicBuilder.formExtrinsic()

        return extrinsicBuilder.build(useBatchAll = true)
    }

    suspend fun paymentInfo(
        chain: Chain,
        formExtrinsic: suspend ExtrinsicBuilder.() -> Unit,
    ): FeeResponse {
        val extrinsic = extrinsicBuilderFactory.create(chain)
            .also { it.formExtrinsic() }
            .build()

        return rpcCalls.getExtrinsicFee(chain.id, extrinsic)
    }

    suspend fun estimateFee(
        chain: Chain,
        formExtrinsic: suspend ExtrinsicBuilder.() -> Unit,
    ): BigInteger {
        val extrinsicBuilder = extrinsicBuilderFactory.create(chain)
        extrinsicBuilder.formExtrinsic()
        val extrinsic = extrinsicBuilder.build()

        val extrinsicType = Extrinsic.create(extrinsicBuilder.runtime)
        val decodedExtrinsic = extrinsicType.fromHex(extrinsicBuilder.runtime, extrinsic)

        val tip = decodedExtrinsic.tip().orZero()
        val baseFee = rpcCalls.getExtrinsicFee(chain.id, extrinsic).partialFee

        return tip + baseFee
    }

    suspend fun estimateFee(chainId: ChainId, extrinsic: String): BigInteger {
        return rpcCalls.getExtrinsicFee(chainId, extrinsic).partialFee
    }

    private suspend fun SecretStoreV2.getKeypairFor(
        metaAccount: MetaAccount,
        chain: Chain,
        accountId: ByteArray,
    ): Keypair {
        return getAccountSecrets(metaAccount.id, accountId).keypair(chain)
    }
}
