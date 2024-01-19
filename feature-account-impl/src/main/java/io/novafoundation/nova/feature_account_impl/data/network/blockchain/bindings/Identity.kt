package io.novafoundation.nova.feature_account_impl.data.network.blockchain.bindings

import io.novafoundation.nova.common.data.network.runtime.binding.HelperBinding
import io.novafoundation.nova.common.data.network.runtime.binding.UseCaseBinding
import io.novafoundation.nova.common.data.network.runtime.binding.bindData
import io.novafoundation.nova.common.data.network.runtime.binding.cast
import io.novafoundation.nova.common.data.network.runtime.binding.castToList
import io.novafoundation.nova.common.data.network.runtime.binding.castToStruct
import io.novafoundation.nova.common.data.network.runtime.binding.incompatible
import io.novafoundation.nova.common.utils.second
import io.novafoundation.nova.feature_account_api.data.model.OnChainIdentity
import io.novafoundation.nova.feature_account_api.data.model.RootIdentity
import io.novafoundation.nova.feature_account_api.data.model.SuperOf
import jp.co.soramitsu.fearless_utils.extensions.toHexString
import jp.co.soramitsu.fearless_utils.runtime.definitions.types.composite.Struct

@UseCaseBinding
fun bindIdentity(dynamic: Any?): OnChainIdentity? {
    if (dynamic == null) return null

    val decoded = dynamic.castToStruct()

    val identityInfo = decoded.get<Struct.Instance>("registration")
        ?.get<Struct.Instance>("info")
        ?: decoded.get<Struct.Instance>("info") ?: incompatible()

    val pgpFingerprint = identityInfo.get<ByteArray?>("pgpFingerprint")

    val matrix = bindIdentityData(identityInfo, "riot", onIncompatibleField = null)
        ?: bindIdentityData(identityInfo, "matrix", onIncompatibleField = null)

    return RootIdentity(
        display = bindIdentityData(identityInfo, "display"),
        legal = bindIdentityData(identityInfo, "legal"),
        web = bindIdentityData(identityInfo, "web"),
        matrix = matrix,
        email = bindIdentityData(identityInfo, "email"),
        pgpFingerprint = pgpFingerprint?.toHexString(withPrefix = true),
        image = bindIdentityData(identityInfo, "image"),
        twitter = bindIdentityData(identityInfo, "twitter")
    )
}

@UseCaseBinding
fun bindSuperOf(decoded: Any?): SuperOf? {
    if (decoded == null) return null

    val asList = decoded.castToList()

    val parentId: ByteArray = asList.first().cast()

    return SuperOf(
        parentId = parentId,
        childName = bindData(asList.second()).asString()
    )
}

@HelperBinding
fun bindIdentityData(
    identityInfo: Struct.Instance,
    field: String,
    onIncompatibleField: (() -> Unit)? = { incompatible() }
): String? {
    val value = identityInfo.get<Any?>(field)
        ?: onIncompatibleField?.invoke()
        ?: return null

    return bindData(value).asString()
}
