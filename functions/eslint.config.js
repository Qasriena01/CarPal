// eslint.config.js
module.exports = {
  root: true,
  env: {
    node: true,
    es2021: true,
  },
  extends: [
    'eslint:recommended',
    'plugin:node/recommended',
  ],
  parserOptions: {
    ecmaVersion: 'latest',
    sourceType: 'module',
  },
  rules: {
    // Add your custom rules here
    'quotes': ['error', 'double'], // Forcing double quotes for strings
    'no-unused-vars': ['warn'], // Warn about unused variables
    'max-len': ['error', { 'code': 80 }], // Ensure lines are not longer than 80 characters
    'no-multi-spaces': ['error'], // No multiple spaces before comments
    'comma-dangle': ['error', 'always-multiline'], // Ensure trailing commas in multi-line objects/arrays
  },
};
