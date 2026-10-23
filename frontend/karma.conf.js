module.exports = function (config) {
  config.set({
    basePath: '',
    frameworks: ['jasmine', '@angular-devkit/build-angular'],
    plugins: [require('karma-jasmine'), require('karma-chrome-launcher'),
      require('karma-coverage'), require('@angular-devkit/build-angular/plugins/karma')],
    reporters: ['progress'],
    browsers: ['ChromeHeadless'],
    customLaunchers: { ChromeHeadlessCI: { base: 'ChromeHeadless',
      flags: ['--no-sandbox', '--disable-dev-shm-usage'] } },
    singleRun: true,
    autoWatch: false,
    browserNoActivityTimeout: 60000,
    client: { clearContext: false },
    coverageReporter: { dir: require('path').join(__dirname, 'coverage'), reporters: [{ type: 'text-summary' }] }
  });
};
