-- Records why each innings ended (declared, all out, overs complete, draw)
-- and whether it was batted under an enforced follow-on. The frontend has
-- always sent isFollowOn/completionReason per innings on POST /matches, but
-- the backend silently dropped both fields since InningsDto didn't declare
-- them. Existing rows default to "unknown" (false / NULL) since that history
-- was never captured.

ALTER TABLE match_innings_summary
    ADD COLUMN IF NOT EXISTS is_follow_on BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE match_innings_summary
    ADD COLUMN IF NOT EXISTS completion_reason VARCHAR(32);
