@echo off
REM 创建根目录
mkdir workflow-ui
cd workflow-ui

REM 创建文件
type nul > index.html
type nul > package.json
type nul > vite.config.js

REM 创建 public 目录
mkdir public

REM 创建 src 目录及文件
mkdir src
cd src

REM api
mkdir api
type nul > api\index.js

REM assets
mkdir assets
type nul > assets\main.css

REM components
mkdir components
type nul > components\AppLayout.vue

REM router
mkdir router
type nul > router\index.js

REM stores
mkdir stores
type nul > stores\user.js

REM views
mkdir views
type nul > views\FormBuilder.vue
type nul > views\WorkflowDesigner.vue

REM 如果有更多页面，可以继续创建
REM type nul > views\OtherPage.vue

REM 根组件与入口文件
type nul > App.vue
type nul > main.js

cd ..
echo 目录结构已创建完成
pause
