ALTER TABLE dish
    ADD COLUMN IF NOT EXISTS price NUMERIC(10, 2) CHECK (price >= 0);

UPDATE dish
SET price = 2000
WHERE id = 1 AND name = 'Salade fraîche';

UPDATE dish
SET price = 6000
WHERE id = 2 AND name = 'Poulet grillé';
