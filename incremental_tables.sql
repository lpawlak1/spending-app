-- SCOPE 1
-- USER

create table public.User (
    U_ID serial primary key,
    U_Name varchar not null,
    U_Email varchar not null,
    U_Role varchar not null,
    U_Password varchar not null,
    RegistrationDate timestamp default NOW()
);

-- SCOPE 2 28.03.2022

BEGIN; -- email to primary email
    alter table public.User add U_PrimaryEmail varchar;
    update public.User set U_PrimaryEmail = U_Email;
    alter table public.User alter column U_PrimaryEmail set not null;
    alter table public.User drop U_Email;
COMMIT;

 -- create table with logins
create table public.UserLogin(
    U_ID integer,
    U_Email varchar not null,
    U_Password varchar not null,
    constraint fk_user
        foreign key (U_ID)
        REFERENCES public.User (U_ID)
);

-- move passwords into userLogin
begin;
    insert into public.UserLogin
    select U_id, u_primaryemail, u_password from public.User;
    alter table public.User drop u_password;
commit;

-- change first and last name found in u_name to u_firstname and u_lastname
alter table public.User add u_firstname varchar;
alter table public.User add u_lastname varchar;

drop function if exists pg_temp.insert_first_last(int);
create function pg_temp.insert_first_last(int)
    returns int as $$
declare
    firstname varchar;
    lastname varchar;
BEGIN
    select split_part(u_name,' ',1), split_part(u_name,' ',2) into firstname,lastname from public.User where u_id = $1;
    update public.User
    set u_firstname = firstname,
        u_lastname = lastname
    where u_id=$1;
    return 1;
end;
$$ language plpgsql;

select pg_temp.insert_first_last(u_id) from public.User;
alter table public.User drop column u_name;

alter table public.User add column u_name varchar generated always as ( u_firstname || ' ' || u_lastname ) stored; -- niestety nie ma virtual, ale koncept zostaje
-- u_name no longer as it was

