CREATE TABLE IF NOT EXISTS seasons (
    id VARCHAR(255) PRIMARY KEY,
    season_name VARCHAR(255),
    matches_played INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMP
);

CREATE TABLE IF NOT EXISTS players (
    id VARCHAR(255) PRIMARY KEY,
    name VARCHAR(120) NOT NULL UNIQUE
);

CREATE TABLE IF NOT EXISTS teams (
    id VARCHAR(255) PRIMARY KEY,
    team_name VARCHAR(120) NOT NULL UNIQUE
);

CREATE TABLE IF NOT EXISTS team_season (
    team_id VARCHAR(255) NOT NULL REFERENCES teams(id),
    season_id VARCHAR(255) NOT NULL REFERENCES seasons(id),
    PRIMARY KEY (team_id, season_id)
);

CREATE TABLE IF NOT EXISTS matches (
    id VARCHAR(255) PRIMARY KEY,
    season_id VARCHAR(255) NOT NULL REFERENCES seasons(id),
    status VARCHAR(255) NOT NULL,
    match_type VARCHAR(255) NOT NULL,
    team_a_id VARCHAR(255) NOT NULL REFERENCES teams(id),
    team_b_id VARCHAR(255) NOT NULL REFERENCES teams(id),
    team_a_score INTEGER NOT NULL DEFAULT 0,
    team_a_wickets INTEGER NOT NULL DEFAULT 0,
    team_a_balls_faced INTEGER NOT NULL DEFAULT 0,
    team_b_score INTEGER NOT NULL DEFAULT 0,
    team_b_wickets INTEGER NOT NULL DEFAULT 0,
    team_b_balls_faced INTEGER NOT NULL DEFAULT 0,
    total_overs INTEGER,
    winner_team_id VARCHAR(255) REFERENCES teams(id),
    super_over BOOLEAN NOT NULL DEFAULT FALSE,
    is_batting_first_team_won BOOLEAN DEFAULT FALSE,
    batting_first_team_id VARCHAR(255) REFERENCES teams(id),
    batting_second_team_id VARCHAR(255) REFERENCES teams(id),
    win_by_runs INTEGER,
    win_by_wickets INTEGER,
    is_innings_win BOOLEAN DEFAULT FALSE,
    is_match_drawn BOOLEAN DEFAULT FALSE,
    is_match_tied BOOLEAN DEFAULT FALSE,
    won_by VARCHAR(255),
    match_data JSONB,
    idempotency_key VARCHAR(128) UNIQUE,
    version BIGINT,
    completed_at TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_matches_season_id ON matches(season_id);
CREATE INDEX IF NOT EXISTS idx_matches_team_a_id ON matches(team_a_id);
CREATE INDEX IF NOT EXISTS idx_matches_team_b_id ON matches(team_b_id);
CREATE INDEX IF NOT EXISTS idx_matches_status ON matches(status);
CREATE INDEX IF NOT EXISTS idx_matches_season_status_completed ON matches(season_id, status, completed_at);

CREATE TABLE IF NOT EXISTS match_innings_summary (
    id VARCHAR(255) PRIMARY KEY,
    match_id VARCHAR(255) NOT NULL REFERENCES matches(id) ON DELETE CASCADE,
    batting_team_id VARCHAR(255) NOT NULL REFERENCES teams(id),
    bowling_team_id VARCHAR(255) NOT NULL REFERENCES teams(id),
    sequence_number INTEGER NOT NULL,
    team_innings_number INTEGER NOT NULL,
    runs INTEGER NOT NULL DEFAULT 0,
    wickets INTEGER NOT NULL DEFAULT 0,
    balls INTEGER NOT NULL DEFAULT 0,
    super_over BOOLEAN NOT NULL DEFAULT FALSE,
    completed BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT uk_match_innings_sequence UNIQUE(match_id, sequence_number),
    CONSTRAINT uk_match_team_innings UNIQUE(match_id, batting_team_id, team_innings_number)
);

CREATE INDEX IF NOT EXISTS idx_innings_match_id ON match_innings_summary(match_id);
CREATE INDEX IF NOT EXISTS idx_innings_batting_team_id ON match_innings_summary(batting_team_id);
CREATE INDEX IF NOT EXISTS idx_innings_bowling_team_id ON match_innings_summary(bowling_team_id);

CREATE TABLE IF NOT EXISTS season_players (
    id VARCHAR(255) PRIMARY KEY,
    player_id VARCHAR(255) NOT NULL REFERENCES players(id),
    season_id VARCHAR(255) NOT NULL REFERENCES seasons(id),
    UNIQUE(player_id, season_id)
);

CREATE TABLE IF NOT EXISTS player_teams (
    id VARCHAR(255) PRIMARY KEY,
    player_id VARCHAR(255) NOT NULL REFERENCES players(id),
    team_id VARCHAR(255) NOT NULL REFERENCES teams(id),
    season_id VARCHAR(255) NOT NULL REFERENCES seasons(id),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    UNIQUE(player_id, team_id, season_id)
);

CREATE TABLE IF NOT EXISTS player_matches (
    id VARCHAR(255) PRIMARY KEY,
    player_id VARCHAR(255) NOT NULL REFERENCES players(id),
    match_id VARCHAR(255) NOT NULL REFERENCES matches(id) ON DELETE CASCADE,
    season_id VARCHAR(255) NOT NULL REFERENCES seasons(id),
    team_represented_id VARCHAR(255) NOT NULL REFERENCES teams(id),
    opposition_team_id VARCHAR(255) NOT NULL REFERENCES teams(id),
    match_type VARCHAR(255),
    innings_number INTEGER NOT NULL DEFAULT 1,
    match_won BOOLEAN NOT NULL DEFAULT FALSE,
    player_of_the_match BOOLEAN NOT NULL DEFAULT FALSE,
    batted BOOLEAN NOT NULL DEFAULT FALSE,
    batting_position INTEGER,
    batting_first BOOLEAN NOT NULL DEFAULT FALSE,
    runs_scored INTEGER NOT NULL DEFAULT 0,
    balls_faced INTEGER NOT NULL DEFAULT 0,
    fours_hit INTEGER NOT NULL DEFAULT 0,
    sixes_hit INTEGER NOT NULL DEFAULT 0,
    out BOOLEAN NOT NULL DEFAULT FALSE,
    dot_balls_played INTEGER NOT NULL DEFAULT 0,
    dismissal_type VARCHAR(255),
    bowled BOOLEAN NOT NULL DEFAULT FALSE,
    bowling_first BOOLEAN NOT NULL DEFAULT FALSE,
    wickets_taken INTEGER NOT NULL DEFAULT 0,
    balls_bowled INTEGER NOT NULL DEFAULT 0,
    runs_conceded INTEGER NOT NULL DEFAULT 0,
    maidens_bowled INTEGER NOT NULL DEFAULT 0,
    wides_bowled INTEGER NOT NULL DEFAULT 0,
    no_balls_bowled INTEGER NOT NULL DEFAULT 0,
    bowled_dismissals INTEGER NOT NULL DEFAULT 0,
    caught_dismissals INTEGER NOT NULL DEFAULT 0,
    lbw_dismissals INTEGER NOT NULL DEFAULT 0,
    stumped_dismissals INTEGER NOT NULL DEFAULT 0,
    hit_wicket_dismissals INTEGER NOT NULL DEFAULT 0,
    special_wicket_dismissals INTEGER NOT NULL DEFAULT 0,
    dot_balls_bowled INTEGER NOT NULL DEFAULT 0,
    catches_taken INTEGER NOT NULL DEFAULT 0,
    run_outs INTEGER NOT NULL DEFAULT 0,
    stumpings INTEGER NOT NULL DEFAULT 0,
    UNIQUE(player_id, match_id, team_represented_id, innings_number)
);

CREATE INDEX IF NOT EXISTS idx_player_matches_player_id ON player_matches(player_id);
CREATE INDEX IF NOT EXISTS idx_player_matches_season_id ON player_matches(season_id);
CREATE INDEX IF NOT EXISTS idx_player_matches_team_represented_id ON player_matches(team_represented_id);
CREATE INDEX IF NOT EXISTS idx_player_matches_opposition_team_id ON player_matches(opposition_team_id);
CREATE INDEX IF NOT EXISTS idx_player_matches_match_id ON player_matches(match_id);
CREATE INDEX IF NOT EXISTS idx_player_matches_season_player ON player_matches(season_id, player_id);
CREATE INDEX IF NOT EXISTS idx_player_matches_match_player ON player_matches(match_id, player_id);

CREATE TABLE IF NOT EXISTS player_partnerships (
    id VARCHAR(255) PRIMARY KEY,
    player1_id VARCHAR(255) NOT NULL REFERENCES players(id),
    player2_id VARCHAR(255) NOT NULL REFERENCES players(id),
    match_id VARCHAR(255) NOT NULL REFERENCES matches(id) ON DELETE CASCADE,
    season_id VARCHAR(255) NOT NULL REFERENCES seasons(id),
    team_represented_id VARCHAR(255) NOT NULL REFERENCES teams(id),
    match_type VARCHAR(255),
    innings_number INTEGER NOT NULL DEFAULT 1,
    runs_scored INTEGER NOT NULL DEFAULT 0,
    balls_faced INTEGER NOT NULL DEFAULT 0,
    dot_balls INTEGER NOT NULL DEFAULT 0,
    fours_hit INTEGER NOT NULL DEFAULT 0,
    sixes_hit INTEGER NOT NULL DEFAULT 0,
    match_won BOOLEAN NOT NULL DEFAULT FALSE,
    batting_first BOOLEAN NOT NULL DEFAULT FALSE,
    partnership_number INTEGER NOT NULL DEFAULT 0,
    partnership_broken BOOLEAN NOT NULL DEFAULT FALSE,
    who_got_out_id VARCHAR(255) REFERENCES players(id),
    player1_runs INTEGER NOT NULL DEFAULT 0,
    player1_balls_faced INTEGER NOT NULL DEFAULT 0,
    player1_dot_balls INTEGER NOT NULL DEFAULT 0,
    player1_fours_hit INTEGER NOT NULL DEFAULT 0,
    player1_sixes_hit INTEGER NOT NULL DEFAULT 0,
    player2_runs INTEGER NOT NULL DEFAULT 0,
    player2_balls_faced INTEGER NOT NULL DEFAULT 0,
    player2_dot_balls INTEGER NOT NULL DEFAULT 0,
    player2_fours_hit INTEGER NOT NULL DEFAULT 0,
    player2_sixes_hit INTEGER NOT NULL DEFAULT 0,
    UNIQUE(player1_id, player2_id, match_id, innings_number, partnership_number)
);

CREATE INDEX IF NOT EXISTS idx_partnerships_season_team ON player_partnerships(season_id, team_represented_id);
CREATE INDEX IF NOT EXISTS idx_partnerships_match ON player_partnerships(match_id);

CREATE TABLE IF NOT EXISTS player_rivalries (
    id VARCHAR(255) PRIMARY KEY,
    batsman_id VARCHAR(255) NOT NULL REFERENCES players(id),
    bowler_id VARCHAR(255) NOT NULL REFERENCES players(id),
    match_id VARCHAR(255) NOT NULL REFERENCES matches(id) ON DELETE CASCADE,
    season_id VARCHAR(255) NOT NULL REFERENCES seasons(id),
    match_type VARCHAR(255),
    innings_number INTEGER NOT NULL DEFAULT 1,
    runs_scored INTEGER NOT NULL DEFAULT 0,
    balls_faced INTEGER NOT NULL DEFAULT 0,
    dot_balls INTEGER NOT NULL DEFAULT 0,
    fours_hit INTEGER NOT NULL DEFAULT 0,
    sixes_hit INTEGER NOT NULL DEFAULT 0,
    batsman_dismissed BOOLEAN NOT NULL DEFAULT FALSE,
    dismissal_type VARCHAR(255),
    UNIQUE(batsman_id, bowler_id, match_id, innings_number)
);

CREATE INDEX IF NOT EXISTS idx_rivalries_season_batter_bowler ON player_rivalries(season_id, batsman_id, bowler_id);
CREATE INDEX IF NOT EXISTS idx_rivalries_match ON player_rivalries(match_id);
