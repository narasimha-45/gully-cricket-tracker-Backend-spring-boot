-- V9 created test_innings_per_team as SMALLINT, but the Match entity field is
-- Integer, which Hibernate schema validation expects to map to INTEGER (int4) —
-- causing a Schema-validation: wrong column type failure on startup. Widening
-- rather than narrowing the entity, since inningsPerTeam has no real reason to
-- be constrained to SMALLINT and Integer is consistent with every other numeric
-- column on this table (team_a_score, win_by_runs, etc).

ALTER TABLE matches
    ALTER COLUMN test_innings_per_team TYPE INTEGER;
