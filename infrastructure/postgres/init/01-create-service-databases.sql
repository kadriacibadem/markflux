CREATE ROLE product_service LOGIN PASSWORD 'product_service';
CREATE DATABASE product_db OWNER product_service;
REVOKE CONNECT ON DATABASE product_db FROM PUBLIC;

CREATE ROLE inventory_service LOGIN PASSWORD 'inventory_service';
CREATE DATABASE inventory_db OWNER inventory_service;
REVOKE CONNECT ON DATABASE inventory_db FROM PUBLIC;

CREATE ROLE cart_service LOGIN PASSWORD 'cart_service';
CREATE DATABASE cart_db OWNER cart_service;
REVOKE CONNECT ON DATABASE cart_db FROM PUBLIC;

CREATE ROLE order_service LOGIN PASSWORD 'order_service';
CREATE DATABASE order_db OWNER order_service;
REVOKE CONNECT ON DATABASE order_db FROM PUBLIC;
