package io.novafoundation.nova.feature_staking_impl.domain.validations.delegation.proxy.add

import io.novafoundation.nova.common.validation.ValidationSystem
import io.novafoundation.nova.common.validation.ValidationSystemBuilder
import io.novafoundation.nova.feature_proxy_api.data.repository.GetProxyRepository
import io.novafoundation.nova.feature_proxy_api.domain.model.ProxyType
import io.novafoundation.nova.feature_proxy_api.domain.validators.enoughBalanceToPayProxyDeposit
import io.novafoundation.nova.feature_proxy_api.domain.validators.maximumProxiesNotReached
import io.novafoundation.nova.feature_proxy_api.domain.validators.proxyIsNotDuplicationForAccount
import io.novafoundation.nova.feature_wallet_api.domain.model.balanceCountedTowardsED
import io.novafoundation.nova.feature_wallet_api.domain.validation.EnoughTotalToStayAboveEDValidationFactory
import io.novafoundation.nova.feature_wallet_api.domain.validation.sufficientBalance
import io.novafoundation.nova.feature_wallet_api.domain.validation.validAddress
import io.novafoundation.nova.feature_wallet_api.domain.validation.validate
import io.novafoundation.nova.runtime.ext.accountIdOf
import io.novafoundation.nova.runtime.multiNetwork.ChainWithAsset
import java.math.BigDecimal

typealias AddStakingProxyValidationSystem = ValidationSystem<AddStakingProxyValidationPayload, AddStakingProxyValidationFailure>
typealias AddStakingProxyValidationSystemBuilder = ValidationSystemBuilder<AddStakingProxyValidationPayload, AddStakingProxyValidationFailure>

fun AddStakingProxyValidationSystemBuilder.validAddress() = validAddress(
    address = { it.proxyAddress },
    chain = { it.chain },
    error = { AddStakingProxyValidationFailure.InvalidAddress(it.chain) }
)

fun AddStakingProxyValidationSystemBuilder.sufficientBalanceToStayAboveEd(
    enoughTotalToStayAboveEDValidationFactory: EnoughTotalToStayAboveEDValidationFactory
) = enoughTotalToStayAboveEDValidationFactory.validate(
    chainWithAsset = { ChainWithAsset(it.chain, it.asset.token.configuration) },
    balance = { it.asset.balanceCountedTowardsED() },
    fee = { it.fee },
    error = { payload, _ ->
        AddStakingProxyValidationFailure.NotEnoughToStayAboveED(
            asset = payload.asset.token.configuration
        )
    }
)

fun AddStakingProxyValidationSystemBuilder.sufficientBalanceToPayFee() =
    sufficientBalance(
        available = { it.asset.free },
        amount = { BigDecimal.ZERO },
        fee = { it.fee },
        error = { context ->
            AddStakingProxyValidationFailure.NotEnoughToPayFee(
                chainAsset = context.payload.asset.token.configuration,
                maxUsable = context.availableToPayFees,
                fee = context.fee
            )
        }
    )

fun AddStakingProxyValidationSystemBuilder.maximumProxies(
    proxyRepository: GetProxyRepository
) = maximumProxiesNotReached(
    chain = { it.chain },
    accountId = { it.proxiedAccountId },
    newProxiedQuantity = { it.depositWithQuantity.quantity },
    error = { payload, max ->
        AddStakingProxyValidationFailure.MaximumProxiesReached(
            chain = payload.chain,
            max = max
        )
    },
    proxyRepository = proxyRepository
)

fun AddStakingProxyValidationSystemBuilder.enoughBalanceToPayDeposit(
    proxyRepository: GetProxyRepository
) = enoughBalanceToPayProxyDeposit(
    chain = { it.chain },
    accountId = { it.proxiedAccountId },
    newDeposit = { it.depositWithQuantity.deposit },
    availableBalance = { it.asset.freeInPlanks - it.asset.frozenInPlanks },
    error = { payload, maxUsable ->
        AddStakingProxyValidationFailure.NotEnoughBalanceToReserveDeposit(
            chainAsset = payload.asset.token.configuration,
            availableBalance = maxUsable,
            deposit = payload.depositWithQuantity.deposit
        )
    },
    proxyRepository = proxyRepository,
    feeReceiver = { it.fee.networkFee.amount }
)

fun AddStakingProxyValidationSystemBuilder.stakingTypeIsNotDuplication(
    proxyRepository: GetProxyRepository
) = proxyIsNotDuplicationForAccount(
    chain = { it.chain },
    proxiedAccountId = { it.proxiedAccountId },
    proxyAccountId = { it.chain.accountIdOf(it.proxyAddress) },
    proxyType = { ProxyType.Staking },
    error = { payload -> AddStakingProxyValidationFailure.AlreadyDelegated(payload.proxyAddress) },
    proxyRepository = proxyRepository
)
