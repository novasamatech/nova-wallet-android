package io.novafoundation.nova.feature_ledger_impl.sdk.application.substrate

import jp.co.soramitsu.fearless_utils.encrypt.junction.Junction

fun List<Junction>.encodeToByteArray(): ByteArray = fold(ByteArray(0)) { acc, junction ->
    acc + junction.chaincode
}
