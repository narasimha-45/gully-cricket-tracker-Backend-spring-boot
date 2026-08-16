DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_name = 'player_partnerships'
          AND column_name = 'player1runs'
    ) AND NOT EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_name = 'player_partnerships'
          AND column_name = 'player1_runs'
    ) THEN
        ALTER TABLE player_partnerships
            RENAME COLUMN player1runs TO player1_runs;
    END IF;

    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_name = 'player_partnerships'
          AND column_name = 'player1balls_faced'
    ) AND NOT EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_name = 'player_partnerships'
          AND column_name = 'player1_balls_faced'
    ) THEN
        ALTER TABLE player_partnerships
            RENAME COLUMN player1balls_faced TO player1_balls_faced;
    END IF;

    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_name = 'player_partnerships'
          AND column_name = 'player1dot_balls'
    ) AND NOT EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_name = 'player_partnerships'
          AND column_name = 'player1_dot_balls'
    ) THEN
        ALTER TABLE player_partnerships
            RENAME COLUMN player1dot_balls TO player1_dot_balls;
    END IF;

    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_name = 'player_partnerships'
          AND column_name = 'player1fours_hit'
    ) AND NOT EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_name = 'player_partnerships'
          AND column_name = 'player1_fours_hit'
    ) THEN
        ALTER TABLE player_partnerships
            RENAME COLUMN player1fours_hit TO player1_fours_hit;
    END IF;

    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_name = 'player_partnerships'
          AND column_name = 'player1sixes_hit'
    ) AND NOT EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_name = 'player_partnerships'
          AND column_name = 'player1_sixes_hit'
    ) THEN
        ALTER TABLE player_partnerships
            RENAME COLUMN player1sixes_hit TO player1_sixes_hit;
    END IF;


    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_name = 'player_partnerships'
          AND column_name = 'player2runs'
    ) AND NOT EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_name = 'player_partnerships'
          AND column_name = 'player2_runs'
    ) THEN
        ALTER TABLE player_partnerships
            RENAME COLUMN player2runs TO player2_runs;
    END IF;

    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_name = 'player_partnerships'
          AND column_name = 'player2balls_faced'
    ) AND NOT EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_name = 'player_partnerships'
          AND column_name = 'player2_balls_faced'
    ) THEN
        ALTER TABLE player_partnerships
            RENAME COLUMN player2balls_faced TO player2_balls_faced;
    END IF;

    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_name = 'player_partnerships'
          AND column_name = 'player2dot_balls'
    ) AND NOT EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_name = 'player_partnerships'
          AND column_name = 'player2_dot_balls'
    ) THEN
        ALTER TABLE player_partnerships
            RENAME COLUMN player2dot_balls TO player2_dot_balls;
    END IF;

    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_name = 'player_partnerships'
          AND column_name = 'player2fours_hit'
    ) AND NOT EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_name = 'player_partnerships'
          AND column_name = 'player2_fours_hit'
    ) THEN
        ALTER TABLE player_partnerships
            RENAME COLUMN player2fours_hit TO player2_fours_hit;
    END IF;

    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_name = 'player_partnerships'
          AND column_name = 'player2sixes_hit'
    ) AND NOT EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_name = 'player_partnerships'
          AND column_name = 'player2_sixes_hit'
    ) THEN
        ALTER TABLE player_partnerships
            RENAME COLUMN player2sixes_hit TO player2_sixes_hit;
    END IF;
END $$;