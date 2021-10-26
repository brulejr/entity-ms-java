CREATE TABLE IF NOT EXISTS t_item (
    it_id SERIAL PRIMARY KEY,
    it_guid VARCHAR(64) NOT NULL UNIQUE,
    it_type VARCHAR(64) NOT NULL,
    it_name VARCHAR(64) NOT NULL UNIQUE
);

CREATE TABLE IF NOT EXISTS t_lookup_value (
    lv_id SERIAL PRIMARY KEY,
    lv_entity_id NUMBER,
    lv_value_type VARCHAR(64) NOT NULL,
    lv_value VARCHAR(64) NOT NULL
);
