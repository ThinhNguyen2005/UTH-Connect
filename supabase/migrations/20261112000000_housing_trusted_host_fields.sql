-- ============================================================================
-- UTH Connect — Ghép trọ: khóa trường danh tính chủ tin vào dữ liệu tin cậy
-- Migration: 20261112000000_housing_trusted_host_fields.sql
-- ----------------------------------------------------------------------------
-- Review CodeRabbit 12/11: policy insert/update chỉ giữ author_id nên chủ tin
-- có thể tự gửi verified=true / verification_method / host_student_id của người
-- khác để giả mạo "chủ tin đã xác minh". Trigger BEFORE INSERT OR UPDATE ghi đè
-- ba trường này từ profiles (student_id, student_id_verified) — nguồn tin cậy
-- duy nhất. RPC security definer (mark fulfilled / report) không đổi ba trường
-- này nên đi qua trigger bình thường.
-- ============================================================================

begin;

create or replace function public.trg_housing_trusted_host()
returns trigger
language plpgsql security definer set search_path = ''
as $$
begin
    select btrim(p.student_id), p.student_id_verified
    into new.host_student_id, new.verified
    from public.profiles p
    where p.id = new.author_id;
    new.host_student_id := coalesce(new.host_student_id, '');
    new.verified := coalesce(new.verified, false);
    new.verification_method := case when new.verified then 'EMAIL_UTH' else null end;
    return new;
end;
$$;

drop trigger if exists housing_trusted_host on public.housing_posts;
create trigger housing_trusted_host
    before insert or update on public.housing_posts
    for each row execute function public.trg_housing_trusted_host();

-- Tin tạo trước khi trigger tồn tại vẫn giữ giá trị danh tính client tự khai:
-- chuẩn hóa một lần từ profiles cho khớp trigger (idempotent, chạy lại cho kết
-- quả cũ; tin không còn profile tương ứng thì giữ nguyên).
update public.housing_posts h
set host_student_id = coalesce(btrim(p.student_id), ''),
    verified = coalesce(p.student_id_verified, false),
    verification_method = case when coalesce(p.student_id_verified, false) then 'EMAIL_UTH' else null end
from public.profiles p
where p.id = h.author_id;

commit;