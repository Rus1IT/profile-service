CREATE TABLE user_profiles (
                               user_id VARCHAR(255) NOT NULL PRIMARY KEY,
                               default_currency VARCHAR(3),
                               balance_visibility BOOLEAN,
                               timezone VARCHAR(50),
                               language VARCHAR(5),
                               theme VARCHAR(10),
                               notify_on_budget_limit BOOLEAN,
                               onboarding_completed BOOLEAN,
                               created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
                               updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL
);