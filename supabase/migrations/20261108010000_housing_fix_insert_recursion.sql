-- ============================================================================
-- UTH Connect — Ghép trọ: sửa đệ quy RLS trong policy insert
-- Migration: 20261108010000_housing_fix_insert_recursion.sql
-- ----------------------------------------------------------------------------
-- Policy insert của 20261108000000 đếm tin OPEN bằng subquery trên chính
-- housing_posts; subquery đó chịu RLS của bảng và gây "infinite recursion
-- detected in policy for relation housing_posts" (42P17) khi insert.
-- Đưa phần đếm vào hàm SECURITY DEFINER (chủ sở hữu bỏ qua RLS) để phá vòng.
-- ============================================================================

begin;

create or replace function public.housing_open_post_count(p_author_id uuid)
returns integer
language sql stable security definer set search_path = ''
as $$
    select count(*)::integer from public.housing_posts
    where author_id = p_author_id and status = 'OPEN';
$$;

revoke all on function public.housing_open_post_count(uuid) from public, anon;
grant execute on function public.housing_open_post_count(uuid) to authenticated;

alter policy housing_posts_insert on public.housing_posts
    with check (
        author_id = (select auth.uid())
        and public.housing_open_post_count((select auth.uid())) < 2
    );

commit;