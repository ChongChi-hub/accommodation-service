-- V5: Additive participant_pair_key with UNIQUE constraint to prevent duplicate conversations under concurrency

ALTER TABLE conversations
    ADD COLUMN participant_pair_key VARCHAR(160) NULL AFTER room_id;

UPDATE conversations c
JOIN (
    SELECT conversation_id,
           MIN(user_id) AS u_min,
           MAX(user_id) AS u_max
    FROM conversation_participants
    GROUP BY conversation_id
    HAVING COUNT(DISTINCT user_id) >= 2
) p ON p.conversation_id = c.id
SET c.participant_pair_key = CONCAT(p.u_min, '|', p.u_max, '|', COALESCE(NULLIF(TRIM(c.room_id), ''), 'direct'))
WHERE c.participant_pair_key IS NULL;

ALTER TABLE conversations
    ADD CONSTRAINT uq_conversations_pair_key UNIQUE (participant_pair_key);
