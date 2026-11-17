const BUCKET = "housing-images";
const PUBLIC_URL = /^https:\/\/[a-z0-9-]+\.supabase\.co\/storage\/v1\/object\/public\/housing-images\//;

export function housingImagePaths(
  images: string[] | null | undefined,
  expectedOrigin?: string
): string[] {
  if (!images) return [];
  const paths: string[] = [];
  for (const url of images) {
    if (typeof url !== "string" || !PUBLIC_URL.test(url)) continue;
    if (expectedOrigin) {
      try {
        const parsed = new URL(url);
        const expected = new URL(expectedOrigin);
        if (parsed.origin !== expected.origin) {
          throw new Error(`HOUSING_IMAGE_ORIGIN_MISMATCH: ${url}`);
        }
      } catch (err: any) {
        if (err.message?.startsWith("HOUSING_IMAGE_ORIGIN_MISMATCH")) {
          throw err;
        }
        throw new Error(`HOUSING_IMAGE_ORIGIN_MISMATCH: ${url}`);
      }
    }
    const rawPath = url.replace(PUBLIC_URL, "").split("?")[0];
    const objectPath = decodeURIComponent(rawPath);
    if (objectPath.includes("..")) {
      throw new Error(`HOUSING_IMAGE_PATH_INVALID: ${url}`);
    }
    if (objectPath) paths.push(objectPath);
  }
  return paths;
}

// Ảnh phải xóa trước bản ghi: nếu xóa ảnh lỗi thì dừng, tin còn nguyên (không tạo ảnh mồ côi công khai).
export async function deleteListingWithImages(
  client: any,
  listingId: string,
  images: string[] | null | undefined
): Promise<void> {
  const expectedOrigin = client?.supabaseUrl;
  const paths = housingImagePaths(images, expectedOrigin);
  if (paths.length) {
    const storageApi = client.storage.from(BUCKET);
    const { data, error } = await storageApi.remove(paths);
    if (error) throw error;
    const deleted = new Set((data || []).map((file: { name: string }) => file.name));
    const unconfirmed = paths.filter(path => !deleted.has(path));

    if (unconfirmed.length > 0) {
      if (typeof storageApi.list !== "function") {
        throw new Error("HOUSING_IMAGE_DELETE_INCOMPLETE");
      }
      for (const path of unconfirmed) {
        const lastSlash = path.lastIndexOf("/");
        const folder = lastSlash >= 0 ? path.substring(0, lastSlash) : "";
        const fileName = lastSlash >= 0 ? path.substring(lastSlash + 1) : path;
        const { data: listed, error: listError } = await storageApi.list(folder, { search: fileName });
        if (listError) throw listError;
        if (listed?.some((f: { name: string }) => f.name === fileName)) {
          throw new Error("HOUSING_IMAGE_DELETE_INCOMPLETE");
        }
      }
    }
  }
  const { error } = await client.rpc("delete_housing_listing", { p_listing_id: listingId });
  if (error) throw error;
}
