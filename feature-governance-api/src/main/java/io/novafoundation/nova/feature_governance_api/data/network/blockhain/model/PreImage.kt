package io.novafoundation.nova.feature_governance_api.data.network.blockhain.model

import io.novasama.substrate_sdk_android.runtime.definitions.types.generics.GenericCall

class PreImage(val encodedCall: ByteArray, val call: GenericCall.Instance)
