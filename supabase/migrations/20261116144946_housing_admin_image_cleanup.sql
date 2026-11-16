-- Cho admin xóa tệp ảnh housing-images qua Storage API khi xóa tin trọ.
-- Khắc phục shortcut "Storage files are retained" trong 20261114111909_housing_admin_actions.sql:
-- web admin gọi storage.remove(paths) bằng JWT admin trước khi gọi delete_housing_listing.
-- Ủy quyền server-side bằng public.is_admin(); không cần secret key hay edge function.
begin;

drop policy if exists housing_images_admin_delete on storage.objects;
create policy housing_images_admin_delete on storage.objects
  for delete to authenticated
  using (bucket_id = 'housing-images' and public.is_admin());

commit;