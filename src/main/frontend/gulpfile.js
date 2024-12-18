/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
const gulp        = require('gulp');
const sass        = require('gulp-sass')(require('sass'));
const header      = require('gulp-header');
const cleanCSS   = require('gulp-clean-css');
var concatCss = require('gulp-concat-css');

const apache2License = [
'/*',
' * Licensed to the Apache Software Foundation (ASF) under one or more',
' * contributor license agreements.  See the NOTICE file distributed with',
' * this work for additional information regarding copyright ownership.',
' * The ASF licenses this file to You under the Apache License, Version 2.0',
' * (the "License"); you may not use this file except in compliance with',
' * the License.  You may obtain a copy of the License at',
' *',
' *      http://www.apache.org/licenses/LICENSE-2.0',
' *',
' * Unless required by applicable law or agreed to in writing, software',
' * distributed under the License is distributed on an "AS IS" BASIS,',
' * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.',
' * See the License for the specific language governing permissions and',
' * limitations under the License.',
' */',
''
].join('\n');

gulp.task('styles', function() {
    return gulp.src('./src/scss/*.scss')
        .pipe(sass().on('error', sass.logError))
        .pipe(cleanCSS())
        .pipe(concatCss('bundle.css'))
        .pipe(header(apache2License))
        .pipe(gulp.dest('./dist/initial-content/content/starter/css'));
});

gulp.task('assets', function() {
    return gulp.src(['./src/{fonts,img}/**/*'])
        .pipe(gulp.dest('./dist/initial-content/content/starter'));
});

gulp.task('default', gulp.series(gulp.parallel('styles', 'assets')));