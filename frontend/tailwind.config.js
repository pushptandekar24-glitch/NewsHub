/** @type {import('tailwindcss').Config} */
export default {
  darkMode: 'class',
  content: ['./index.html', './src/**/*.{js,jsx}'],
  theme: {
    extend: {
      colors: {
        // LIGHT MODE IS THE FIRST-CLASS EXPERIENCE.
        // Cool off-white base, pure white surfaces, near-black text.
        surface: {
          base: '#F5F7FB',   // page background
          raised: '#FFFFFF', // cards
          sunken: '#EEF2F9', // wells, skeletons
          muted: '#F8FAFD',
        },
        ink: {
          900: '#111827',    // primary text
          600: '#475569',
          500: '#64748B',    // secondary text
          400: '#94A3B8',
        },
        edge: {
          DEFAULT: '#E2E8F0',
          strong: '#CBD5E1',
        },
        brand: {
          50: '#EEF2FF',
          100: '#E0E7FF',
          400: '#818CF8',
          500: '#6366F1',    // indigo — primary accent
          600: '#4F46E5',
          700: '#4338CA',
        },
        violet: { 500: '#8B5CF6', 600: '#7C3AED' },
        sky: { 500: '#3B82F6', 600: '#2563EB' },
        // Dark mode surfaces, kept deliberately soft rather than pure black.
        night: {
          base: '#0B1020',
          raised: '#121A2E',
          sunken: '#0E1526',
          edge: '#233049',
        },
      },
      fontFamily: {
        sans: ['Inter', 'system-ui', '-apple-system', 'Segoe UI', 'sans-serif'],
        mono: ['JetBrains Mono', 'ui-monospace', 'SFMono-Regular', 'monospace'],
      },
      boxShadow: {
        // Layered depth. Multiple small shadows read as a physical surface;
        // one large blurry shadow reads as a drop-shadow filter.
        surface: '0 1px 2px rgba(15,23,42,.04), 0 2px 8px rgba(15,23,42,.04)',
        lift: '0 2px 4px rgba(15,23,42,.05), 0 12px 28px rgba(15,23,42,.10)',
        float: '0 8px 16px rgba(15,23,42,.06), 0 24px 48px rgba(15,23,42,.14)',
        glow: '0 0 0 1px rgba(99,102,241,.25), 0 12px 32px rgba(99,102,241,.18)',
      },
      backgroundImage: {
        'brand-gradient': 'linear-gradient(135deg,#6366F1 0%,#8B5CF6 50%,#3B82F6 100%)',
        'hero-glow': 'radial-gradient(60% 80% at 50% 0%, rgba(99,102,241,.16) 0%, rgba(99,102,241,0) 70%)',
        shimmer: 'linear-gradient(90deg, transparent 0%, rgba(255,255,255,.65) 50%, transparent 100%)',
      },
      keyframes: {
        rise: { '0%': { opacity: 0, transform: 'translateY(10px)' }, '100%': { opacity: 1, transform: 'none' } },
        shimmer: { '100%': { transform: 'translateX(100%)' } },
        pulseDot: { '0%,100%': { opacity: 1 }, '50%': { opacity: .35 } },
        floaty: { '0%,100%': { transform: 'translateY(0)' }, '50%': { transform: 'translateY(-8px)' } },
      },
      animation: {
        rise: 'rise .4s cubic-bezier(.22,1,.36,1) both',
        shimmer: 'shimmer 1.6s infinite',
        pulseDot: 'pulseDot 1.8s ease-in-out infinite',
        floaty: 'floaty 6s ease-in-out infinite',
      },
    },
  },
  plugins: [],
}
