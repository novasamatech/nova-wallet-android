package io.novafoundation.nova.feature_governance_api.data.network.blockhain.model

import jp.co.soramitsu.fearless_utils.runtime.definitions.types.generics.GenericCall

class PreImage(val encodedCall: ByteArray, val call: GenericCall.Instance)
