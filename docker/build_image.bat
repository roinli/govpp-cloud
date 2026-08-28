@echo off
chcp 65001 >nul & cls
echo.
echo [信息] 复制文件到Docker目录
echo.

%~d0
cd %~dp0

cd ..
echo 编译后端
start /wait cmd /c "mvn clean package -P prod -Dmaven.test.skip=true"
echo 编译前端
cd ..\pc_web
start /wait cmd /c "npm install"
start /wait cmd /c "npm run build:prod"
cd ..\platform-service\docker

echo 复制 sql
xcopy ..\sql\vpp_platform.sql .\mysql\db  /y
xcopy ..\sql\witos_config.sql .\mysql\db  /y

echo 复制 html
xcopy ..\..\pc_web\dist .\nginx\html\dist  /s /e /y

echo 复制 witos-nacos
xcopy ..\vpp-register\target\vpp-register.jar .\nacos\jar  /y

echo 复制 vpp-gateway
xcopy ..\vpp-gateway\target\vpp-gateway.jar .\vpp\gateway\jar  /y

echo 复制 vpp-auth
xcopy ..\vpp-auth\target\vpp-auth.jar .\vpp\auth\jar  /y


echo 复制 vpp-monitor
xcopy ..\vpp-visual\vpp-monitor\target\vpp-monitor.jar  .\vpp\visual\monitor\jar  /y

echo 复制 vpp-system
xcopy ..\vpp-modules\vpp-system\target\vpp-system.jar .\vpp\modules\system\jar  /y

echo 复制 vpp-file
xcopy ..\vpp-modules\vpp-file\target\vpp-file.jar .\vpp\modules\file\jar  /y

echo 复制 vpp-gen
xcopy ..\vpp-modules\vpp-gen\target\vpp-gen.jar .\vpp\modules\gen\jar  /y

echo 复制 vpp-job
xcopy ..\vpp-modules\vpp-job\target\vpp-job.jar .\vpp\modules\job\jar  /y

echo 复制 vpp-event-service
xcopy ..\vpp-event-service\target\vpp-event-service.jar .\vpp\event-service\jar  /y

echo 复制 vpp-dispatch-service
xcopy ..\vpp-dispatch-service\target\vpp-dispatch-service.jar .\vpp\dispatch-service\jar  /y

@REM 前端打包成镜像
cd ./nginx
docker build -t vpp/vpp-nginx:v1.0 .

@REM nacos打包成镜像
cd ../nacos
docker build -t witos/vpp-register:v1.0 .

@REM 打包网关镜像 (修改构建上下文)
cd ../vpp/gateway
docker build -t witos/vpp-gateway:v1.0 .

@REM 打包认证镜像 (修改构建上下文)
cd ../../vpp/auth
docker build -t witos/vpp-auth:v1.0 .

@REM 打包系统模块镜像 (修改构建上下文)
cd ../../vpp/modules/system
docker build -t witos/vpp-system:v1.0 .

@REM 打包文件服务镜像 (修改构建上下文)
cd ../file
docker build -t witos/vpp-file:v1.0 .

@REM 打包定时任务镜像
cd ../job
docker build -t witos/vpp-job:v1.0 .

@REM 打包监视镜像
cd ../../visual/monitor
docker build -t witos/vpp-monitor:v1.0 .


@REM 打包代码生成镜像
cd ../modules/gen
docker build -t witos/vpp-gen:v1.0 .

@REM 打包事件服务镜像
cd ../../event-service
docker build -t witos/vpp-event-service:v1.0 .

@REM 打包调度服务镜像
cd ../dispatch-service
docker build -t witos/vpp-dispatch-service:v1.0 .

pause
