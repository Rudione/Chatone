package io.rudione.chatone.util.network

import java.net.NetworkInterface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.isActive

internal actual fun observeNetworkStatus(): Flow<NetworkStatus> = flow {
    var lastCheck = System.currentTimeMillis()
    var wakeVersion = 0L
    while (currentCoroutineContext().isActive) {
        val now = System.currentTimeMillis()
        if (now - lastCheck > 15_000) wakeVersion++
        lastCheck = now
        val route = runCatching {
            NetworkInterface.getNetworkInterfaces()?.toList().orEmpty()
                .filter { it.isUp && !it.isLoopback }
                .flatMap { network ->
                    network.inetAddresses.toList().filterNot { it.isLoopbackAddress || it.isLinkLocalAddress }
                        .map { "${network.name}:${it.hostAddress}" }
                }.sorted().joinToString("|")
        }.getOrNull()
        emit(NetworkStatus(available = route?.isNotEmpty(), route = route.orEmpty(), wakeVersion = wakeVersion))
        delay(5_000)
    }
}.distinctUntilChanged().flowOn(Dispatchers.IO)
