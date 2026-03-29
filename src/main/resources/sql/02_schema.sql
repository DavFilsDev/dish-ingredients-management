create type category as enum (
    'VEGETABLE',
    'ANIMAL',
    'MARINE',
    'DAIRY',
    'OTHER'
);

create type dish_type as enum (
    'START',
    'MAIN',
    'DESSERT'
);

create table dish (
                      id serial constraint dish_pk primary key,
                      name varchar(255) not null,
                      dish_type dish_type not null
);

create table ingredient (
                            id serial constraint ingredient_pk primary key,
                            name varchar(255) not null,
                            price numeric(10,2) not null,
                            category category not null,
                            id_dish int,
                            constraint fk_dish foreign key (id_dish) references dish(id) on delete set null
);