module.exports = {
  env: {
    es6: true,
    node: true,
  },
  parserOptions: {
    "ecmaVersion": 2018,
  },
  extends: [
    "eslint:recommended",
    "google",
  ],
  rules: {
    "no-restricted-globals": ["error", "name", "length"],
    "prefer-arrow-callback": "error",
    "quotes": ["error", "double", {"allowTemplateLiterals": true}],
    'quotes': ['error', 'double'], // Forcing double quotes for strings
    'no-unused-vars': ['warn'], // Warn about unused variables
    'max-len': ['error', { 'code': 80 }], // Ensure lines are not longer than 80 characters
    'no-multi-spaces': ['error'], // No multiple spaces before comments
     'comma-dangle': ['error', 'always-multiline'], // Ensure trailing commas in multi-line objects/arrays
  },
  overrides: [
    {
      files: ["**/*.spec.*"],
      env: {
        mocha: true,
      },
      rules: {},
    },
  ],
  globals: {},
};
