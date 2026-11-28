-- Preserve the existing personal notification types and allow incident events.
create or replace function public.validate_personal_notification_type()
returns trigger language plpgsql set search_path = '' as $$
declare
    should_validate_type boolean;
begin
    if tg_op = 'INSERT' then
        should_validate_type := true;
    else
        should_validate_type := old.type is distinct from new.type;
    end if;
    if should_validate_type and (
        new.type is null or new.type not in (
            'HOUSING_APPROVED', 'HOUSING_REJECTED', 'EVENT_REMINDER', 'SYSTEM',
            'INCIDENT_SUBMITTED', 'INCIDENT_STATUS_UPDATED', 'INCIDENT_RESOLVED', 'INCIDENT_REJECTED'
        )
    ) then
        raise exception 'Unsupported personal notification type: %', new.type using errcode = '23514';
    end if;
    return new;
end;
$$;

-- A locked row and expected version prevent two operators from silently
-- overwriting one another. Only this function changes the workflow state.
create function public.transition_incident_report(
    p_incident_id uuid,
    p_expected_version integer,
    p_target_status text,
    p_note text default null,
    p_duplicate_of_id uuid default null
) returns integer
language plpgsql security definer set search_path = '' as $$
declare
    v_user_id uuid := auth.uid();
    v_report public.incident_reports%rowtype;
    v_is_admin boolean;
    v_owner_action boolean;
    v_admin_action boolean;
    v_type text;
begin
    if v_user_id is null or coalesce((auth.jwt() ->> 'is_anonymous')::boolean, false) then
        raise exception 'INCIDENT_UNAUTHENTICATED' using errcode = '42501';
    end if;
    v_is_admin := coalesce(public.is_admin(), false);
    select * into v_report from public.incident_reports r where r.id = p_incident_id for update;
    if not found or (v_report.reporter_id <> v_user_id and not v_is_admin) then
        raise exception 'INCIDENT_NOT_FOUND' using errcode = 'P0002';
    end if;
    if p_expected_version is null or v_report.row_version <> p_expected_version then
        raise exception 'INCIDENT_CONFLICT' using errcode = 'P0001';
    end if;

    v_owner_action := v_report.reporter_id = v_user_id and (
        (v_report.status = 'SUBMITTED' and p_target_status = 'CANCELLED') or
        (v_report.status = 'RESOLVED' and p_target_status = 'CLOSED') or
        (v_report.status = 'RESOLVED' and p_target_status = 'REOPENED'
            and v_report.resolved_at is not null and now() <= v_report.resolved_at + interval '72 hours')
    );
    v_admin_action := v_is_admin and (
        (v_report.status = 'SUBMITTED' and p_target_status in ('ACKNOWLEDGED', 'REJECTED', 'DUPLICATE')) or
        (v_report.status in ('ACKNOWLEDGED', 'REOPENED') and p_target_status = 'IN_PROGRESS') or
        (v_report.status = 'IN_PROGRESS' and p_target_status = 'RESOLVED')
    );
    if not coalesce(v_owner_action, false) and not coalesce(v_admin_action, false) then
        raise exception 'INCIDENT_INVALID_TRANSITION' using errcode = '22023';
    end if;
    if p_note is not null and char_length(btrim(p_note)) > 1000 then
        raise exception 'INCIDENT_NOTE_TOO_LONG' using errcode = '22023';
    end if;
    if p_target_status in ('REJECTED', 'DUPLICATE', 'RESOLVED', 'REOPENED') and nullif(btrim(p_note), '') is null then
        raise exception 'INCIDENT_NOTE_REQUIRED' using errcode = '22023';
    end if;
    if p_target_status = 'DUPLICATE' then
        if p_duplicate_of_id is null or p_duplicate_of_id = p_incident_id or not exists (
            select 1 from public.incident_reports r where r.id = p_duplicate_of_id
        ) then
            raise exception 'INCIDENT_INVALID_DUPLICATE' using errcode = '22023';
        end if;
    elsif p_duplicate_of_id is not null then
        raise exception 'INCIDENT_INVALID_DUPLICATE' using errcode = '22023';
    end if;

    update public.incident_reports
       set status = p_target_status,
           row_version = row_version + 1,
           updated_at = now(),
           resolved_at = case when p_target_status = 'RESOLVED' then now() else resolved_at end,
           closed_at = case when p_target_status = 'CLOSED' then now() else closed_at end,
           resolution_note = case when p_target_status = 'RESOLVED' then btrim(p_note) else resolution_note end,
           rejection_reason = case when p_target_status in ('REJECTED', 'DUPLICATE') then btrim(p_note) else rejection_reason end,
           duplicate_of_id = case when p_target_status = 'DUPLICATE' then p_duplicate_of_id else duplicate_of_id end
     where id = p_incident_id;
    insert into public.incident_status_logs (incident_id, from_status, to_status, actor_id, note)
      values (p_incident_id, v_report.status, p_target_status, v_user_id, nullif(btrim(p_note), ''));

    if v_admin_action then
        v_type := case
            when p_target_status = 'RESOLVED' then 'INCIDENT_RESOLVED'
            when p_target_status in ('REJECTED', 'DUPLICATE') then 'INCIDENT_REJECTED'
            else 'INCIDENT_STATUS_UPDATED'
        end;
        insert into public.notifications (recipient_id, title, content, type)
          values (v_report.reporter_id, 'Cập nhật báo cáo sự cố',
                  'Phiếu ' || v_report.code || ' đã chuyển sang ' || p_target_status || '.', v_type);
    end if;
    return v_report.row_version + 1;
