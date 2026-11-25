-- event_comments: bình luận của sinh viên dưới các bài viết sự kiện
create table if not exists public.event_comments (
  id                uuid primary key default gen_random_uuid(),
  event_id          uuid not null references public.events (id) on delete cascade,
  user_id           uuid not null references public.profiles (id) on delete cascade,
  student_id        text not null default '',
  author_name       text not null default '',
  author_class      text not null default '',
  author_major      text not null default '',
  author_avatar_url text,
  content           text not null,
  created_at        timestamptz not null default now()
);

create index if not exists event_comments_event_id_idx on public.event_comments (event_id, created_at asc);
create index if not exists event_comments_user_id_idx on public.event_comments (user_id);

alter table public.event_comments enable row level security;

-- Mọi sinh viên đã đăng nhập đều đọc được bình luận sự kiện
create policy event_comments_select on public.event_comments
  for select to authenticated using (true);

-- Sinh viên chỉ được tạo bình luận dưới danh tính của chính mình
create policy event_comments_insert on public.event_comments
  for insert to authenticated with check ((select auth.uid()) = user_id);

-- Sinh viên có thể xoá bình luận của chính mình, hoặc admin có thể xoá
create policy event_comments_delete on public.event_comments
  for delete to authenticated using ((select auth.uid()) = user_id or public.is_admin());
