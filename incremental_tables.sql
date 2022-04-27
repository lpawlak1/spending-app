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

-- SCOPE 3 31.03.2022

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

-- jako ze kazdy uzytkownik musi miec budget to funkcja do tego
drop function if exists pg_temp.insert_budget(int);
create function pg_temp.insert_budget(int)
    returns int as $$
BEGIN
    insert into Budget(B_Amount, U_ID) values (1000.0, $1);
    return 1;
end;
$$ language plpgsql;

select pg_temp.insert_budget(u_id) from public.User;

create table Category(
    Cat_ID serial primary key,
    Cat_Name varchar not null,
    Cat_Superior_Cat_Id int,
    constraint fk_cat_sup_cat foreign key (Cat_Superior_Cat_Id) references Category(Cat_ID)
);

insert into Category(Cat_Name) VALUES ('Transportation');
insert into Category(Cat_Name) VALUES ('Food');
insert into Category(Cat_Name, Cat_Superior_Cat_Id) VALUES ('Drinks', (select Cat_ID from Category where cat_name = 'Food'));

drop function if exists public.user_email_trigger_proc() cascade;
create function public.user_email_trigger_proc() returns trigger as $user_email_tri$
    declare
        email varchar;
    begin
        -- trzeba sprawdzic czy na pewno w UserLogin ten mail nie istnieje i przerwać inserta jeżeli istnieje
        email := NEW.u_primaryemail;
        IF exists (select count(*) from public.userlogin ul where ul.U_Email = email and (NEW.U_id is null or ul.U_ID != NEW.U_id))
            OR exists(select ul.u_primaryemail from public."user" ul where ul.U_PrimaryEmail = email) THEN
            RAISE EXCEPTION '% % can''t have be placed in db', NEW.U_firstname, NEW.U_lastname;
        END IF;
        return NEW;
    end;
$user_email_tri$ language plpgsql;

CREATE TRIGGER email_trigger_user BEFORE INSERT OR UPDATE ON public."user"
    FOR EACH ROW EXECUTE PROCEDURE public.user_email_trigger_proc();

-- test if working
-- insert into public."user"(u_role, u_primaryemail, u_firstname, u_lastname)
-- values (
--         'user',
--         'admin@admin',
--         'user first name',
--         'user last name'
--        );

create index if not exists email_w_passwd_index on public.UserLogin (u_email) include (u_password);
create unique index if not exists id_email_login on public.UserLogin(U_id, U_Email);

-- call insert_budget(1, cast (1500.00 as money));
create or replace procedure insert_budget(u_id int, new_budget money)
language plpgsql
as $$
    declare
        month text;
    BEGIN
        select to_char(now(), 'MM') into month;
        if exists(select * from budget b where b.U_ID = insert_budget.u_id and to_char(b.b_starting_date, 'MM') = month) then
            update budget
            set B_Active = false
            where budget.u_id = insert_budget.u_id and to_char(budget.b_starting_date, 'MM') = month;
        end if;

        insert into budget(B_Amount,
                           B_Starting_Date,
                           U_ID,
                           B_Active,
                           AddedDate)
        values (
               new_budget,
               to_date(to_char(now(), 'MM-YYYY'), 'MM-YYYY'), -- pierwszy dzien aktualnego miesiaca
               u_id,
               true,
               now()
               );
    END;
$$;

drop function if exists public.get_all_months_between;
create or replace function public.get_all_months_between(start_date timestamp, end_date timestamp) returns table(month text,year text) as
$$
    declare
    begin
        return query (
        WITH RECURSIVE date_range AS
           (
               select (to_char(start_date, 'MM')) as month_, (to_char(start_date, 'YYYY')) as year_, start_date + INTERVAL '1 MONTH' as next_month_date
               UNION
               SELECT (to_char(dr.next_month_date, 'MM')) as month_, (to_char(dr.next_month_date, 'YYYY')) as year_, dr.next_month_date +  INTERVAL '1 MONTH' as next_month_date
               FROM date_range dr
               where next_month_date <= end_date
           )
        select month_, year_ from date_range
        order by next_month_date asc);
    end
