package io.rudione.chatone.domain.usecase

import io.rudione.chatone.data.repository.ChatRepository
import io.rudione.chatone.domain.model.Channel
import io.rudione.chatone.domain.model.TwitchAccount
import io.rudione.chatone.util.Result

class ConnectChatUseCase(
    private val chatRepository: ChatRepository
) {
    suspend operator fun invoke(account: TwitchAccount) {
        chatRepository.connect(account)
    }
}

class DisconnectChatUseCase(
    private val chatRepository: ChatRepository
) {
    suspend operator fun invoke() {
        chatRepository.disconnect()
    }
}

class JoinChannelUseCase(
    private val chatRepository: ChatRepository
) {
    suspend operator fun invoke(channelName: String) {
        chatRepository.joinChannel(channelName)
    }
}

class PartChannelUseCase(
    private val chatRepository: ChatRepository
) {
    suspend operator fun invoke(channelName: String) {
        chatRepository.partChannel(channelName)
    }
}

class SendMessageUseCase(
    private val chatRepository: ChatRepository
) {
    suspend operator fun invoke(channelName: String, message: String) {
        chatRepository.sendMessage(channelName, message)
    }
}

class SearchChannelsUseCase(
    private val chatRepository: ChatRepository
) {
    suspend operator fun invoke(query: String, accessToken: String): Result<List<Channel>> {
        return chatRepository.searchChannels(query, accessToken)
    }
}

class GetChannelInfoUseCase(
    private val chatRepository: ChatRepository
) {
    suspend operator fun invoke(channelLogin: String, accessToken: String): Result<Channel?> {
        return chatRepository.getChannelInfo(channelLogin, accessToken)
    }
}
