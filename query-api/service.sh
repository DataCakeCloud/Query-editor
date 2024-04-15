#!/bin/bash
# java 服务管理脚本
source /etc/profile
AppUser=java
adduser $AppUser
AppDir=$(cd $(dirname $0); pwd)
ExeArgs="-server -Xms512mG -Xmx51mG -Xss512k -XX:MaxNewSize=128m -XX:+UseParNewGC -XX:+UseConcMarkSweepGC -XX:+DisableExplicitGC -XX:+CMSParallelRemarkEnabled"
ExeArgs="$ExeArgs -Dspring.profiles.active=auth-test,test"
JarFile=$(find $AppDir/ -maxdepth 1 -name "*.jar")
LogDir=/data/logs/query

Pid()
{
    AppPid=$(ps ax | grep "$AppDir/" | grep -v grep | grep -v start  | grep -v stop  | awk '{print $1}')
}

Start()
{
    Pid
    if [ -n "$AppPid" ]
    then
        echo "服务正在运行"
    else
        mkdir -p $LogDir
        sudo -u $AppUser java -jar $ExeArgs $JarFile >/dev/null 2>&1 &
        [ $? -eq 0 ] && echo "启动服务" || echo "启动服务失败"
    fi
}

Stop()
{
    Pid
    if [ -n "$AppPid" ]
    then
        kill -9 $AppPid && echo "停止服务"
    else
        echo "服务未运行"
    fi
}

Status()
{
    Pid
    if [ -n "$AppPid" ]
    then
        echo "服务正在运行，AppPid: $AppPid"
        ps aux | grep -v grep | grep "$AppDir/.*.jar"
    else
        echo "服务未运行"
    fi
}

case "$1" in
'start')
    Start
    ;;
'stop')
    Stop
    ;;
'restart')
    Stop
    sleep 1
    Start
    ;;
'status')
    Status
    ;;
*)
    echo "Usage: $0 {status | start | stop | restart}"
    ;;
esac
exit 0
