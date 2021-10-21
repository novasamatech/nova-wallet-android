package io.novafoundation.nova.feature_staking_impl.data.network.blockhain.bindings

import io.novafoundation.nova.common.data.network.runtime.binding.UseCaseBinding
import io.novafoundation.nova.common.data.network.runtime.binding.bindNumberConstant
import jp.co.soramitsu.fearless_utils.runtime.RuntimeSnapshot
import jp.co.soramitsu.fearless_utils.runtime.metadata.Constant
import java.math.BigInteger

/*
SlashDeferDuration = EraIndex
 */
@UseCaseBinding
fun bindSlashDeferDuration(
    constant: Constant,
    runtime: RuntimeSnapshot
): BigInteger = bindNumberConstant(constant, runtime)

@UseCaseBinding
fun bindMaximumRewardedNominators(
    constant: Constant,
    runtime: RuntimeSnapshot
): BigInteger = bindNumberConstant(constant, runtime)
