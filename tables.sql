-- USER
create table public.User (
    U_ID serial primary key,
    U_Role varchar not null,
    U_Password varchar not null,
    RegistrationDate timestamp default NOW(),
    U_PrimaryEmail varchar not null,
    u_firstname varchar not null,
    u_lastname varchar not null,
    U_Name varchar generated always as ( u_firstname || ' ' || u_lastname ) stored
);

-- create table with logins
create table public.UserLogin(
    U_ID integer,
    U_Email varchar not null,
    U_Password varchar not null,
    constraint fk_user
        foreign key (U_ID)
        REFERENCES public.User (U_ID)
);
