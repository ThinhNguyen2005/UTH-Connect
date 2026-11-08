import type { Config } from "tailwindcss";

const config: Config = {
  content: [
    "./src/pages/**/*.{js,ts,jsx,tsx,mdx}",
    "./src/components/**/*.{js,ts,jsx,tsx,mdx}",
    "./src/app/**/*.{js,ts,jsx,tsx,mdx}",
  ],
  theme: {
    extend: {
      colors: {
        uth: {
          navy: {
            DEFAULT: "#0F5FC2",
            dark: "#003366",
            light: "#2575D6",
            subtle: "#EBF3FC",
          },
          amber: {
            DEFAULT: "#F59E0B",
            light: "#FEF3C7",
            dark: "#92400E",
          },
          slate: {
            50: "#F8FAFC",
            100: "#F1F5F9",
            200: "#E2E8F0",
            300: "#CBD5E1",
            400: "#94A3B8",
            500: "#64748B",
            600: "#475569",
            900: "#0F172A",
          },
        },
      },
      borderRadius: {
        sm: "4px",
        DEFAULT: "6px",
        md: "8px",
        lg: "8px", // Con số vàng cho Input/Button
        xl: "12px", // Chuẩn cho Card
        "2xl": "16px", // Giới hạn tối đa cho Modal
      },
      boxShadow: {
        ambient: "0 4px 20px -2px rgba(15, 23, 42, 0.04), 0 2px 6px -1px rgba(15, 23, 42, 0.02)",
        subtle: "0 1px 3px 0 rgba(15, 23, 42, 0.03), 0 1px 2px -1px rgba(15, 23, 42, 0.02)",
      },
    },
  },
  plugins: [],
};

export default config;
