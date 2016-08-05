var gulp       = require('gulp');
var sass       = require('gulp-sass');
var buster     = require('gulp-buster');
var rename     = require('gulp-rename');
var cleanCSS   = require('gulp-clean-css');
var sourcemaps = require('gulp-sourcemaps');
var rework     = require('gulp-rework');
var clean      = require('gulp-clean');
var reworkUrl  = require('rework-plugin-url');
var path       = require('path')
var del        = require('del');
var vinylPaths = require('vinyl-paths');

var paths = {
  sass_libs: ['node_modules/normalize.css'],
  sass:       'src/assets/scss/app.scss',
  css:        'resources/public/css/app.css',
  css_dir:    'resources/public/css',
  js:         'resources/public/js/**/*.js',
  fonts:      'resources/public/fonts/*'
}

var dest = {
  css:       'resources/public/assets/css/app.css',
  css_dir:   'resources/public/assets/css',
  js_dir:    'resources/public/assets/js',
  fonts_dir: 'resources/public/assets/fonts',
  asset_dir: 'resources/public/assets'
}

gulp.task('build', ['clean'], function() {
  gulp.start('assets');
});

gulp.task('assets', ['rework'], function () {
  var busters = require("./resources/public/assets/manifest.json");

  gulp.src([dest.js_dir + "/**/*.js", dest.css, dest.fonts_dir + "/*"], { base: process.cwd() })
    .pipe(vinylPaths(del))
    .pipe(rename(function (p) {
      var file = p.dirname + "/" + p.basename + p.extname
      var hash = busters[file];

      if(p.extname && p.extname.indexOf('.') == 0 && hash) {
        p.basename += "-" + busters[file]
      }

      return p
    }))
    .pipe(gulp.dest('.'))
});

gulp.task('rework', ['buster'], function () {
  var busters = require("./resources/public/assets/manifest.json");

  return gulp.src(dest.css)
    .pipe(rework(reworkUrl(function(url) {
      var p    = path.parse(url)
      var hash = busters["resources/public/assets" + url]
      console.log(p);
      if(p.extname && p.extname.indexOf('.') == 0 && hash) {
        console.log(p);
        var url = path.format({
          name: p.name + "-" + hash,
          dir:  path.join("/assets", p.dir),
          ext:  p.ext
        });

        return url;
      }

      return url;
    })))
    .pipe(gulp.dest(dest.css_dir))
});

gulp.task('buster', ['css', 'js', 'fonts'], function () {
  return gulp.src([dest.css, dest.js_dir + "/**/*.js", dest.fonts_dir + "/*"])
    .pipe(buster({fileName: 'manifest.json'}))
    .pipe(gulp.dest(dest.asset_dir))
});

gulp.task('css', ['sass'], function () {
  return gulp.src(paths.css)
    .pipe(gulp.dest(dest.css_dir))
});

gulp.task('js', function () {
  return gulp.src(paths.js)
    .pipe(gulp.dest(dest.js_dir))
});

gulp.task('fonts', function () {
  return gulp.src(paths.fonts)
    .pipe(gulp.dest(dest.fonts_dir))
});

gulp.task('sass', function () {
  return gulp.src(paths.sass)
    .pipe(sourcemaps.init())
    .pipe(sass({includePaths: paths.sass_libs}).on('error', sass.logError))
    .pipe(cleanCSS())
    .pipe(sourcemaps.write('.'))
    .pipe(gulp.dest(paths.css_dir))
});

gulp.task('clean', function() {
  return gulp.src(dest.asset_dir, {read: false})
    .pipe(clean());
});

gulp.task('watch', ['clean'], function() {
  gulp.watch("src/assets/scss/**/*", ['sass']);
});

gulp.task('default', ['watch', 'sass']);
