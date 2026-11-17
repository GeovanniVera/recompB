-- Nutrition domain, phase 1.
-- All timestamps are UTC (DATETIME(6)), matching app_users/otp_codes.

CREATE TABLE user_profiles (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    sex VARCHAR(10) NOT NULL,
    age INT NOT NULL,
    height_cm DECIMAL(5, 2) NOT NULL,
    current_weight_kg DECIMAL(5, 2) NOT NULL,
    gym_days_per_week INT NOT NULL,
    activity_factor DECIMAL(4, 2) NOT NULL DEFAULT 1.48,
    deficit_pct DECIMAL(4, 2) NOT NULL DEFAULT 0.12,
    protein_per_kg DECIMAL(4, 2) NOT NULL DEFAULT 2.00,
    fat_per_kg DECIMAL(4, 2) NOT NULL DEFAULT 0.80,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_profiles_user (user_id),
    CONSTRAINT fk_user_profiles_user
        FOREIGN KEY (user_id) REFERENCES app_users (id)
        ON DELETE CASCADE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

CREATE TABLE food_items (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NULL,
    name VARCHAR(150) NOT NULL,
    state_tag VARCHAR(30) NOT NULL,
    kcal_per_100g DECIMAL(6, 2) NOT NULL,
    protein_per_100g DECIMAL(6, 2) NOT NULL,
    carbs_per_100g DECIMAL(6, 2) NOT NULL,
    fat_per_100g DECIMAL(6, 2) NOT NULL,
    quick_grams_json VARCHAR(100) NULL,
    created_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    KEY idx_food_items_user (user_id),
    KEY idx_food_items_name (name),
    CONSTRAINT fk_food_items_user
        FOREIGN KEY (user_id) REFERENCES app_users (id)
        ON DELETE CASCADE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

CREATE TABLE quick_plates (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NULL,
    title VARCHAR(120) NOT NULL,
    subtitle VARCHAR(255) NULL,
    kcal DECIMAL(6, 2) NOT NULL,
    protein DECIMAL(6, 2) NOT NULL,
    carbs DECIMAL(6, 2) NOT NULL,
    fat DECIMAL(6, 2) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    KEY idx_quick_plates_user (user_id),
    CONSTRAINT fk_quick_plates_user
        FOREIGN KEY (user_id) REFERENCES app_users (id)
        ON DELETE CASCADE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

CREATE TABLE daily_logs (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    log_date DATE NOT NULL,
    is_gym_day BOOLEAN NOT NULL DEFAULT TRUE,
    water_ml INT NOT NULL DEFAULT 0,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_daily_logs_user_date (user_id, log_date),
    CONSTRAINT fk_daily_logs_user
        FOREIGN KEY (user_id) REFERENCES app_users (id)
        ON DELETE CASCADE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

CREATE TABLE meal_entries (
    id BIGINT NOT NULL AUTO_INCREMENT,
    daily_log_id BIGINT NOT NULL,
    food_item_id BIGINT NULL,
    entry_name VARCHAR(200) NOT NULL,
    grams DECIMAL(7, 2) NULL,
    kcal DECIMAL(7, 2) NOT NULL,
    protein DECIMAL(6, 2) NOT NULL,
    carbs DECIMAL(6, 2) NOT NULL,
    fat DECIMAL(6, 2) NOT NULL,
    is_off_plan BOOLEAN NOT NULL DEFAULT FALSE,
    logged_time VARCHAR(10) NULL,
    created_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    KEY idx_meal_entries_log (daily_log_id),
    KEY idx_meal_entries_food (food_item_id),
    CONSTRAINT fk_meal_entries_log
        FOREIGN KEY (daily_log_id) REFERENCES daily_logs (id)
        ON DELETE CASCADE,
    CONSTRAINT fk_meal_entries_food
        FOREIGN KEY (food_item_id) REFERENCES food_items (id)
        ON DELETE SET NULL
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

CREATE TABLE biweekly_checkins (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    checkin_date DATE NOT NULL,
    weight_kg DECIMAL(5, 2) NOT NULL,
    waist_cm DECIMAL(5, 2) NULL,
    green_days_pct INT NULL,
    clean_eating_pct INT NULL,
    note VARCHAR(500) NULL,
    created_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    KEY idx_biweekly_checkins_user_date (user_id, checkin_date),
    CONSTRAINT fk_biweekly_checkins_user
        FOREIGN KEY (user_id) REFERENCES app_users (id)
        ON DELETE CASCADE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;
