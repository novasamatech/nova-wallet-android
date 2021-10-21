package io.novafoundation.nova.feature_staking_impl.data.network.blockhain.bindings

import io.novafoundation.nova.common.data.network.runtime.binding.HelperBinding
import io.novafoundation.nova.common.data.network.runtime.binding.UseCaseBinding
import io.novafoundation.nova.common.data.network.runtime.binding.incompatible
import io.novafoundation.nova.common.data.network.runtime.binding.requireType
import io.novafoundation.nova.feature_staking_api.domain.model.Exposure
import io.novafoundation.nova.feature_staking_api.domain.model.IndividualExposure
import jp.co.soramitsu.fearless_utils.runtime.RuntimeSnapshot
import jp.co.soramitsu.fearless_utils.runtime.definitions.types.composite.Struct
import jp.co.soramitsu.fearless_utils.runtime.definitions.types.fromHexOrNull
import java.math.BigInteger

/*
IndividualExposure: {
  who: AccountId; // account id of the nominator
  value: Compact<Balance>; // nominator’s stake
}
 */
@HelperBinding
fun bindIndividualExposure(dynamicInstance: Any?, runtime: RuntimeSnapshot): IndividualExposure {
    requireType<Struct.Instance>(dynamicInstance)

    val who = dynamicInstance.get<ByteArray>("who") ?: incompatible()
    val value = dynamicInstance.get<BigInteger>("value") ?: incompatible()

    return IndividualExposure(who, value)
}

/*
 Exposure: {
  total: Compact<Balance>; // total stake of the validator
  own: Compact<Balance>; // own stake of the validator
  others: Vec<IndividualExposure>; // nominators stakes
}
 */
@UseCaseBinding
fun bindExposure(scale: String, runtime: RuntimeSnapshot): Exposure {
    val type = runtime.typeRegistry["Exposure"] ?: incompatible()
    val decoded = type.fromHexOrNull(runtime, scale) as? Struct.Instance ?: incompatible()

    val total = decoded.get<BigInteger>("total") ?: incompatible()
    val own = decoded.get<BigInteger>("own") ?: incompatible()

    val others = decoded.get<List<*>>("others")?.map { bindIndividualExposure(it, runtime) } ?: incompatible()

    return Exposure(total, own, others)
}
