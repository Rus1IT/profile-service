CREATE TABLE accounts (
    -- Основной ключ таблицы, UUID
                          account_id UUID PRIMARY KEY,

    -- Внешний ключ, ссылается на user_profiles.user_id
                          user_profile_id VARCHAR(255) NOT NULL,

                          bank_name VARCHAR(100) NOT NULL,

                          account_name VARCHAR(100) NOT NULL,

                          currency VARCHAR(3) NOT NULL,

                          first_transaction_date TIMESTAMPTZ,

                          last_transaction_date TIMESTAMPTZ,

                          created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
                          updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- Внешний ключ: связывает эту таблицу с user_profiles
                          CONSTRAINT fk_accounts_user_profile
                              FOREIGN KEY (user_profile_id)
                                  REFERENCES user_profiles (user_id)
                                  ON DELETE CASCADE,

                          CONSTRAINT uk_user_profile_bank_name
                              UNIQUE (user_profile_id, bank_name)
);

CREATE INDEX idx_accounts_user_profile_id ON accounts(user_profile_id);