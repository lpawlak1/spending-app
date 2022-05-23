DO
$$
begin
    for i in 1..100000 LOOP
        INSERT INTO public.expense(ex_name, cat_id, u_id, dateofpurchase, price, deleted)
            VALUES ('test_ex_name' || i, i%20, 1, now() - i%3 * interval '1 month', 100.0, false);
    end loop;
end
$$;

\timing on
SET auto_explain.log_nested_statements = ON;
SET auto_explain.log_analyze  = true;
LOAD 'auto_explain';
set client_min_messages TO log;

SET enable_seqscan = off;
SET enable_seqscan = on;

-- Manualne podejście wykorzystujące generate_series
do
$$
    begin;
        insert into public.user (u_id, u_role, registrationdate, u_primaryemail, u_firstname, u_lastname, col_id)
        select id,
               'User',
               NOW() - '1 day'::INTERVAL * (RANDOM()::int * 100 + 100),
               'user' || id || '@spending-app.com',
               substr(md5(random()::text), 0, 20),
               substr(md5(random()::text), 0, 20),
               floor(random() * 3 + 1)::int
        from generate_series(4, 100000) as id;
        commit;
    end;
$$;

DO
$$
    BEGIN
        for i in 1..100000
            LOOP
                INSERT INTO public.expense(ex_name, cat_id, u_id, description, price, deleted)
                values ('test_ex_name' || i, i % 20, 1, now() - i % 3 * interval '1 month', 100.0, false);
            end loop;
        COMMIT;
    END;
$$;

