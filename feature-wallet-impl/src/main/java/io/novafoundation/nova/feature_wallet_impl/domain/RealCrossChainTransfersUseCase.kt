package io.novafoundation.nova.feature_wallet_impl.domain

import io.novafoundation.nova.common.data.memory.ComputationalCache
import io.novafoundation.nova.common.utils.combineToPair
import io.novafoundation.nova.common.utils.isPositive
import io.novafoundation.nova.common.utils.withFlowScope
import io.novafoundation.nova.feature_account_api.data.extrinsic.ExtrinsicService
import io.novafoundation.nova.feature_account_api.data.extrinsic.SubmissionOrigin
import io.novafoundation.nova.feature_account_api.data.model.SubstrateFee
import io.novafoundation.nova.feature_account_api.data.model.SubstrateFeeBase
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.tranfers.AssetTransferBase
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.tranfers.AssetTransferDirection
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import io.novafoundation.nova.feature_wallet_api.data.network.crosschain.CrossChainTransactor
import io.novafoundation.nova.feature_wallet_api.data.network.crosschain.CrossChainTransfersRepository
import io.novafoundation.nova.feature_wallet_api.data.network.crosschain.CrossChainWeigher
import io.novafoundation.nova.feature_wallet_api.data.network.crosschain.paidByAccountOrNull
import io.novafoundation.nova.feature_wallet_api.data.repository.getXcmChain
import io.novafoundation.nova.feature_wallet_api.domain.interfaces.CrossChainTransfersUseCase
import io.novafoundation.nova.feature_wallet_api.domain.interfaces.IncomingDirection
import io.novafoundation.nova.feature_wallet_api.domain.interfaces.OutcomingDirection
import io.novafoundation.nova.feature_wallet_api.domain.interfaces.WalletRepository
import io.novafoundation.nova.feature_wallet_api.domain.model.CrossChainTransferFee
import io.novafoundation.nova.feature_wallet_api.domain.model.xcm.CrossChainTransferConfiguration
import io.novafoundation.nova.feature_wallet_api.domain.model.xcm.CrossChainTransfersConfiguration
import io.novafoundation.nova.feature_wallet_api.domain.model.xcm.availableInDestinations
import io.novafoundation.nova.feature_wallet_api.domain.model.xcm.availableOutDestinations
import io.novafoundation.nova.feature_wallet_api.domain.model.xcm.transferConfiguration
import io.novafoundation.nova.runtime.ext.commissionAsset
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.ChainWithAsset
import io.novafoundation.nova.runtime.multiNetwork.assets
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.chainsById
import io.novafoundation.nova.runtime.repository.ParachainInfoRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.coroutines.coroutineContext
import kotlin.time.Duration

private const val INCOMING_DIRECTIONS = "RealCrossChainTransfersUseCase.INCOMING_DIRECTIONS"
private const val CONFIGURATION_CACHE = "RealCrossChainTransfersUseCase.CONFIGURATION"

