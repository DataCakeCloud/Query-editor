#!/bin/bash
VERSION=$1
PROJECT=$2
PROJECT_NAME=$3
BUILDENV=$4
IMAGE_HUB='swr.ap-southeast-3.myhuaweicloud.com/shareit-bdp'
if [ ! -n "$VERSION" ]; then
    VERSION='SNAPSHOT-1.0'
fi
if [ ! -n "$PROJECT" ]; then
    PROJECT='query-editor'
fi
if [ `docker images|grep $PROJECT|awk '{print $3}'|wc -l` -gt 0 ]; then
    echo -------------------------------------------
    echo begin remove docker images $PROJECT
    echo -------------------------------------------
    docker rmi -f `docker images|grep $PROJECT|awk '{print $3}'`
fi
# IMAGE_NAME=`echo $PROJECT| awk -F "/" '{print $NF}'`
IMAGE_NAME=$PROJECT
echo -------------------------------------------
echo begin build docker images $PROJECT:$VERSION
echo -------------------------------------------
docker build --no-cache --build-arg PROJECT_NAME=$PROJECT_NAME --build-arg BUILDENV=$BUILDENV -t $IMAGE_NAME:$VERSION .
if [ $? -ne 0 ];then
    echo "build image failed " && exit 1
fi
docker tag $IMAGE_NAME:$VERSION $IMAGE_HUB/$IMAGE_NAME:$VERSION
docker push $IMAGE_HUB/$IMAGE_NAME:$VERSION
