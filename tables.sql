-- USER
create table public.User (
    U_ID serial primary key,
    U_Role varchar not null,
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

-- just initial data
insert into public.User (U_Role, U_PrimaryEmail, u_firstname, u_lastname)
values ('Admin', 'admin@admin', 'admin_firstname', 'admin_lastname');

insert into public.UserLogin(U_ID, U_Email, U_Password) values (1, 'admin@admin', 'admin');

create table Budget(
   B_ID serial primary key,
   B_Amount money not null,
   B_Starting_Date timestamp not null default now(),
   U_ID int not null,
   B_Active boolean not null default true,
   AddedDate timestamp not null default now(),
   constraint fk_user_budget
       foreign key (U_ID)
           REFERENCES public.User (U_ID)
);

create table Category(
    Cat_ID serial primary key,
    Cat_Name varchar not null,
    Cat_Superior_Cat_Id int,
    constraint fk_cat_sup_cat foreign key (Cat_Superior_Cat_Id) references Category(Cat_ID)
);

insert into Category(Cat_Name) VALUES ('Transportation');
insert into Category(Cat_Name) VALUES ('Food');
insert into Category(Cat_Name, Cat_Superior_Cat_Id) VALUES ('Drinks', (select Cat_ID from Category where cat_name = 'Food'));
