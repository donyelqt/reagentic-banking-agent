/** @type {import('tailwindcss').Config} */
export default {
  content: ['./index.html', './src/**/*.{ts,tsx}'],
  theme: {
    extend: {
      colors: {
        ink: '#14130F',
        muted: '#6E6C64',
        line: '#E7E5DD',
        bg: '#F6F5F1',
        surface: '#FFFFFF',
        accent: { DEFAULT: '#2D43F5', soft: 'rgba(45,67,245,0.10)' },
        pos: '#0B8A63',
        neg: '#C9323C',
        gold: '#C9A227'
      },
      fontFamily: {
        display: ['Clash Display', 'General Sans', 'system-ui', 'sans-serif'],
        sans: ['General Sans', 'system-ui', 'sans-serif']
      },
      borderRadius: { xl2: '22px' },
      boxShadow: {
        soft: '0 1px 2px rgba(20,19,15,.04), 0 4px 14px rgba(20,19,15,.05)',
        card: '0 12px 34px -16px rgba(20,19,15,.22)',
        lift: '0 30px 60px -22px rgba(20,19,15,.34)'
      }
    }
  },
  plugins: []
}