/** @type {import('next').NextConfig} */
const nextConfig = {
  reactStrictMode: true,
  images: {
    remotePatterns: [
      {
        protocol: "https",
        hostname: "lh3.googleusercontent.com",
      },
      {
        protocol: "https",
        hostname: "bvxilfcctwmgmrwrncvd.supabase.co",
      },
    ],
  },
  async redirects() {
    return [
      {
        source: "/users",
        destination: "/admin/users",
        permanent: true,
      },
    ];
  },
};

export default nextConfig;
