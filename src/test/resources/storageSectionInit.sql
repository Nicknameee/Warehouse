CREATE OR REPLACE FUNCTION init_storage_sections() RETURNS void
    LANGUAGE plpgsql AS
$$
DECLARE
    cur_warehouses CURSOR FOR SELECT id, code
                              FROM warehouses; w_rec RECORD; i INTEGER;
BEGIN
    OPEN cur_warehouses;
    LOOP
        FETCH cur_warehouses INTO w_rec; EXIT WHEN NOT FOUND;
        FOR i IN 1..10
            LOOP
                BEGIN
                    INSERT INTO storage_sections(warehouse_id, code, is_active)
                    VALUES (w_rec.id, format('%s-%s', w_rec.code, i), TRUE);
                EXCEPTION
                    WHEN unique_violation THEN CONTINUE;
                END;
            END LOOP;
    END LOOP;
    CLOSE cur_warehouses;
END;
$$;
SELECT init_storage_sections();