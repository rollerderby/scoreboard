import compat from "eslint-plugin-compat";

export default [
  {
    ignores: ["html/external/**", "html/documentation/wiki-snapshot.html"],
  },
  {
    files: ["html/**/*.js"],
    plugins: { compat },
    languageOptions: {
      // Use the latest ECMAScript version and treat files as scripts (not modules),
      // combined with the compat plugin & no-restricted-syntax rules, this will help us catch compatibility
      // issues with older browsers, even if we are using modern-ish syntax in our codebase.
      ecmaVersion: "latest",
      sourceType: "script",
      globals: {
        window: "readonly",
        document: "readonly",
        console: "readonly"
      }
    },
    rules: {
      // treat all compatibility issues as errors, so we can catch them in CI
      "compat/compat": "error",
      
      // Warn against block-scope catch bugs present in older iOS webviews
      "no-catch-shadow": "error",

      "no-restricted-syntax": [
        "error",
        {
          selector: "ExpressionStatement[directive='use strict']",
          message: "iOS Safari 9.3's legacy `const` works only in classic, non-strict scripts.",
        },
        {
          selector: "VariableDeclaration[kind='let']",
          message: "let declarations aren’t supported on older devices.",
        },
        {
          selector: "ArrowFunctionExpression",
          message: "Arrow functions aren’t supported on older devices.",
        },
      ],
    },
  },
];
