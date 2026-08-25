package com.hermes.wearos.domain.usecases

import com.hermes.wearos.core.utils.Result
import com.hermes.wearos.data.repository.ChatRepository
import javax.inject.Inject

class SendMessageUseCase @Inject constructor(
    private val chatRepository: ChatRepository
) {
    suspend operator fun invoke(message: String): Result<String> {
        return chatRepository.sendMessage(message)
    }
}

class GetRecentChatsUseCase @Inject constructor(
    private val chatRepository: ChatRepository
) {
    operator fun invoke(limit: Int = 10) = chatRepository.getRecentMessagesSync(limit)
}

class ClearChatHistoryUseCase @Inject constructor(
    private val chatRepository: ChatRepository
) {
    suspend operator fun invoke() {
        chatRepository.clearHistory()
    }
}
