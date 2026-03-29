-- table_schema.sql
CREATE TABLE restaurant_table (
                                  id SERIAL CONSTRAINT restaurant_table_pk PRIMARY KEY,
                                  number INTEGER NOT NULL UNIQUE
);

-- table_data.sql
INSERT INTO restaurant_table (id, number) VALUES
                                              (1, 1),
                                              (2, 2),
                                              (3, 3),
                                              (4, 4),
                                              (5, 5);

SELECT setval(
               pg_get_serial_sequence('restaurant_table', 'id'),
               (SELECT MAX(id) FROM restaurant_table)
       );

-- modify_order_schema.sql
ALTER TABLE "order"
    ADD COLUMN IF NOT EXISTS id_table INTEGER NOT NULL,
    ADD COLUMN IF NOT EXISTS arrival_datetime TIMESTAMP NOT NULL,
    ADD COLUMN IF NOT EXISTS departure_datetime TIMESTAMP NOT NULL,
    ADD CONSTRAINT fk_order_table FOREIGN KEY (id_table)
    REFERENCES restaurant_table(id) ON DELETE RESTRICT;