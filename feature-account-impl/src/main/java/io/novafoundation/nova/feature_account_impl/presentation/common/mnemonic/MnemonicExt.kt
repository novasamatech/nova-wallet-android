package io.novafoundation.nova.feature_account_impl.presentation.common.mnemonic

import io.novasama.substrate_sdk_android.encrypt.mnemonic.Mnemonic

fun Mnemonic.spacedWords(spacing: Int = 2) = wordList.joinToString(separator = " ".repeat(spacing))