$$ language plpgsql;

drop function if exists public.get_budget_per_month(u_id int);
create or replace function get_budget_per_month(u_id int) returns table(budget money, month text, year text) as $body$
declare
    startingDate timestamp;
    endingDate timestamp;
begin
    select to_date(to_char(min(b.b_starting_date), 'MM-YYYY'),'MM-YYYY'), to_date(to_char(max(b.b_starting_date),'MM-YYYY'),'MM-YYYY') into startingDate, endingDate from budget b where b.u_id = $1 and b.b_active = true;
    return query select (select B_Amount
               from budget b
               where b.u_id = $1
                 and b.b_active = true
                 and b.b_starting_date < to_date(gamb.month || '-' || gamb.year, 'MM-YYYY') + 1 * INTERVAL '1 Month'
               order by b.b_starting_date desc limit 1) as budget, gamb.*
    from public.get_all_months_between(startingDate, endingDate) gamb;
end;
$body$ language plpgsql;

-- scope 02.04.2022

-- Był błąd, trzeba go naprawić
drop function if exists public.user_email_trigger_proc() cascade;
create function public.user_email_trigger_proc() returns trigger as $user_email_tri$
declare
    email varchar;
begin
    -- trzeba sprawdzic czy na pewno w UserLogin ten mail nie istnieje i przerwać inserta jeżeli istnieje
    email := NEW.u_primaryemail;
    IF exists (select ul.u_id from public.userlogin ul where ul.U_Email = email and (NEW.U_id is null or ul.U_ID != NEW.U_id))
        OR exists(select ul.u_primaryemail from public."user" ul where ul.U_PrimaryEmail = email and ul.u_id != NEW.u_id) THEN
        RAISE EXCEPTION '% % can''t have be placed in db', NEW.U_firstname, NEW.U_lastname;
    END IF;
    return NEW;
end;
$user_email_tri$ language plpgsql;

CREATE TRIGGER email_trigger_user BEFORE INSERT OR UPDATE ON public."user"
    FOR EACH ROW EXECUTE PROCEDURE public.user_email_trigger_proc();

create table Colors (
    Col_ID serial primary key ,
    Col_Name varchar not null unique ,
    Col_Filename varchar not null unique
);

alter table public.user add column Col_ID int;
alter table public.user add constraint fk_user_color foreign key (Col_ID) references Colors (Col_ID);

insert into colors (Col_Name, Col_Filename) values ('Orange', 'orange.css');
insert into colors (Col_Name, Col_Filename) values ('Pink', 'pink.css');

update public.user set Col_ID = (select col_id from colors limit 1);

-- SCOPE 09.04.2022

create table Expense(
    Ex_ID serial primary key,
    Ex_name varchar,
    Cat_ID int not null,
    U_ID int not null,
    AddedDateTime timestamp not null,
    LastModificationDate timestamp not null,
    DateOfPurchase timestamp not null,
    Description varchar,
    Price money not null,
    Deleted bool not null,
    constraint fk_user_expense foreign key (U_ID) references public.User (U_ID)
);


drop function if exists public.get_users_current_budget(u_id int);
create or replace function get_users_current_budget(u_id int) returns money as $$
declare
    ret_value money;
begin
    with
    wydatki as (
        select coalesce(sum(price), 0::money) as sum_price
        from expense e
        where e.u_id = $1
          and e.dateofpurchase
              between to_date(to_char(now(), 'YYYY-MM'),'YYYY-MM')
              and to_date(to_char(now() + 1 * INTERVAL '1 Month' , 'YYYY-MM'), 'YYYY-MM')
    )
    select b.b_amount-w.sum_price as current_budget
    into ret_value
    from budget b
        inner join wydatki w on 1=1
    where b.u_id = 1 and b.b_active = true and b_starting_date = (select
        max(b_starting_date) from budget b where b.u_id = $1 and b.b_active = true);

    return(ret_value);
end;
$$ language plpgsql;

-- 27.04 
update public.User
set col_id = (select col_id from public.colors where col_name='Pink');


