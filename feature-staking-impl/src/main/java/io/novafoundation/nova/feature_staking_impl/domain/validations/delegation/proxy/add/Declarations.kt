package io.novafoundation.nova.feature_staking_impl.domain.validations.delegation.proxy.add

import io.novafoundation.nova.common.validation.ValidationSystem
import io.novafoundation.nova.common.validation.ValidationSystemBuilder
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_account_api.domain.validation.notSelfAccount
import io.novafoundation.nova.feature_proxy_api.data.repository.GetProxyRepository
import io.novafoundation.nova.feature_proxy_api.domain.model.ProxyType
import io.novafoundation.nova.feature_proxy_api.domain.validators.maximumProxiesNotReached
import io.novafoundation.nova.feature_proxy_api.domain.validators.proxyIsNotDuplicationForAccount
import io.novafoundation.nova.feature_wallet_api.domain.model.amountFromPlanks
import io.novafoundation.nova.feature_wallet_api.domain.model.balanceCountedTowardsED
import io.novafoundation.nova.feature_wallet_api.domain.model.planksFromAmount
import io.novafoundation.nova.feature_wallet_api.domain.model.regulatTransferableBalance
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
                maxUsable = context.maxUsable,
                fee = context.fee
            )
        }
    )

fun AddStakingProxyValidationSystemBuilder.maximumProxies(
    proxyRepository: GetProxyRepository
) = maximumProxiesNotReached(
    chain = { it.chain },
    accountId = { it.proxiedAccountId },
    proxiesQuantity = { it.currentQuantity + 1 },
    error = { payload, max ->
        AddStakingProxyValidationFailure.MaximumProxiesReached(
            chain = payload.chain,
            max = max
        )
    },
    proxyRepository = proxyRepository
)

fun AddStakingProxyValidationSystemBuilder.enoughBalanceToPayDepositAndFee() = sufficientBalance(
    fee = { it.fee },
    amount = { it.asset.token.configuration.amountFromPlanks(it.deltaDeposit) },
    available = { it.asset.token.amountFromPlanks(it.asset.regulatTransferableBalance()) },
    error = {
        val chainAsset = it.payload.asset.token.configuration
        AddStakingProxyValidationFailure.NotEnoughBalanceToReserveDeposit(
            chainAsset = chainAsset,
            availableBalance = chainAsset.planksFromAmount(it.maxUsable),
            deposit = it.payload.deltaDeposit
        )
    }
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

fun AddStakingProxyValidationSystemBuilder.notSelfAccount(
    accountRepository: AccountRepository
) = notSelfAccount(
    chainProvider = { it.chain },
    accountIdProvider = { it.chain.accountIdOf(it.proxyAddress) },
    failure = { AddStakingProxyValidationFailure.SelfDelegation },
    accountRepository = accountRepository
)
