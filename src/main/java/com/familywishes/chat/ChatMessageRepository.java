package com.familywishes.chat;

import static com.familywishes.chat.ChatDtos.*;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class ChatMessageRepository {

  public record MessageMeta(Long messageId, Long conversationId, Long senderId, LocalDateTime sentAt) {}
  public record ConversationSummary(
      Long conversationId,
      Long otherUserId,
      String otherUserName,
      String otherUserEmail,
      boolean otherUserOnline,
      LocalDateTime otherUserLastSeenAt,
      String messageText,
      LocalDateTime sentAt,
      LocalDateTime seenAt,
      long unreadCount) {}

  @Qualifier("supabaseChatJdbc")
  private final NamedParameterJdbcTemplate chatJdbc;

  public Long findConversationId(long userA, long userB) {
    List<Long> ids =
        chatJdbc.query(
            "SELECT id FROM chat_conversations WHERE user_a_id = :a AND user_b_id = :b",
            Map.of("a", userA, "b", userB),
            (rs, rowNum) -> rs.getLong("id"));

    return ids.isEmpty() ? null : ids.get(0);
  }

  public Long createConversation(long userA, long userB, LocalDateTime createdAt) {
    return chatJdbc.queryForObject(
        """
        INSERT INTO chat_conversations(user_a_id, user_b_id, created_at)
        VALUES (:a, :b, :createdAt)
        ON CONFLICT (user_a_id, user_b_id)
        DO UPDATE SET user_a_id = EXCLUDED.user_a_id
        RETURNING id
        """,
        new MapSqlParameterSource()
            .addValue("a", userA)
            .addValue("b", userB)
            .addValue("createdAt", Timestamp.valueOf(createdAt)),
        Long.class);
  }

  public Long insertMessage(
      Long conversationId,
      Long senderId,
      Long receiverId,
      String text,
      String attachmentKey,
      String attachmentFileName,
      String attachmentContentType,
      LocalDateTime sentAt) {
    return chatJdbc.queryForObject(
        """
        INSERT INTO chat_messages(conversation_id, sender_id, receiver_id, message_text, attachment_key,
            attachment_file_name, attachment_content_type, sent_at)
        VALUES (:conversationId, :senderId, :receiverId, :text, :attachmentKey, :attachmentFileName,
            :attachmentContentType, :sentAt)
        RETURNING id
        """,
        new MapSqlParameterSource()
            .addValue("conversationId", conversationId)
            .addValue("senderId", senderId)
            .addValue("receiverId", receiverId)
            .addValue("text", text)
            .addValue("attachmentKey", attachmentKey)
            .addValue("attachmentFileName", attachmentFileName)
            .addValue("attachmentContentType", attachmentContentType)
            .addValue("sentAt", Timestamp.valueOf(sentAt)),
        Long.class);
  }

  public int markConversationSeen(Long conversationId, Long receiverId, LocalDateTime seenAt) {
    return chatJdbc.update(
        """
        UPDATE chat_messages
           SET seen_at = :seenAt
         WHERE conversation_id = :conversationId
           AND receiver_id = :receiverId
           AND seen_at IS NULL
        """,
        new MapSqlParameterSource()
            .addValue("conversationId", conversationId)
            .addValue("receiverId", receiverId)
            .addValue("seenAt", Timestamp.valueOf(seenAt)));
  }

  public List<MessageResponse> findConversationMessages(Long conversationId, int page, int size, Long me) {
    String query =
        """
        SELECT id, conversation_id, sender_id, receiver_id, message_text, attachment_key,
               attachment_file_name, attachment_content_type, sent_at, seen_at
          FROM chat_messages
         WHERE conversation_id = :conversationId
         ORDER BY sent_at DESC
         LIMIT :limit OFFSET :offset
        """;
    return chatJdbc.query(
        query,
        new MapSqlParameterSource()
            .addValue("conversationId", conversationId)
            .addValue("limit", size)
            .addValue("offset", page * size),
        (rs, rowNum) -> {
          Long sender = rs.getLong("sender_id");
          return new MessageResponse(
              rs.getLong("id"),
              rs.getLong("conversation_id"),
              sender,
              rs.getLong("receiver_id"),
              rs.getString("message_text"),
              rs.getString("attachment_key"),
              rs.getString("attachment_file_name"),
              rs.getString("attachment_content_type"),
              rs.getTimestamp("sent_at") == null ? null : rs.getTimestamp("sent_at").toLocalDateTime(),
              rs.getTimestamp("seen_at") == null ? null : rs.getTimestamp("seen_at").toLocalDateTime(),
              sender.equals(me));
        });
  }

  public List<ConversationSummary> findConversationSummaries(Long me) {
    String query =
        """
        SELECT c.id AS conversation_id,
               CASE WHEN c.user_a_id = :me THEN c.user_b_id ELSE c.user_a_id END AS other_user_id,
               m.message_text,
               m.sent_at,
               m.seen_at,
               (
                 SELECT count(1)
                   FROM chat_messages um
                  WHERE um.conversation_id = c.id
                   AND um.receiver_id = :me
                   AND um.seen_at IS NULL
               ) AS unread_count
          FROM chat_conversations c
          JOIN (
            SELECT ranked.conversation_id, ranked.message_text, ranked.sent_at, ranked.seen_at
              FROM (
                SELECT cm.conversation_id,
                       cm.message_text,
                       cm.sent_at,
                       cm.seen_at,
                       ROW_NUMBER() OVER (
                         PARTITION BY cm.conversation_id
                         ORDER BY cm.sent_at DESC, cm.id DESC
                       ) AS rn
                  FROM chat_messages cm
              ) ranked
             WHERE ranked.rn = 1
          ) m ON m.conversation_id = c.id
         WHERE c.user_a_id = :me OR c.user_b_id = :me
         ORDER BY m.sent_at DESC
        """;
    return chatJdbc.query(
        query,
        Map.of("me", me),
        (rs, rowNum) ->
            new ConversationSummary(
                rs.getLong("conversation_id"),
                rs.getLong("other_user_id"),
                null,
                null,
                false,
                null,
                rs.getString("message_text"),
                rs.getTimestamp("sent_at") == null ? null : rs.getTimestamp("sent_at").toLocalDateTime(),
                rs.getTimestamp("seen_at") == null ? null : rs.getTimestamp("seen_at").toLocalDateTime(),
                rs.getLong("unread_count")));
  }

  public List<ConversationSummary> findConversationSummaries(
      Long me, int page, int size, String searchKey) {
    String normalizedSearch = searchKey == null ? "" : searchKey.trim();
    boolean hasSearch = !normalizedSearch.isEmpty();
    String searchFilterSql =
        hasSearch
            ? """
              AND LOWER(COALESCE(m.message_text, '')) LIKE LOWER(CONCAT('%', :search, '%'))
            """
            : "";
    String query =
        """
        SELECT c.id AS conversation_id,
               CASE WHEN c.user_a_id = :me THEN c.user_b_id ELSE c.user_a_id END AS other_user_id,
               m.message_text,
               m.sent_at,
               m.seen_at,
               (
                 SELECT count(1)
                   FROM chat_messages um
                  WHERE um.conversation_id = c.id
                    AND um.receiver_id = :me
                    AND um.seen_at IS NULL
               ) AS unread_count
          FROM chat_conversations c
          JOIN (
            SELECT ranked.conversation_id, ranked.message_text, ranked.sent_at, ranked.seen_at
              FROM (
                SELECT cm.conversation_id,
                       cm.message_text,
                       cm.sent_at,
                       cm.seen_at,
                       ROW_NUMBER() OVER (
                         PARTITION BY cm.conversation_id
                         ORDER BY cm.sent_at DESC, cm.id DESC
                       ) AS rn
                  FROM chat_messages cm
              ) ranked
             WHERE ranked.rn = 1
          ) m ON m.conversation_id = c.id
         WHERE (c.user_a_id = :me OR c.user_b_id = :me)
         %s
         ORDER BY m.sent_at DESC
         LIMIT :limit OFFSET :offset
        """
            .formatted(searchFilterSql);

    return chatJdbc.query(
        query,
        new MapSqlParameterSource()
            .addValue("me", me)
            .addValue("limit", size)
            .addValue("offset", page * size)
            .addValue("search", normalizedSearch),
        (rs, rowNum) ->
            new ConversationSummary(
                rs.getLong("conversation_id"),
                rs.getLong("other_user_id"),
                null,
                null,
                false,
                null,
                rs.getString("message_text"),
                rs.getTimestamp("sent_at") == null ? null : rs.getTimestamp("sent_at").toLocalDateTime(),
                rs.getTimestamp("seen_at") == null ? null : rs.getTimestamp("seen_at").toLocalDateTime(),
                rs.getLong("unread_count")));
  }

  public long countConversationSummaries(Long me, String searchKey) {
    String normalizedSearch = searchKey == null ? "" : searchKey.trim();
    boolean hasSearch = !normalizedSearch.isEmpty();
    String searchFilterSql =
        hasSearch
            ? """
               AND LOWER(COALESCE(m.message_text, '')) LIKE LOWER(CONCAT('%', :search, '%'))
            """
            : "";
    String query =
        """
            SELECT count(1)
              FROM chat_conversations c
              JOIN (
                SELECT ranked.conversation_id, ranked.message_text, ranked.sent_at
                  FROM (
                    SELECT cm.conversation_id,
                           cm.message_text,
                           cm.sent_at,
                           ROW_NUMBER() OVER (
                             PARTITION BY cm.conversation_id
                             ORDER BY cm.sent_at DESC, cm.id DESC
                           ) AS rn
                      FROM chat_messages cm
                  ) ranked
                 WHERE ranked.rn = 1
              ) m ON m.conversation_id = c.id
             WHERE (c.user_a_id = :me OR c.user_b_id = :me)
             %s
            """
            .formatted(searchFilterSql);
    Long count =
        chatJdbc.queryForObject(
            query,
            new MapSqlParameterSource().addValue("me", me).addValue("search", normalizedSearch),
            Long.class);
    return count == null ? 0L : count;
  }

  public DeleteMessageResponse deleteLastSentMessage(
      Long conversationId, Long senderId, LocalDateTime minSentAt, LocalDateTime deletedAt) {
    List<DeleteMessageResponse> deleted =
        chatJdbc.query(
            """
            DELETE FROM chat_messages
             WHERE id = (
               SELECT id
                 FROM chat_messages
                WHERE conversation_id = :conversationId
                  AND sender_id = :senderId
                  AND sent_at >= :minSentAt
                ORDER BY sent_at DESC, id DESC
                LIMIT 1
             )
             RETURNING id, conversation_id
            """,
            new MapSqlParameterSource()
                .addValue("conversationId", conversationId)
                .addValue("senderId", senderId)
                .addValue("minSentAt", Timestamp.valueOf(minSentAt)),
            (rs, rowNum) ->
                new DeleteMessageResponse(
                    rs.getLong("id"), rs.getLong("conversation_id"), deletedAt));

    return deleted.isEmpty() ? null : deleted.get(0);
  }

  public MessageMeta findLastSentMessageMeta(Long conversationId, Long senderId) {
    List<MessageMeta> rows =
        chatJdbc.query(
            """
            SELECT id, conversation_id, sender_id, sent_at
              FROM chat_messages
             WHERE conversation_id = :conversationId
               AND sender_id = :senderId
             ORDER BY sent_at DESC, id DESC
             LIMIT 1
            """,
            Map.of("conversationId", conversationId, "senderId", senderId),
            (rs, rowNum) ->
                new MessageMeta(
                    rs.getLong("id"),
                    rs.getLong("conversation_id"),
                    rs.getLong("sender_id"),
                    rs.getTimestamp("sent_at") == null ? null : rs.getTimestamp("sent_at").toLocalDateTime()));
    return rows.isEmpty() ? null : rows.get(0);
  }

  public MessageMeta findMessageMetaForParticipant(Long messageId, Long userId) {
    List<MessageMeta> rows =
        chatJdbc.query(
            """
            SELECT id, conversation_id, sender_id, sent_at
              FROM chat_messages
             WHERE id = :messageId
               AND (sender_id = :userId OR receiver_id = :userId)
            """,
            Map.of("messageId", messageId, "userId", userId),
            (rs, rowNum) ->
                new MessageMeta(
                    rs.getLong("id"),
                    rs.getLong("conversation_id"),
                    rs.getLong("sender_id"),
                    rs.getTimestamp("sent_at") == null ? null : rs.getTimestamp("sent_at").toLocalDateTime()));

    return rows.isEmpty() ? null : rows.get(0);
  }

  public MessageResponse updateMessageText(Long messageId, Long senderId, String messageText, Long me) {
    List<MessageResponse> rows =
        chatJdbc.query(
            """
            UPDATE chat_messages
               SET message_text = :messageText
             WHERE id = :messageId
               AND sender_id = :senderId
             RETURNING id, conversation_id, sender_id, receiver_id, message_text, attachment_key,
                       attachment_file_name, attachment_content_type, sent_at, seen_at
            """,
            new MapSqlParameterSource()
                .addValue("messageId", messageId)
                .addValue("senderId", senderId)
                .addValue("messageText", messageText),
            (rs, rowNum) -> {
              Long sender = rs.getLong("sender_id");
              return new MessageResponse(
                  rs.getLong("id"),
                  rs.getLong("conversation_id"),
                  sender,
                  rs.getLong("receiver_id"),
                  rs.getString("message_text"),
                  rs.getString("attachment_key"),
                  rs.getString("attachment_file_name"),
                  rs.getString("attachment_content_type"),
                  rs.getTimestamp("sent_at") == null ? null : rs.getTimestamp("sent_at").toLocalDateTime(),
                  rs.getTimestamp("seen_at") == null ? null : rs.getTimestamp("seen_at").toLocalDateTime(),
                  sender.equals(me));
            });

    return rows.isEmpty() ? null : rows.get(0);
  }

  public String findAttachmentKeyByMessageIdForUser(Long messageId, Long userId) {
    List<Map<String, Object>> rows =
        chatJdbc.queryForList(
            """
            SELECT attachment_key
              FROM chat_messages
             WHERE id = :messageId
               AND (sender_id = :userId OR receiver_id = :userId)
            """,
            Map.of("messageId", messageId, "userId", userId));

    if (rows.isEmpty()) {
      return null;
    }
    Object key = rows.get(0).get("attachment_key");
    return key == null ? null : String.valueOf(key);
  }

  public List<Map<String, Object>> findUnreadCountsByReceiver() {
    return chatJdbc.queryForList(
        """
        SELECT receiver_id, count(1) AS unread_count
          FROM chat_messages
         WHERE seen_at IS NULL
         GROUP BY receiver_id
        """,
        Map.of());
  }

  public List<GlobalMessageResponse> findGlobalMessages(int page, int size, String searchKey) {
    String normalizedSearch = searchKey == null ? "" : searchKey.trim();
    return chatJdbc.query(
        """
        SELECT m.id,
               m.conversation_id,
               m.sender_id,
               s.name AS sender_name,
               m.receiver_id,
               r.name AS receiver_name,
               m.message_text,
               m.attachment_file_name,
               m.sent_at,
               m.seen_at
          FROM chat_messages m
          JOIN users s ON s.id = m.sender_id
          JOIN users r ON r.id = m.receiver_id
         WHERE (:search = ''
                OR LOWER(COALESCE(m.message_text, '')) LIKE LOWER(CONCAT('%', :search, '%'))
                OR LOWER(COALESCE(s.name, '')) LIKE LOWER(CONCAT('%', :search, '%'))
                OR LOWER(COALESCE(r.name, '')) LIKE LOWER(CONCAT('%', :search, '%'))
                OR CAST(m.sender_id AS TEXT) LIKE CONCAT('%', :search, '%')
                OR CAST(m.receiver_id AS TEXT) LIKE CONCAT('%', :search, '%'))
         ORDER BY m.sent_at DESC
         LIMIT :limit OFFSET :offset
        """,
        new MapSqlParameterSource()
            .addValue("search", normalizedSearch)
            .addValue("limit", size)
            .addValue("offset", page * size),
        (rs, rowNum) ->
            new GlobalMessageResponse(
                rs.getLong("id"),
                rs.getLong("conversation_id"),
                rs.getLong("sender_id"),
                rs.getString("sender_name"),
                rs.getLong("receiver_id"),
                rs.getString("receiver_name"),
                rs.getString("message_text"),
                rs.getString("attachment_file_name"),
                rs.getTimestamp("sent_at") == null ? null : rs.getTimestamp("sent_at").toLocalDateTime(),
                rs.getTimestamp("seen_at") == null ? null : rs.getTimestamp("seen_at").toLocalDateTime()));
  }
}
