-- This creates the one table the whole app uses.
-- I picked SQLite because it's just a file - no server to set up,
-- which is perfect since this is only ever used by one person (me).

CREATE TABLE IF NOT EXISTS company (
    id            INTEGER PRIMARY KEY AUTOINCREMENT,
    name          TEXT NOT NULL,
    role_title    TEXT,
    stage         TEXT NOT NULL DEFAULT 'APPLIED'
                  CHECK (stage IN ('APPLIED','ASSESSMENT','INTERVIEW','OFFER','REJECTED')),
    deadline      TEXT,               -- stored as a plain date string, e.g. 2026-07-20
    notes         TEXT,
    application_url TEXT,
    created_at    TEXT NOT NULL DEFAULT (datetime('now')),
    updated_at    TEXT NOT NULL DEFAULT (datetime('now'))
);

-- These make searching and sorting by stage/deadline faster later on
CREATE INDEX IF NOT EXISTS idx_company_stage ON company(stage);
CREATE INDEX IF NOT EXISTS idx_company_deadline ON company(deadline);