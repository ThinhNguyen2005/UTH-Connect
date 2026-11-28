-- Run only after the KAN-182 migration on an isolated test database.
-- This checks catalog privileges and does not replace real-user RLS tests.
do $contract$
declare
  table_name text;
begin
  foreach table_name in array array['incident_reports', 'incident_status_logs', 'incident_confirmations'] loop
    if not exists (
      select 1 from pg_catalog.pg_class c
      join pg_catalog.pg_namespace n on n.oid = c.relnamespace
      where n.nspname = 'public' and c.relname = table_name and c.relrowsecurity
    ) then
      raise exception 'Missing table or RLS disabled: %', table_name;
    end if;

    if has_table_privilege('anon', format('public.%I', table_name), 'SELECT')
      or has_table_privilege('authenticated', format('public.%I', table_name), 'INSERT')
      or has_table_privilege('authenticated', format('public.%I', table_name), 'UPDATE')
      or has_table_privilege('authenticated', format('public.%I', table_name), 'DELETE') then
      raise exception 'Unexpected client privilege on %', table_name;
    end if;
  end loop;
end;
$contract$;
