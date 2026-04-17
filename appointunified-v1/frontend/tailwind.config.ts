import type { Config } from 'tailwindcss'

const config: Config = {
  content: [
    './pages/**/*.{js,ts,jsx,tsx,mdx}',
    './components/**/*.{js,ts,jsx,tsx,mdx}',
    './app/**/*.{js,ts,jsx,tsx,mdx}',
  ],
  darkMode: 'class',
  theme: {
    extend: {
      colors: {
        primary: 'var(--bg-primary)',
        secondary: 'var(--bg-secondary)',
        elevated: 'var(--bg-elevated)',
        subtle: 'var(--bg-subtle)',
        border: {
          DEFAULT: 'var(--border)',
          focus: 'var(--border-focus)',
          card: 'var(--border-card)',
        },
        'border-focus': 'var(--border-focus)',
        'border-card': 'var(--border-card)',
        'text-primary': 'var(--text-primary)',
        'text-secondary': 'var(--text-secondary)',
        'text-muted': 'var(--text-muted)',
        'text-on-dark': 'var(--text-on-dark)',
        accent: {
          DEFAULT: 'var(--accent)',
          light: 'var(--accent-light)',
          glow: 'var(--accent-glow)',
          mint: 'var(--pastel-mint)',
          warm: 'var(--pastel-peach)',
        },
        pastel: {
          purple: 'var(--pastel-purple)',
          pink: 'var(--pastel-pink)',
          mint: 'var(--pastel-mint)',
          peach: 'var(--pastel-peach)',
          yellow: 'var(--pastel-yellow)',
        },
        /* Legacy Alias Mapping */
        brand: {
          50: 'var(--bg-elevated)',
          100: 'var(--accent-glow)',
          200: 'var(--pastel-purple)', // Fun map for old colors
          300: 'var(--accent-light)',
          400: 'var(--accent)',
          500: 'var(--accent)',
          600: 'var(--text-primary)', // Maps deep brand buttons to the new stark black layout
          700: 'var(--text-primary)',
          800: 'var(--text-primary)',
          900: 'var(--text-primary)',
          950: 'var(--text-primary)',
        },
      },
      fontFamily: {
        sans: ['var(--font-inter)', 'Inter', 'system-ui', 'sans-serif'],
      },
      boxShadow: {
        soft: 'var(--shadow-soft)',
        float: 'var(--shadow-float)',
      },
      animation: {
        'fade-in': 'fadeIn 0.5s ease-out',
        'slide-up': 'slideUp 0.6s cubic-bezier(0.16, 1, 0.3, 1)',
        'float-slow': 'floatSlow 6s ease-in-out infinite',
        'pulse-soft': 'pulseSoft 3s ease-in-out infinite',
        'marquee': 'marquee 30s linear infinite',
      },
      keyframes: {
        fadeIn: { from: { opacity: '0' }, to: { opacity: '1' } },
        slideUp: { from: { transform: 'translateY(24px)', opacity: '0' }, to: { transform: 'translateY(0)', opacity: '1' } },
        floatSlow: {
          '0%, 100%': { transform: 'translateY(0)' },
          '50%': { transform: 'translateY(-10px)' },
        },
        pulseSoft: { '0%,100%': { opacity: '1' }, '50%': { opacity: '0.8' } },
        marquee: {
          '0%': { transform: 'translateX(0)' },
          '100%': { transform: 'translateX(-50%)' },
        },
      },
    },
  },
  plugins: [
    require('@tailwindcss/typography'),
  ],
}

export default config
