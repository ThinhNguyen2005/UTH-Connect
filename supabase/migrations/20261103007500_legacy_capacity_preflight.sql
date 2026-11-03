-- Run before secure_core_workflows recalculates registration counts.
-- The initial migration used CHECK (current_registered <= max_participants),
-- while the Android model treats max_participants = 0 as unlimited.
-- Keep the original migration intact for databases that already applied it.
-- Match the exact legacy expression, ignoring PostgreSQL's formatting and
-- parentheses, to avoid dropping unrelated capacity/integrity constraints.
do $preflight$
declare v_name name;
begin
  for v_name in
    select c.conname
    from pg_constraint c
    where c.conrelid = 'public.events'::regclass
      and c.contype = 'c'
      and regexp_replace(
        pg_get_expr(c.conbin, c.conrelid), '[[:space:]()]', '', 'g'
      ) = 'current_registered<=max_participants'
  loop
    execute format('alter table public.events drop constraint %I', v_name);
  end loop;
end;
$preflight$;
