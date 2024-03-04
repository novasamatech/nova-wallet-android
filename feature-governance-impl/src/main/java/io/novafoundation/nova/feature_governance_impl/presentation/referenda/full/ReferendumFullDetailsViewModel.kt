package io.novafoundation.nova.feature_governance_impl.presentation.referenda.full

import io.novafoundation.nova.common.address.AddressIconGenerator
import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.utils.flowOf
import io.novafoundation.nova.common.utils.withLoading
import io.novafoundation.nova.feature_account_api.presenatation.account.icon.createIdentityAddressModel
import io.novafoundation.nova.feature_account_api.presenatation.actions.ExternalActions
import io.novafoundation.nova.feature_governance_api.domain.referendum.common.ReferendumProposer
import io.novafoundation.nova.feature_governance_api.presentation.referenda.full.PreImagePreviewPayload
import io.novafoundation.nova.feature_governance_api.presentation.referenda.full.ReferendumCallPayload
import io.novafoundation.nova.feature_governance_api.presentation.referenda.full.ReferendumFullDetailsPayload
import io.novafoundation.nova.feature_governance_api.presentation.referenda.full.ReferendumProposerPayload
import io.novafoundation.nova.feature_governance_impl.data.GovernanceSharedState
import io.novafoundation.nova.feature_governance_impl.domain.identity.GovernanceIdentityProviderFactory
import io.novafoundation.nova.feature_governance_impl.presentation.GovernanceRouter
import io.novafoundation.nova.feature_governance_impl.presentation.referenda.full.model.AddressAndAmountModel
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import io.novafoundation.nova.feature_wallet_api.domain.TokenUseCase
import io.novafoundation.nova.feature_wallet_api.domain.model.Token
import io.novafoundation.nova.feature_wallet_api.presentation.model.mapAmountToAmountModel
import io.novafoundation.nova.runtime.ext.addressOf
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.state.chain
import jp.co.soramitsu.fearless_utils.extensions.toHexString
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class ReferendumFullDetailsViewModel(
    private val router: GovernanceRouter,
    private val payload: ReferendumFullDetailsPayload,
    private val identityProviderFactory: GovernanceIdentityProviderFactory,
    private val addressIconGenerator: AddressIconGenerator,
    private val governanceSharedState: GovernanceSharedState,
    private val tokenUseCase: TokenUseCase,
    private val externalActions: ExternalActions.Presentation,
) : BaseViewModel(), ExternalActions by externalActions {

    private val payloadFlow = flowOf { payload }

    private val referendumProposerFlow = payloadFlow.map {
        payload.proposer?.let { ReferendumProposer(it.accountId, it.offChainName) }
    }

    val proposerIdentityProvider = identityProviderFactory.proposerProvider(referendumProposerFlow)
    val defaultIdentityProvider = identityProviderFactory.defaultProvider()

    val proposerModel = payloadFlow
        .map { createProposerAddressModel(it.proposer, it.deposit) }
        .withLoading()
        .shareInBackground()

    val beneficiaryModel = payloadFlow
        .map { createBeneficiaryAddressModel(it.referendumCall) }
        .withLoading()
        .shareInBackground()

    val hasPreimage = payload.preImage != null
    val isPreimageTooLong = payload.preImage is PreImagePreviewPayload.TooLong
    val isPreviewAvailable = payload.preImage is PreImagePreviewPayload.Preview

    val voteThreshold = payload.voteThreshold
    val approveThreshold = payload.approveThreshold
    val supportThreshold = payload.supportThreshold
    val preImage = mapPreimage(payload.preImage)
    val callHash = payload.hash?.toHexString()

    val turnoutAmount = payloadFlow
        .map { payload -> payload.turnout?.let { mapAmountToAmountModel(it, getToken()) } }
        .shareInBackground()

    val electorateAmount = payloadFlow
        .map { payload -> payload.electorate?.let { mapAmountToAmountModel(it, getToken()) } }
        .shareInBackground()

    fun backClicked() {
        router.back()
    }

    fun openProposal() {
        payload.proposer?.let {
            openAddressInfo(it.accountId)
        }
    }

    fun openBeneficiary() {
        payload.referendumCall?.let {
            if (it !is ReferendumCallPayload.TreasuryRequest) return
            openAddressInfo(it.beneficiary)
        }
    }

    private suspend fun createProposerAddressModel(referendumProposer: ReferendumProposerPayload?, deposit: Balance?): AddressAndAmountModel? {
        if (referendumProposer == null) return null

        val addressModel = addressIconGenerator.createIdentityAddressModel(
            getChain(),
            referendumProposer.accountId,
            proposerIdentityProvider
        )

        val amountModel = deposit?.let { mapAmountToAmountModel(deposit, getToken()) }

        return AddressAndAmountModel(addressModel, amountModel)
    }

    private suspend fun createBeneficiaryAddressModel(referendumCall: ReferendumCallPayload?): AddressAndAmountModel? {
        if (referendumCall == null || referendumCall !is ReferendumCallPayload.TreasuryRequest) return null

        val addressModel = addressIconGenerator.createIdentityAddressModel(
            getChain(),
            referendumCall.beneficiary,
            defaultIdentityProvider
        )
        val amountModel = mapAmountToAmountModel(referendumCall.amount, getToken())

        return AddressAndAmountModel(addressModel, amountModel)
    }

    private suspend fun getChain(): Chain {
        return governanceSharedState.chain()
    }

    private suspend fun getToken(): Token {
        return tokenUseCase.currentToken()
    }

    private fun mapPreimage(preImage: PreImagePreviewPayload?): String? {
        return when (preImage) {
            is PreImagePreviewPayload.Preview -> preImage.preview
            else -> null
        }
    }

    private fun openAddressInfo(accountId: ByteArray) = launch {
        val chain = getChain()
        val address = chain.addressOf(accountId)
        val type = ExternalActions.Type.Address(address)
        externalActions.showExternalActions(type, chain)
    }
}
