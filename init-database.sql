-- Just try to create, ignore if exists
CREATE DATABASE inventorydb;
CREATE DATABASE orderdb;
GRANT ALL PRIVILEGES ON DATABASE inventorydb TO postgres;
GRANT ALL PRIVILEGES ON DATABASE orderdb TO postgres;