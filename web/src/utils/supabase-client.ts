import { createBrowserClient } from "@supabase/ssr";

const supabaseUrl = process.env.NEXT_PUBLIC_SUPABASE_URL || "https://bvxilfcctwmgmrwrncvd.supabase.co";
const supabaseAnonKey = process.env.NEXT_PUBLIC_SUPABASE_ANON_KEY || "sb_publishable_-tCoKaUA7cXMETDFwAnOjw_Z-VOPzTo";

export const supabaseBrowserClient = createBrowserClient(
  supabaseUrl,
  supabaseAnonKey
);
