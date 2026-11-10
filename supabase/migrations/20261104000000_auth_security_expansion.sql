-- Migration: 20261104000000_auth_security_expansion.sql
-- Enforces server-side email domain check on auth.users, adds contact fields to profiles,
-- and provisions student verification requests table and RPCs with zero profile update privilege leakage.

-- 1. Server-side Domain Enforcement Trigger on auth.users
create or replace function public.check_user_email_domain()
returns trigger language plpgsql security definer set search_path = '' as $$
begin
  if new.email is null or (
    lower(new.email) not like '%@ut.edu.vn' and
    lower(new.email) not like '%@uth.edu.vn'
  ) then
    raise exception 'DOMAIN_RESTRICTION: Only @ut.edu.vn or @uth.edu.vn addresses are allowed'
      using errcode = '23514';
  end if;
  return new;
end;
$$;

drop trigger if exists enforce_user_email_domain on auth.users;
create trigger enforce_user_email_domain
  before insert or update of email on auth.users
  for each row execute function public.check_user_email_domain();

revoke execute on function public.check_user_email_domain() from public, anon, authenticated;

-- 2. Add contact fields to public.profiles and grant column-level update
alter table public.profiles
  add column if not exists personal_email text,
  add column if not exists current_address text;

grant update (display_name, avatar_url, phone_number, personal_email, current_address)
  on public.profiles to authenticated;

-- 3. Dedicated student verification requests table
create table if not exists public.student_verification_requests (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null references public.profiles(id) on delete cascade,
  student_id text not null check (length(btrim(student_id)) between 5 and 32),
  status text not null default 'PENDING' check (status in ('PENDING', 'APPROVED', 'REJECTED')),
  rejection_reason text,
  created_at timestamptz not null default now(),
  reviewed_at timestamptz,
  reviewed_by uuid references public.profiles(id)
);

create index if not exists idx_student_verif_user on public.student_verification_requests(user_id);
create unique index if not exists idx_student_verif_pending_unique 
  on public.student_verification_requests(user_id) where status = 'PENDING';

alter table public.student_verification_requests enable row level security;

-- Authenticated students only see their own requests; admins can see all requests
create policy student_verif_select on public.student_verification_requests
  for select to authenticated
  using (user_id = (select auth.uid()) or (select public.is_admin()));

-- Revoke all table write privileges from authenticated role (writes happen strictly via security definer RPC)
revoke insert, update, delete on public.student_verification_requests from authenticated;

-- 4. RPC for students to submit verification request
create or replace function public.submit_student_verification_request(p_student_id text)
returns jsonb language plpgsql security definer set search_path = '' as $$
declare
  v_uid uuid := (select auth.uid());
  v_student_id text := btrim(p_student_id);
  v_is_verified boolean;
  v_req_id uuid;
begin
  if v_uid is null then raise exception 'AUTH_REQUIRED' using errcode = '42501'; end if;
  
  if v_student_id is null or length(v_student_id) not between 5 and 32 then
    raise exception 'INVALID_STUDENT_ID' using errcode = '22023';
  end if;

  select student_id_verified into v_is_verified from public.profiles where id = v_uid;
  if v_is_verified then
    raise exception 'STUDENT_ALREADY_VERIFIED' using errcode = '42501';
  end if;

  if exists (select 1 from public.profiles where student_id_verified and upper(btrim(student_id)) = upper(v_student_id)) then
    raise exception 'STUDENT_ID_ALREADY_TAKEN' using errcode = '23505';
  end if;

  insert into public.student_verification_requests(user_id, student_id, status)
  values (v_uid, v_student_id, 'PENDING')
  returning id into v_req_id;

  return jsonb_build_object('request_id', v_req_id, 'status', 'PENDING');
end;
$$;

revoke execute on function public.submit_student_verification_request(text) from public, anon;
grant execute on function public.submit_student_verification_request(text) to authenticated;

-- 5. RPC for admin to review verification request
create or replace function public.admin_review_student_verification(
  p_request_id uuid,
  p_decision text,
  p_reason text default null
)
returns void language plpgsql security definer set search_path = '' as $$
declare
  v_req record;
begin
  if (select auth.uid()) is null or not public.is_admin() then
    raise exception 'FORBIDDEN' using errcode = '42501';
  end if;

  select * into v_req from public.student_verification_requests where id = p_request_id for update;
  if not found then raise exception 'REQUEST_NOT_FOUND' using errcode = 'P0002'; end if;

  if v_req.status is distinct from 'PENDING' then
    raise exception 'REQUEST_ALREADY_RESOLVED' using errcode = '42501';
  end if;

  if p_decision = 'APPROVED' then
    perform public.admin_verify_student(v_req.user_id, v_req.student_id);
    update public.student_verification_requests
    set status = 'APPROVED', reviewed_at = now(), reviewed_by = (select auth.uid())
    where id = p_request_id;
  elsif p_decision = 'REJECTED' then
    update public.student_verification_requests
    set status = 'REJECTED', rejection_reason = p_reason, reviewed_at = now(), reviewed_by = (select auth.uid())
    where id = p_request_id;
  else
    raise exception 'INVALID_DECISION' using errcode = '22023';
  end if;
end;
$$;

revoke execute on function public.admin_review_student_verification(uuid, text, text) from public, anon;
grant execute on function public.admin_review_student_verification(uuid, text, text) to authenticated;

