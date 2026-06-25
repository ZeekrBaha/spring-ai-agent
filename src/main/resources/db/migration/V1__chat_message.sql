-- Conversation memory for the JDBC-backed ChatMemoryStore.
-- One row per message; (conversation_id, seq) orders messages within a conversation.
CREATE TABLE IF NOT EXISTS chat_message (
    conversation_id TEXT        NOT NULL,
    seq             BIGINT      NOT NULL,
    role            TEXT        NOT NULL,   -- 'user' | 'assistant'
    content         TEXT        NOT NULL,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (conversation_id, seq)
);

CREATE INDEX IF NOT EXISTS chat_message_conv_seq_idx
    ON chat_message (conversation_id, seq);
