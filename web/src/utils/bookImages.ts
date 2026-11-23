import { supabaseBrowserClient } from "./supabase-client";

export const BOOK_STORAGE_BUCKET = "book_listings";

/**
 * Lấy URL công khai của ảnh sách từ Supabase Storage bucket `book_listings`
 */
export function getBookImageUrl(imagePath?: string | null): string | null {
  if (!imagePath || typeof imagePath !== "string") return null;

  // Nếu đã là URL đầy đủ (http/https)
  if (imagePath.startsWith("http://") || imagePath.startsWith("https://")) {
    return imagePath;
  }

  const { data } = supabaseBrowserClient.storage
    .from(BOOK_STORAGE_BUCKET)
    .getPublicUrl(imagePath);

  return data?.publicUrl || null;
}

/**
 * Upload ảnh bìa sách lên bucket `book_listings` theo cấu trúc: `<userId>/<listingId>/<random>.ext`
 */
export async function uploadBookImage(
  file: File,
  userId: string,
  listingId: string
): Promise<string> {
  const allowedTypes = ["image/jpeg", "image/png", "image/webp"];
  if (!allowedTypes.includes(file.type)) {
    throw new Error("Chỉ hỗ trợ định dạng ảnh JPEG, PNG hoặc WebP.");
  }

  // Giới hạn 5MB theo cấu hình bucket
  if (file.size > 5 * 1024 * 1024) {
    throw new Error("Dung lượng ảnh không được vượt quá 5MB.");
  }

  const ext = file.name.split(".").pop()?.toLowerCase() || "jpg";
  const randomName = `${Date.now()}-${Math.random().toString(36).substring(2, 9)}.${ext}`;
  const objectPath = `${userId}/${listingId}/${randomName}`;

  const { error } = await supabaseBrowserClient.storage
    .from(BOOK_STORAGE_BUCKET)
    .upload(objectPath, file, {
      cacheControl: "3600",
      upsert: false,
    });

  if (error) {
    throw new Error(error.message || "Không thể tải ảnh lên máy chủ.");
  }

  return objectPath;
}

/**
 * Xóa ảnh sách khỏi bucket nếu người dùng xóa tin
 */
export async function deleteBookImage(imagePath?: string | null): Promise<void> {
  if (!imagePath || typeof imagePath !== "string") return;
  if (imagePath.startsWith("http://") || imagePath.startsWith("https://")) return;

  try {
    await supabaseBrowserClient.storage
      .from(BOOK_STORAGE_BUCKET)
      .remove([imagePath]);
  } catch {
    // Không chặn luồng nếu dọn ảnh storage thất bại
  }
}
