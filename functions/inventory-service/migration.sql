create DATABASE inventory_db;
\c inventory_db
CREATE TABLE IF NOT EXISTS inventory(
                                        id INTEGER,
                                        name VARCHAR(100),
    type VARCHAR(100),
    quantity INTEGER,
    PRIMARY KEY(id)
    );
INSERT INTO inventory(id, name, type, quantity) VALUES
                                                    (1,'Rose','flower',35),
                                                    (2,'Lily','flower',23),
                                                    (3,'Jasmine','flower',23);

select * from public.inventory;