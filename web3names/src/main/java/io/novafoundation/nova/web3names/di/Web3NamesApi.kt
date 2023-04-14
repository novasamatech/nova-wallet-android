package io.novafoundation.nova.web3names.di

import io.novafoundation.nova.web3names.domain.networking.Web3NamesInteractor

interface Web3NamesApi {

    val web3NamesInteractor: Web3NamesInteractor
}
