/* eslint-env node */
module.exports = {
  extends: [
    'stylelint-config-standard-scss',
    'stylelint-config-recommended-vue/scss',
  ],
  overrides: [
    {
      files: ['**/*.{vue,html}'],
      customSyntax: 'postcss-html',
    },
    {
      files: ['**/*.scss'],
      customSyntax: 'postcss-scss',
    },
  ],
  rules: {
    'selector-class-pattern': null,
    'scss/at-import-partial-extension': null,
    'scss/load-no-partial-leading-underscore': null,
    'scss/dollar-variable-pattern': null,
    'no-descending-specificity': null,
    'declaration-block-no-redundant-longhand-properties': null,
    'color-function-notation': null,
    'alpha-value-notation': null,
    'shorthand-property-no-redundant-values': null,
  },
  ignoreFiles: ['**/*.d.ts', 'dist/**', 'node_modules/**'],
}
