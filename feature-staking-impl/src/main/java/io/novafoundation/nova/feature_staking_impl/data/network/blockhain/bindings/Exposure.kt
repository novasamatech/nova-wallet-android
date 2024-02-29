package io.novafoundation.nova.feature_staking_impl.data.network.blockhain.bindings

import io.novafoundation.nova.common.data.network.runtime.binding.HelperBinding
import io.novafoundation.nova.common.data.network.runtime.binding.UseCaseBinding
import io.novafoundation.nova.common.data.network.runtime.binding.bindList
import io.novafoundation.nova.common.data.network.runtime.binding.bindNumber
import io.novafoundation.nova.common.data.network.runtime.binding.castToStruct
import io.novafoundation.nova.common.data.network.runtime.binding.incompatible
import io.novafoundation.nova.common.data.network.runtime.binding.requireType
import io.novafoundation.nova.feature_staking_api.domain.model.Exposure
import io.novafoundation.nova.feature_staking_api.domain.model.ExposureOverview
import io.novafoundation.nova.feature_staking_api.domain.model.ExposurePage
import io.novafoundation.nova.feature_staking_api.domain.model.IndividualExposure
import io.novasama.substrate_sdk_android.runtime.definitions.types.composite.Struct
import java.math.BigInteger

/*
IndividualExposure: {
  who: AccountId; // account id of the nominator
  value: Compact<Balance>; // nominator’s stake
}
 */
@HelperBinding
private fun bindIndividualExposure(dynamicInstance: Any?): IndividualExposure {
    requireType<Struct.Instance>(dynamicInstance)

    val who = dynamicInstance.get<ByteArray>("who") ?: incompatible()
    val value = dynamicInstance.get<BigInteger>("value") ?: incompatible()

    return IndividualExposure(who, value)
}

@UseCaseBinding
fun bindExposure(instance: Any?): Exposure {
    val decoded = instance.castToStruct()

    val total = decoded.get<BigInteger>("total") ?: incompatible()
    val own = decoded.get<BigInteger>("own") ?: incompatible()

    val others = decoded.get<List<*>>("others")?.map { bindIndividualExposure(it) } ?: incompatible()

    return Exposure(total, own, others)
}

@UseCaseBinding
fun bindExposureOverview(instance: Any?): ExposureOverview {
    val decoded = instance.castToStruct()

    return ExposureOverview(
        total = bindNumber(decoded["total"]),
        own = bindNumber(decoded["own"]),
        nominatorCount = bindNumber(decoded["nominatorCount"]),
        pageCount = bindNumber(decoded["pageCount"])
    )
}

@UseCaseBinding
fun bindExposurePage(instance: Any?): ExposurePage {
    val decoded = instance.castToStruct()

    return ExposurePage(
        others = bindList(decoded["others"], ::bindIndividualExposure)
    )
}
