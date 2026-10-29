/** @type {import('tailwindcss').Config} */
export default {
  content: ["./index.html", "./src/**/*.{ts,tsx}"],
  theme: {
    extend: {
      fontFamily: {
        sans: ["\"Source Sans 3\"", "Segoe UI", "system-ui", "sans-serif"],
        display: ["\"Outfit\"", "Segoe UI", "system-ui", "sans-serif"],
      },
      colors: {
        ink: {
          950: "#070b14",
          900: "#0b1220",
          800: "#111827",
          700: "#1a2333",
        },
        accent: {
          cyan: "#38bdf8",
          violet: "#818cf8",
        },
      },
    },
  },
  plugins: [],
};