internal class RealCrossChainTransfersUseCase(
    private val crossChainTransfersRepository: CrossChainTransfersRepository,
    private val walletRepository: WalletRepository,
    private val chainRegistry: ChainRegistry,
    private val accountRepository: AccountRepository,
    private val computationalCache: ComputationalCache,
    private val crossChainWeigher: CrossChainWeigher,
    private val crossChainTransactor: CrossChainTransactor,
    private val parachainInfoRepository: ParachainInfoRepository,
) : CrossChainTransfersUseCase {

    override suspend fun syncCrossChainConfig() {
        crossChainTransfersRepository.syncConfiguration()
    }

    override fun incomingCrossChainDirections(destination: Flow<Chain.Asset?>): Flow<List<IncomingDirection>> {
        return withFlowScope { scope ->
            computationalCache.useSharedFlow(INCOMING_DIRECTIONS, scope) {
                scope.launch { crossChainTransfersRepository.syncConfiguration() }

                combineToPair(destination, cachedConfigurationFlow(scope)).flatMapLatest { (destinationAsset, crossChainConfig) ->
                    if (destinationAsset == null) return@flatMapLatest flowOf(emptyList())

                    val selectedMetaAccountId = accountRepository.getSelectedMetaAccount().id
                    val availableDirections = crossChainConfig.availableInDestinations(destinationAsset)

                    val chains = chainRegistry.chainsById()
                    val availableDirectionChainAssets = chains.assets(availableDirections)

                    walletRepository.assetsFlow(selectedMetaAccountId, availableDirectionChainAssets).map { balances ->
                        balances
                            .filter { it.transferable.isPositive }
                            .map { IncomingDirection(it, chains.getValue(it.token.configuration.chainId)) }
                    }
                }
            }
        }.catch { emit(emptyList()) }
    }

    override fun outcomingCrossChainDirectionsFlow(origin: Chain.Asset): Flow<List<OutcomingDirection>> {
        return withFlowScope { scope ->
            scope.launch { crossChainTransfersRepository.syncConfiguration() }

            crossChainTransfersRepository.configurationFlow().map { configuration ->
                val chainsById = chainRegistry.chainsById.first()

                configuration.availableOutDestinations(origin).mapNotNull { (chainId, assetId) ->
                    val chain = chainsById[chainId] ?: return@mapNotNull null
                    val asset = chain.assetsById[assetId] ?: return@mapNotNull null

                    ChainWithAsset(chain, asset)
                }
            }
        }.catch { emit(emptyList()) }
    }

    override suspend fun getConfiguration(): CrossChainTransfersConfiguration {
        return crossChainTransfersRepository.getConfiguration()
    }


    override suspend fun requiredRemainingAmountAfterTransfer(
        originChain: Chain,
        sendingAsset: Chain.Asset,
        destinationChain: Chain,
    ): Balance {
        val xcmConfig = cachedConfigurationFlow(CoroutineScope(coroutineContext)).first()
        val transferConfig = xcmConfig.transferConfiguration(
            originChain = parachainInfoRepository.getXcmChain(originChain),
            originAsset = sendingAsset,
            destinationChain = parachainInfoRepository.getXcmChain(destinationChain),
        )!!

        return crossChainTransactor.requiredRemainingAmountAfterTransfer(transferConfig)
    }

    override suspend fun ExtrinsicService.estimateFee(
        transfer: AssetTransferBase,
        cachingScope: CoroutineScope?
    ): CrossChainTransferFee = withContext(Dispatchers.IO) {
        val configuration = cachedConfigurationFlow(cachingScope).first()
        val transferConfiguration = configuration.transferConfiguration(
            originChain = parachainInfoRepository.getXcmChain(transfer.originChain),
            originAsset = transfer.originChainAsset,
            destinationChain = parachainInfoRepository.getXcmChain(transfer.destinationChain),
        )!!

        val originFeeAsync = async { crossChainTransactor.estimateOriginFee(transferConfiguration, transfer) }
        val crossChainFeeAsync = async { crossChainWeigher.estimateFee(transfer, transferConfiguration) }

        val originFee = originFeeAsync.await()
        val crossChainFee = crossChainFeeAsync.await()

        CrossChainTransferFee(
            submissionFee = originFee,
            postSubmissionByAccount = crossChainFee.paidByAccountOrNull()?.let {
                val submissionOrigin = SubmissionOrigin.singleOrigin(originFee.submissionOrigin.signingAccount)
                SubstrateFee(it, submissionOrigin, transfer.originChain.commissionAsset)
            },
            postSubmissionFromAmount = SubstrateFeeBase(
                amount = crossChainFee.paidFromHolding,
                asset = transfer.originChainAsset,
            ),
        )
    }

    override suspend fun ExtrinsicService.performTransfer(
        transfer: AssetTransferBase,
        computationalScope: CoroutineScope
    ): Result<Balance> {
        val transferConfiguration = transferConfigurationFor(transfer, computationalScope)
        return crossChainTransactor.performAndTrackTransfer(transferConfiguration, transfer)
    }

    override suspend fun maximumExecutionTime(
        assetTransferDirection: AssetTransferDirection,
        computationalScope: CoroutineScope
    ): Duration {
        val transferConfiguration = transferConfigurationFor(assetTransferDirection, computationalScope)
        return crossChainTransactor.estimateMaximumExecutionTime(transferConfiguration)
    }

    private suspend fun transferConfigurationFor(
        transfer: AssetTransferDirection,
        computationalScope: CoroutineScope
    ): CrossChainTransferConfiguration {
        val configuration = cachedConfigurationFlow(computationalScope).first()

        return configuration.transferConfiguration(
            originChain = parachainInfoRepository.getXcmChain(transfer.originChain),
            originAsset = transfer.originChainAsset,
            destinationChain = parachainInfoRepository.getXcmChain(transfer.destinationChain),
        )!!
    }

    private fun cachedConfigurationFlow(cachingScope: CoroutineScope?): Flow<CrossChainTransfersConfiguration> {
        if (cachingScope == null) {
            return crossChainTransfersRepository.configurationFlow()
        }

        return computationalCache.useSharedFlow(CONFIGURATION_CACHE, cachingScope) {
            crossChainTransfersRepository.configurationFlow()
        }
    }
}
