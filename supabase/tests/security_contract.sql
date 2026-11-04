-- Run in the SQL editor of an ISOLATED Supabase staging project after all
-- eight 20261103 migrations. Read-only catalog assertions; no test accounts
-- or production data are created or modified.

do $contract$
declare
  v_table text;
  v_enabled boolean;
  v_actual text[];
begin
  foreach v_table in array array[
    'profiles', 'events', 'registrations', 'attendance',
    'notifications', 'schedules', 'grades', 'student_verification_requests'
  ] loop
    select c.relrowsecurity into v_enabled
    from pg_class c where c.oid = format('public.%I', v_table)::regclass;
    if v_enabled is distinct from true then
      raise exception 'RLS is disabled on public.%', v_table;
    end if;
  end loop;

  select array_agg(e.enumlabel::text order by e.enumsortorder) into v_actual
  from pg_enum e where e.enumtypid = 'public.event_status'::regtype;
  if v_actual is distinct from array[
    'DRAFT','PUBLISHED','HAPPENING','COMPLETED','CANCELLED'
  ] then
    raise exception 'Event status enum mismatch: %', v_actual;
  end if;

  if has_column_privilege('authenticated', 'public.profiles', 'role', 'UPDATE')
    or has_column_privilege('authenticated', 'public.profiles', 'student_id', 'UPDATE')
    or has_column_privilege('authenticated', 'public.profiles', 'student_id_verified', 'UPDATE')
    or has_table_privilege('authenticated', 'public.profiles', 'INSERT')
    or has_column_privilege('authenticated', 'public.events', 'current_registered', 'UPDATE')
    or has_column_privilege('authenticated', 'public.events', 'current_registered', 'INSERT')
    or has_table_privilege('authenticated', 'public.events', 'INSERT')
    or has_table_privilege('authenticated', 'public.events', 'UPDATE')
    or has_column_privilege('authenticated', 'public.events', 'title', 'INSERT')
    or has_column_privilege('authenticated', 'public.events', 'title', 'UPDATE')
    or has_column_privilege('authenticated', 'public.events', 'status', 'UPDATE')
    or has_table_privilege('authenticated', 'public.events', 'DELETE')
    or has_table_privilege('authenticated', 'public.registrations', 'INSERT')
    or has_table_privilege('authenticated', 'public.registrations', 'UPDATE')
    or has_table_privilege('authenticated', 'public.registrations', 'DELETE')
    or has_table_privilege('authenticated', 'public.attendance', 'INSERT')
    or has_table_privilege('authenticated', 'public.attendance', 'UPDATE')
    or has_table_privilege('authenticated', 'public.attendance', 'DELETE')
    or has_table_privilege('authenticated', 'public.student_verification_requests', 'INSERT')
    or has_table_privilege('authenticated', 'public.student_verification_requests', 'UPDATE')
    or has_table_privilege('authenticated', 'public.student_verification_requests', 'DELETE') then
    raise exception 'Dangerous write privilege exposed to authenticated';
  end if;

  if has_schema_privilege('authenticated', 'app', 'USAGE')
    or has_schema_privilege('anon', 'app', 'USAGE') then
    raise exception 'Internal app schema is accessible to client roles';
  end if;

  if not exists (
    select 1 from pg_constraint c
    where c.conrelid = 'public.profiles'::regclass
      and c.conname = 'profiles_verified_student_id_present'
      and c.contype = 'c'
  ) then
    raise exception 'Verified student ID constraint missing';
  end if;

  if exists (
    select 1 from pg_constraint c
    where c.conrelid = 'public.events'::regclass and c.contype = 'c'
      and regexp_replace(pg_get_expr(c.conbin, c.conrelid), '[[:space:]()]', '', 'g')
          = 'current_registered<=max_participants'
  ) then
    raise exception 'Legacy zero-capacity CHECK is still installed';
  end if;

  if not exists (
    select 1 from pg_index i join pg_class idx on idx.oid = i.indexrelid
    where i.indrelid = 'public.profiles'::regclass
      and idx.relname = 'profiles_verified_student_id_unique'
      and i.indisunique and i.indisvalid
  ) then
    raise exception 'Verified student ID uniqueness index missing';
  end if;

  if not exists (
    select 1 from pg_constraint c
    where c.conrelid = 'public.events'::regclass
      and c.conname = 'events_capacity_consistent' and c.contype = 'c'
  ) then
    raise exception 'Capacity consistency constraint missing';
  end if;

  if has_function_privilege('anon', 'public.register_for_event(uuid)', 'EXECUTE')
    or has_function_privilege('anon', 'public.cancel_event_registration(uuid)', 'EXECUTE')
    or has_function_privilege('anon', 'public.issue_checkin_token(uuid)', 'EXECUTE')
    or has_function_privilege('anon', 'public.check_in(text)', 'EXECUTE')
    or has_function_privilege('anon', 'public.admin_set_user_role(uuid,public.user_role)', 'EXECUTE')
    or has_function_privilege('anon', 'public.admin_verify_student(uuid,text)', 'EXECUTE')
    or has_function_privilege('anon', 'public.submit_student_verification_request(text)', 'EXECUTE')
    or has_function_privilege('anon', 'public.admin_review_student_verification(uuid,text,text)', 'EXECUTE')
    or has_function_privilege('anon', 'public.create_event(text,text,text,text,timestamptz,timestamptz,integer)', 'EXECUTE')
    or has_function_privilege('anon', 'public.edit_event(uuid,text,text,text,text,timestamptz,timestamptz,integer)', 'EXECUTE')
    or has_function_privilege('anon', 'public.set_event_status(uuid,public.event_status)', 'EXECUTE')
    or not has_function_privilege('authenticated', 'public.check_in(text)', 'EXECUTE')
    or not has_function_privilege('authenticated', 'public.register_for_event(uuid)', 'EXECUTE')
    or not has_function_privilege('authenticated', 'public.create_event(text,text,text,text,timestamptz,timestamptz,integer)', 'EXECUTE')
    or not has_function_privilege('authenticated', 'public.edit_event(uuid,text,text,text,text,timestamptz,timestamptz,integer)', 'EXECUTE')
    or not has_function_privilege('authenticated', 'public.set_event_status(uuid,public.event_status)', 'EXECUTE')
    or not has_function_privilege('authenticated', 'public.submit_student_verification_request(text)', 'EXECUTE')
    or not has_function_privilege('authenticated', 'public.admin_review_student_verification(uuid,text,text)', 'EXECUTE') then
    raise exception 'RPC EXECUTE grants do not match authenticated-only contract';
  end if;

  if exists (
    select 1 from pg_proc p
    where p.oid = any (array[
      'public.register_for_event(uuid)'::regprocedure,
      'public.cancel_event_registration(uuid)'::regprocedure,
      'public.issue_checkin_token(uuid)'::regprocedure,
      'public.check_in(text)'::regprocedure,
      'public.admin_set_user_role(uuid,public.user_role)'::regprocedure,
      'public.admin_verify_student(uuid,text)'::regprocedure,
      'public.create_event(text,text,text,text,timestamptz,timestamptz,integer)'::regprocedure,
      'public.edit_event(uuid,text,text,text,text,timestamptz,timestamptz,integer)'::regprocedure,
      'public.set_event_status(uuid,public.event_status)'::regprocedure,
      'public.submit_student_verification_request(text)'::regprocedure,
      'public.admin_review_student_verification(uuid,text,text)'::regprocedure
    ]) and not p.prosecdef
  ) then
    raise exception 'Privileged RPC is missing SECURITY DEFINER';
  end if;

  if not exists (select 1 from pg_policies
                 where schemaname='public' and tablename='notifications'
                   and policyname='notifications_select')
    or not exists (select 1 from pg_policies
                   where schemaname='public' and tablename='registrations'
                     and policyname='registrations_select')
    or not exists (select 1 from pg_policies
                   where schemaname='public' and tablename='attendance'
                     and policyname='attendance_select')
    or not exists (select 1 from pg_policies
                   where schemaname='public' and tablename='student_verification_requests'
                     and policyname='student_verif_select') then
    raise exception 'Required row ownership / recipient policies missing';
  end if;

  raise notice 'PASS: RLS, event statuses, identity constraint, privileges, and RPC permissions';
end;
$contract$;
