console.log('>>> LOADED KARMA CONFIG:', __filename);
const path = require('path');

module.exports = function (config) {
  config.set({
    basePath: '',
    frameworks: ['jasmine', '@angular-devkit/build-angular'],

    plugins: [
      require('karma-jasmine'),
      require('karma-chrome-launcher'),
      require('karma-coverage'),
      require('karma-junit-reporter')
      // ✅ DO NOT require '@angular-devkit/build-angular/plugins/karma' in Angular 20 (@angular/build)
    ],

    reporters: ['progress', 'junit', 'coverage'],

    junitReporter: {
      outputDir: path.resolve(__dirname, 'test-results'),
      outputFile: 'karma.xml',
      useBrowserName: false
    },

    coverageReporter: {
      dir: path.resolve(__dirname, 'coverage'),
      reporters: [{ type: 'html' }, { type: 'lcovonly' }, { type: 'cobertura' }]
    },

    browsers: ['ChromeHeadless'],
		customLaunchers: {
			ChromeHeadless: {
				base: 'Chrome',
				flags: [
					'--headless',
					'--no-sandbox'
				]
			}
		},

    singleRun: true,
    restartOnFileChange: false
  });
};
