-- A deleted event or account must not silently erase attendance history.
-- Retention/privacy requests require a separate reviewed administrative workflow.
alter table public.attendance
  drop constraint attendance_event_id_fkey,
  add constraint attendance_event_id_fkey
    foreign key (event_id) references public.events(id) on delete restrict;

alter table public.attendance
  drop constraint attendance_user_id_fkey,
  add constraint attendance_user_id_fkey
    foreign key (user_id) references public.profiles(id) on delete restrict;

alter table public.attendance
  drop constraint attendance_checked_in_by_fkey,
  add constraint attendance_checked_in_by_fkey
    foreign key (checked_in_by) references public.profiles(id) on delete restrict;
