import type {Config} from 'tailwindcss'

export default {
    content: [
        './index.html',
        './src/**/*.{vue,js,ts,jsx,tsx}',
        '../packages/agent-ui/src/**/*.{vue,js,ts,jsx,tsx}',
    ],
    theme: {
        extend: {
            colors: {
                ink: '#182032',
                mist: '#f5f7fb',
                brand: {
                    50: '#eef5ff',
                    100: '#dceaff',
                    500: '#3b76f6',
                    600: '#295fd8',
                    700: '#234fb3',
                },
            },
            boxShadow: {
                panel: '0 24px 80px -36px rgba(38, 58, 105, 0.38)',
                composer: '0 12px 34px -16px rgba(29, 55, 102, 0.26)',
            },
            fontFamily: {
                sans: ['Inter', 'ui-sans-serif', 'system-ui', 'sans-serif'],
            },
        },
    },
    plugins: [],
} satisfies Config
