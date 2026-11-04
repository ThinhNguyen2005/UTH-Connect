-- Forward-only hardening for projects that already applied earlier migrations.
-- Normalize legacy capacity constraints by expression shape instead of relying
-- on pg_get_constraintdef's whitespace / parenthesization.
do $finalize$
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
$finalize$;

-- A single verified MSSV cannot belong to multiple accounts. Unverified,
-- imported student numbers may still collide until verified by an admin.
-- If verified duplicates already exist, this statement intentionally fails
-- so administrators can investigate rather than silently reassign identity.
create unique index if not exists profiles_verified_student_id_unique
  on public.profiles (upper(btrim(student_id)))
  where student_id_verified;
