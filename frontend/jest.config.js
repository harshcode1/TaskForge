const nextJest = require('next/jest');

const createJestConfig = nextJest({
  // Tells next/jest where to find next.config.mjs and .env files
  dir: './',
});

/** @type {import('jest').Config} */
const customConfig = {
  testEnvironment: 'jest-environment-jsdom',
  // Runs after the test framework is installed in the environment
  setupFilesAfterEnv: ['<rootDir>/jest.setup.js'],
  moduleNameMapper: {
    // Handle @/ path alias (mirrors jsconfig.json)
    '^@/(.*)$': '<rootDir>/src/$1',
  },
  testPathIgnorePatterns: ['<rootDir>/node_modules/', '<rootDir>/.next/'],
  // Allow transforming ESM packages that ship without CJS
  transformIgnorePatterns: [
    '/node_modules/(?!(lucide-react|@radix-ui|@dnd-kit)/)',
  ],
  collectCoverageFrom: [
    'src/**/*.{js,jsx}',
    '!src/**/*.test.{js,jsx}',
    '!src/app/layout.js',
    '!src/components/ui/**',
  ],
};

module.exports = createJestConfig(customConfig);