end;
$$;

revoke all on function public.transition_incident_report(uuid, integer, text, text, uuid) from public, anon, authenticated;
grant execute on function public.transition_incident_report(uuid, integer, text, text, uuid) to authenticated;

-- Public campus feed deliberately exposes no reporter identity or description.
create function public.list_open_incidents(p_campus text default null)
returns table (
    id uuid, code text, campus text, room_code text, category text, severity text,
    title text, status text, created_at timestamptz, confirmation_count bigint
)
language plpgsql stable security definer set search_path = '' as $$
begin
    if auth.uid() is null or coalesce((auth.jwt() ->> 'is_anonymous')::boolean, false) then
        raise exception 'INCIDENT_UNAUTHENTICATED' using errcode = '42501';
    end if;
    return query
      select r.id, r.code, r.campus, r.room_code, r.category, r.severity,
             r.title, r.status, r.created_at,
             1 + (select count(*) from public.incident_confirmations c where c.incident_id = r.id)
      from public.incident_reports r
      where r.status in ('SUBMITTED', 'ACKNOWLEDGED', 'IN_PROGRESS', 'REOPENED')
        and (p_campus is null or r.campus = btrim(p_campus))
      order by r.created_at desc, r.id desc;
end;
$$;
revoke all on function public.list_open_incidents(text) from public, anon, authenticated;
grant execute on function public.list_open_incidents(text) to authenticated;

create function public.confirm_incident_report(p_incident_id uuid) returns bigint
language plpgsql security definer set search_path = '' as $$
declare
    v_user_id uuid := auth.uid();
    v_report public.incident_reports%rowtype;
    v_count bigint;
begin
    if v_user_id is null or coalesce((auth.jwt() ->> 'is_anonymous')::boolean, false) then
        raise exception 'INCIDENT_UNAUTHENTICATED' using errcode = '42501';
    end if;
    select * into v_report from public.incident_reports r where r.id = p_incident_id for update;
    if not found or v_report.status not in ('SUBMITTED', 'ACKNOWLEDGED', 'IN_PROGRESS', 'REOPENED') then
        raise exception 'INCIDENT_NOT_FOUND' using errcode = 'P0002';
    end if;
    if v_report.reporter_id = v_user_id then
        raise exception 'INCIDENT_SELF_CONFIRM' using errcode = '22023';
    end if;
    insert into public.incident_confirmations (incident_id, user_id)
      values (p_incident_id, v_user_id) on conflict (incident_id, user_id) do nothing;
    select 1 + count(*) into v_count from public.incident_confirmations c where c.incident_id = p_incident_id;
    return v_count;
end;
$$;
revoke all on function public.confirm_incident_report(uuid) from public, anon, authenticated;
grant execute on function public.confirm_incident_report(uuid) to authenticated;

create function public.assign_incident_report(
    p_incident_id uuid,
    p_expected_version integer,
    p_assigned_to uuid,
    p_assigned_unit text
) returns integer
language plpgsql security definer set search_path = '' as $$
declare
    v_user_id uuid := auth.uid();
    v_report public.incident_reports%rowtype;
    v_unit text := nullif(btrim(p_assigned_unit), '');
begin
    if v_user_id is null or coalesce((auth.jwt() ->> 'is_anonymous')::boolean, false)
       or not coalesce(public.is_admin(), false) then
        raise exception 'INCIDENT_FORBIDDEN' using errcode = '42501';
    end if;
    select * into v_report from public.incident_reports r where r.id = p_incident_id for update;
    if not found then
        raise exception 'INCIDENT_NOT_FOUND' using errcode = 'P0002';
    end if;
    if p_expected_version is null or v_report.row_version <> p_expected_version then
        raise exception 'INCIDENT_CONFLICT' using errcode = 'P0001';
    end if;
    if v_report.status not in ('SUBMITTED', 'ACKNOWLEDGED', 'IN_PROGRESS', 'REOPENED') then
        raise exception 'INCIDENT_INVALID_TRANSITION' using errcode = '22023';
    end if;
    if (p_assigned_to is null and v_unit is null) or char_length(coalesce(v_unit, '')) > 200 then
        raise exception 'INCIDENT_INVALID_ASSIGNMENT' using errcode = '22023';
    end if;
    if p_assigned_to is not null and not exists (
        select 1 from public.profiles p where p.id = p_assigned_to and p.role::text = 'admin'
    ) then
        raise exception 'INCIDENT_INVALID_ASSIGNMENT' using errcode = '22023';
    end if;
    update public.incident_reports
      set assigned_to = p_assigned_to, assigned_unit = v_unit,
          row_version = row_version + 1, updated_at = now()
      where id = p_incident_id;
    insert into public.incident_status_logs (incident_id, from_status, to_status, actor_id, note)
      values (p_incident_id, v_report.status, v_report.status, v_user_id, 'Đã phân công xử lý');
    insert into public.notifications (recipient_id, title, content, type)
      values (v_report.reporter_id, 'Cập nhật báo cáo sự cố',
              'Phiếu ' || v_report.code || ' đã được phân công xử lý.', 'INCIDENT_STATUS_UPDATED');
    return v_report.row_version + 1;
end;
$$;
revoke all on function public.assign_incident_report(uuid, integer, uuid, text) from public, anon, authenticated;
grant execute on function public.assign_incident_report(uuid, integer, uuid, text) to authenticated;
