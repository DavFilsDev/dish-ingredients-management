--Database creation
create database mini_dish_db;

-- User Creation
create user mini_dish_db_manager with password 'admin_mini_dish_db';

--Grant connect on database access to the database
grant connect on database mini_dish_db to mini_dish_db_manager;

--Optionnal but useful if not wanting to log to another session
\c mini_dish_db;

--Authorize tables creation
grant create on schema public to mini_dish_db_manager;
--Authorize crud operations on all future tables
alter default privileges in schema public
    grant select, insert, update, delete on tables to mini_dish_db_manager;
--if need to use auto-increment (as SERIAL)
alter default privileges in schema public
      grant usage, select, update on sequences to mini_dish_db_manager;