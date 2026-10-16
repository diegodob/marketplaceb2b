ALTER TABLE scale_discount ADD COLUMN priority INT NOT NULL DEFAULT 0;
ALTER TABLE combo ADD COLUMN priority INT NOT NULL DEFAULT 0;
ALTER TABLE gift ADD COLUMN priority INT NOT NULL DEFAULT 0;

UPDATE scale_discount SET priority = 10;
UPDATE combo SET priority = 20;
UPDATE gift SET priority = 5;
