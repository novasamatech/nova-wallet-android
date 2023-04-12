package io.novafoundation.nova.caip.di

import com.google.gson.Gson
import io.novafoundation.nova.common.data.network.NetworkApiCreator

interface CaipDependencies {

    val networkApiCreator: NetworkApiCreator

    val gson: Gson
}
