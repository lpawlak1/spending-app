-- USER
create table public.User (
    U_ID serial primary key,
    U_Role varchar not null,
    RegistrationDate timestamp default NOW(),
    U_PrimaryEmail varchar not null,
    u_firstname varchar not null,
    u_lastname varchar not null,
    U_Name varchar generated always as ( u_firstname || ' ' || u_lastname ) stored,
    Col_ID int
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

create index if not exists email_w_passwd_index on public.UserLogin (u_email) include (u_password);
create unique index if not exists id_email_login on public.UserLogin(U_id, U_Email);

create table Colors (
    Col_ID serial primary key ,
    Col_Name varchar not null unique ,
    Col_Filename varchar not null unique
);

insert into Colors (Col_Name, Col_Filename)
select 'Pink', 'pink.css' union all
select 'Orange', 'orange.css' union all
select 'Gray', 'gray.css';

alter table public.user add constraint fk_user_color foreign key (Col_ID) references Colors (Col_ID);

-- just initial data
insert into public.User (U_Role, U_PrimaryEmail, u_firstname, u_lastname, Col_ID)
values ('Admin', 'admin@admin', 'admin_firstname', 'admin_lastname', (select col_ID from public.colors where col_name = 'Pink'));

insert into public.UserLogin(U_ID, U_Email, U_Password) values ((select u_id from "user" where U_PrimaryEmail='admin@admin'), 'admin@admin', 'admin');


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
call insert_budget(1, cast (1500.00 as money));

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

create table Expense(
    Ex_ID serial primary key,
    Ex_name varchar not null,
    Cat_ID int not null,
    U_ID int not null,
    AddedDateTime timestamp not null default now(),
    LastModificationDate timestamp not null default now(),
    DateOfPurchase timestamp not null default now(),
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
