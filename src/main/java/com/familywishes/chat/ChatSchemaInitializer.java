package com.familywishes.chat;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class ChatSchemaInitializer {

  @Qualifier("supabaseChatJdbc")
  private final NamedParameterJdbcTemplate chatJdbc;

  @PostConstruct
  public void init() {
    chatJdbc.getJdbcTemplate().execute(
        """
        CREATE TABLE IF NOT EXISTS chat_conversations (
          id BIGSERIAL PRIMARY KEY,
          user_a_id BIGINT NOT NULL,
          user_b_id BIGINT NOT NULL,
          created_at TIMESTAMP NOT NULL,
          CONSTRAINT uq_chat_conversation UNIQUE(user_a_id, user_b_id)
        )
        """);

    chatJdbc.getJdbcTemplate().execute(
        """
        CREATE TABLE IF NOT EXISTS chat_messages (
          id BIGSERIAL PRIMARY KEY,
          conversation_id BIGINT NOT NULL,
          sender_id BIGINT NOT NULL,
          receiver_id BIGINT NOT NULL,
          message_text TEXT,
          attachment_key TEXT,
          attachment_file_name TEXT,
          attachment_content_type TEXT,
          sent_at TIMESTAMP NOT NULL,
          seen_at TIMESTAMP NULL
        )
        """);

    chatJdbc.getJdbcTemplate().execute(
        "CREATE INDEX IF NOT EXISTS idx_chat_messages_conv_sent ON chat_messages(conversation_id, sent_at DESC)");

    chatJdbc.getJdbcTemplate().execute(
        "CREATE INDEX IF NOT EXISTS idx_chat_messages_receiver_seen ON chat_messages(receiver_id, seen_at)");

    log.info("Supabase chat schema initialized");
  }
}
