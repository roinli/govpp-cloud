#!/bin/sh

# 复制项目的文件到对应docker路径，便于一键生成镜像。
usage() {
	echo "Usage: sh copy.sh"
	exit 1
}

echo "begin package "
#打包开始
cd ..
mvn clean install -Dmaven.test.skip=true
#前端
cd ../pc_web
npm install --registry=https://registry.npmmirror.com
export NODE_OPTIONS=--openssl-legacy-provider
npm run build:prod
cd ../platform-service/docker
# copy sql
echo "begin copy sql "
cp ../sql/vpp_platform.sql ./mysql/db
cp ../sql/witos_config.sql ./mysql/db

# copy html
echo "begin copy html "
rm -rf ./nginx/html/dist
mkdir -p ./nginx/html/dist
cp -rp ../../pc_web/dist/. ./nginx/html/dist


# copy jar
echo "begin copy vpp-register "
cp ../vpp-register/target/vpp-register.jar ./nacos/jar

echo "begin copy vpp-gateway "
cp ../vpp-gateway/target/vpp-gateway.jar ./vpp/gateway/jar

echo "begin copy vpp-auth "
cp ../vpp-auth/target/vpp-auth.jar ./vpp/auth/jar


echo "begin copy vpp-monitor "
cp ../vpp-visual/vpp-monitor/target/vpp-monitor.jar  ./vpp/visual/monitor/jar

echo "begin copy vpp-system "
cp ../vpp-modules/vpp-system/target/vpp-system.jar ./vpp/modules/system/jar

echo "begin copy vpp-file "
cp ../vpp-modules/vpp-file/target/vpp-file.jar ./vpp/modules/file/jar

echo "begin copy vpp-gen "
cp ../vpp-modules/vpp-gen/target/vpp-gen.jar ./vpp/modules/gen/jar

echo "begin copy vpp-job "
cp ../vpp-modules/vpp-job/target/vpp-job.jar ./vpp/modules/job/jar

echo "begin copy vpp-event-service "
cp ../vpp-event-service/target/vpp-event-service.jar ./vpp/event-service/jar

echo "begin copy vpp-dispatch-service "
cp ../vpp-dispatch-service/target/vpp-dispatch-service.jar ./vpp/dispatch-service/jar

