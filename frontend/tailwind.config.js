/** @type {import('tailwindcss').Config} */
export default {
  darkMode: 'class', // Enable class-based dark mode
  content: [
    "./index.html",
    "./src/**/*.{vue,js,ts,jsx,tsx}",
  ],
  theme: {
    extend: {
      colors: {
        // Custom premium palette extensions if needed
        gray: {
          750: '#2d3748', // Custom shade
        }
      }
    },
  },
  plugins: [],
}
