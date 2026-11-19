/** @type {import('tailwindcss').Config} */
export default {
  content: ["./index.html", "./src/**/*.{js,jsx}"],
  theme: {
    extend: {
      fontFamily: {
        sans: ["Inter", "ui-sans-serif", "system-ui", "sans-serif"]
      },
      colors: {
        ink: "#202020",
        muted: "#77827d",
        paper: "#ffffff",
        forest: "#236b52",
        lime: "#ff9933",
        line: "#e7ebe7"
      },
      boxShadow: {
        card: "0 16px 50px rgba(27, 46, 37, 0.07)"
      }
    }
  },
  plugins: []
};
