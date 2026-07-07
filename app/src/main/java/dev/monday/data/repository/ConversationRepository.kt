package dev.monday.data.repository

import dev.monday.data.database.dao.ConversationDao
import dev.monday.data.database.dao.ConversationMessageDao
import dev.monday.data.database.entity.ConversationEntity
import dev.monday.data.database.entity.ConversationMessageEntity
import dev.monday.core.model.MessageRole
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ConversationRepository @Inject constructor(
    private val conversationDao: ConversationDao,
    private val messageDao: ConversationMessageDao
) {
    fun getAllConversations(): Flow<List<ConversationEntity>> =
        conversationDao.getAll()

    fun getPinned(): Flow<List<ConversationEntity>> =
        conversationDao.getPinned()

    fun getBookmarked(): Flow<List<ConversationEntity>> =
        conversationDao.getBookmarked()

    suspend fun getConversation(id: Long): ConversationEntity? =
        conversationDao.getById(id)

    fun getMessages(conversationId: Long): Flow<List<ConversationMessageEntity>> =
        messageDao.getMessages(conversationId)

    suspend fun createConversation(title: String? = null): Long =
        conversationDao.insert(ConversationEntity(title = title))

    suspend fun addMessage(conversationId: Long, role: MessageRole, content: String): Long {
        val messageId = messageDao.insert(
            ConversationMessageEntity(
                conversationId = conversationId,
                role = role.name,
                content = content
            )
        )
        // Update conversation timestamp
        conversationDao.getById(conversationId)?.let {
            conversationDao.update(it.copy(updatedAt = System.currentTimeMillis()))
        }
        return messageId
    }

    suspend fun setPinned(conversationId: Long, pinned: Boolean) =
        conversationDao.setPinned(conversationId, pinned)

    suspend fun setBookmarked(conversationId: Long, bookmarked: Boolean) =
        conversationDao.setBookmarked(conversationId, bookmarked)

    suspend fun searchMessages(query: String, limit: Int = 50): List<ConversationMessageEntity> =
        messageDao.search(query, limit)

    suspend fun deleteConversation(conversationId: Long) {
        messageDao.deleteForConversation(conversationId)
        conversationDao.getById(conversationId)?.let {
            conversationDao.delete(it)
        }
    }
}
