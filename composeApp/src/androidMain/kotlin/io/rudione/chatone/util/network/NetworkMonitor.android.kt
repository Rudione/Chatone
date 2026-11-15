package io.rudione.chatone.util.network

import android.app.Application
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import org.koin.mp.KoinPlatform
import android.net.LinkProperties

internal actual fun observeNetworkStatus(): Flow<NetworkStatus> = callbackFlow {
    val application = KoinPlatform.getKoin().get<Application>()
    val manager = application.getSystemService(ConnectivityManager::class.java)
    fun publish() {
        val network = manager.activeNetwork
        val capabilities = network?.let(manager::getNetworkCapabilities)
        trySend(NetworkStatus(
            available = capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true,
            route = network?.toString().orEmpty()
        ))
    }
    val callback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) = publish()
        override fun onLost(network: Network) = publish()
        override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) = publish()
        override fun onLinkPropertiesChanged(network: Network, linkProperties: LinkProperties) = publish()
    }
    val registered = runCatching { manager.registerDefaultNetworkCallback(callback) }.isSuccess
    if (registered) publish() else trySend(NetworkStatus())
    awaitClose { if (registered) manager.unregisterNetworkCallback(callback) }
}.distinctUntilChanged().withAppResumes()
