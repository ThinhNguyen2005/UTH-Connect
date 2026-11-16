const BUCKET = "housing-images";
const PUBLIC_URL = /^https:\/\/[a-z0-9-]+\.supabase\.co\/storage\/v1\/object\/public\/housing-images\//;

export function housingImagePaths(images: string[] | null | undefined): string[] {
  if (!images) return [];
  const paths: string[] = [];
  for (const url of images) {
    if (typeof url !== "string" || !PUBLIC_URL.test(url)) continue;
    const objectPath = decodeURIComponent(url.replace(PUBLIC_URL, "").split("?")[0]);
    if (objectPath) paths.push(objectPath);
  }
  return paths;
}

// Ảnh phải xóa trước bản ghi: nếu xóa ảnh lỗi thì dừng, tin còn nguyên (không tạo ảnh mồ côi công khai).
export async function deleteListingWithImages(client: any, listingId: string, images: string[] | null | undefined): Promise<void> {
  const paths = housingImagePaths(images);
  if (paths.length) {
    const { error } = await client.storage.from(BUCKET).remove(paths);
    if (error) throw error;
  }
  const { error } = await client.rpc("delete_housing_listing", { p_listing_id: listingId });
  if (error) throw error;
}