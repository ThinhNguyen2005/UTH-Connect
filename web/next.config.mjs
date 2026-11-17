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
  webpack: (config, { dev }) => {
    if (dev) {
      // Tăng timeout tải chunk cho Webpack dev server tránh lỗi ChunkLoadError trên Windows
      config.output.chunkLoadTimeout = 300000;
    }
    return config;
  },
};

export default nextConfig;
