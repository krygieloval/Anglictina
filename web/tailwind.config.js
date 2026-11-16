/** @type {import('tailwindcss').Config} */
export default {
  content: ['./index.html', './src/**/*.{js,ts,jsx,tsx}'],
  theme: {
    extend: {
      fontFamily: {
        display: ['"Baloo 2"', 'Nunito', 'system-ui', 'sans-serif'],
        sans: ['Nunito', 'system-ui', 'sans-serif'],
      },
      colors: {
        brand: {
          indigo: '#3F45D7',
          indigoLight: '#E6E8FF',
          indigoDark: '#1A1E7A',
          coral: '#FF6B4A',
          coralLight: '#FFE5DF',
          coralDark: '#7D2210',
          mint: '#0EA572',
          mintLight: '#D5F9EC',
          mintDark: '#065037',
          amber: '#F59E0B',
          amberLight: '#FEF3C7',
          amberDark: '#78350F',
          skyBg: '#F8FAFF',
          ink: '#1E293B',
          muted: '#526079',
          outline: '#D0D7F0',
        },
      },
      keyframes: {
        'mic-pulse': {
          '0%, 100%': { transform: 'scale(1)', opacity: '0.35' },
          '50%': { transform: 'scale(1.22)', opacity: '0.15' },
        },
      },
      animation: {
        'mic-pulse': 'mic-pulse 1.2s ease-in-out infinite',
      },
    },
  },
  plugins: [],
};
