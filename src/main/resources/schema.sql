CREATE TABLE IF NOT EXISTS t_thing (
    th_id SERIAL PRIMARY KEY,
    th_guid VARCHAR(64) NOT NULL UNIQUE,
    th_type VARCHAR(64) NOT NULL,
    th_name VARCHAR(64) NOT NULL UNIQUE
);

CREATE TABLE IF NOT EXISTS t_lookup_value (
    lv_id SERIAL PRIMARY KEY,
    lv_entity_id NUMBER,
    lv_value_type VARCHAR(64) NOT NULL,
    lv_value VARCHAR(64) NOT NULL
);
