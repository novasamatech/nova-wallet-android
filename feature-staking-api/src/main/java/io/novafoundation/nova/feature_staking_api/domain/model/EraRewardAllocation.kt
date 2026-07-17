package io.novafoundation.nova.feature_staking_api.domain.model

import io.emeraldpay.polkaj.scale.ScaleCodecReader
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import io.novasama.substrate_sdk_android.hash.Hasher.xxHash128
import io.novasama.substrate_sdk_android.scale.dataType.uint128

/**
 * Per-era reward allocation as recorded by the staking pallet at era end.
 *
 * [stakerRewards] mirrors `Staking.ErasValidatorReward` — the exact amount payouts distribute
 * to stakers (nominators + validators) for the era. [validatorIncentive] is the DAP validator
 * self-stake incentive paid to validator stashes only, so it must not be counted into the
 * nominator-facing return.
 */
class EraRewardAllocation(
    val stakerRewards: Balance,
    val validatorIncentive: Balance,
) {

    companion object {

        /**
         * `Staking::era_reward_allocation(era)` view function id. FRAME view function ids are
         * `twox128(pallet_name) ++ twox128("fn_name(arg_types) -> return_type")` — derived from
         * the method signature the same way storage keys derive from pallet/item names, so the
         * id is identical on every chain running the pallet and only changes if the signature
         * changes.
         */
        val ERA_REWARD_ALLOCATION_VIEW_FUNCTION_ID: ByteArray =
            "Staking".encodeToByteArray().xxHash128() +
                "era_reward_allocation(EraIndex) -> crate::reward::EraRewardAllocation<BalanceOf<T>>"
                    .encodeToByteArray().xxHash128()

        /**
         * Decodes the `Result<Vec<u8>, ViewFunctionDispatchError>` envelope returned by
         * `RuntimeViewFunction_execute_view_function` and then the inner
         * `EraRewardAllocation { staker_rewards: u128, validator_incentive: u128 }`.
         * Static coding is used deliberately: view function ids and types ship with metadata
         * v16, which is above the version the app requests.
         */
        fun fromStateCallResult(result: ByteArray): EraRewardAllocation {
            val envelopeReader = ScaleCodecReader(result)

            val resultVariant = envelopeReader.readUByte()
            require(resultVariant == 0) { "View function dispatch failed with variant $resultVariant" }

            val payloadReader = ScaleCodecReader(envelopeReader.readByteArray())

            return EraRewardAllocation(
                stakerRewards = uint128.read(payloadReader),
                validatorIncentive = uint128.read(payloadReader),
            )
        }
    }
}
