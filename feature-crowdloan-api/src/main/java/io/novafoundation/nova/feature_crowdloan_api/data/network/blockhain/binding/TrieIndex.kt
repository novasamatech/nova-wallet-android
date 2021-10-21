package io.novafoundation.nova.feature_crowdloan_api.data.network.blockhain.binding

import io.novafoundation.nova.common.data.network.runtime.binding.HelperBinding
import io.novafoundation.nova.common.data.network.runtime.binding.bindNumber
import java.math.BigInteger

typealias TrieIndex = BigInteger

@HelperBinding
fun bindTrieIndex(dynamicInstance: Any?) = bindNumber(dynamicInstance)
