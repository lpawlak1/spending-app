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

