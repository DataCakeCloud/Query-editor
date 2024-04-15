#!/bin/bash
start(){
   # java 服务管理脚本
   ExtraArgs=$1
   AppUser=${AppUser}
   AppDir=$(cd $(dirname $0); pwd)

   curTime=$(date +%s)
   JarFile=$(find $AppDir/ -maxdepth 1 -name "*.jar")
   CpArgs="-cp .:$JarFile"
   ExeArgs="-server -XX:+UseConcMarkSweepGC -XX:+UseParNewGC -XX:+CMSClassUnloadingEnabled -XX:+DisableExplicitGC
   -XX:+UseCMSInitiatingOccupancyOnly -XX:CMSInitiatingOccupancyFraction=68 -verbose:gc -XX:+PrintGCDetails
   -XX:+PrintGCDateStamps  -Xloggc:/data/logs/cbs_ds_task_gc_$curTime.log -XX:+HeapDumpOnOutOfMemoryError
   -XX:HeapDumpPath=/data/logs/gc -XX:+CrashOnOutOfMemoryError -XX:MaxDirectMemorySize=2G 
   -XX:ErrorFile=/data/logs/gc/hs_err_pid%p.log -Xms512m -Xmx2048m -Xmn256m -Xss256k -XX:MetaspaceSize=128m -XX:MaxMetaspaceSize=512m -XX:SurvivorRatio=4 "
   ExeArgs="$ExeArgs -Dlog4j2.formatMsgNoLookups=true"
   if [ ! -z $ExtraArgs ]; then
       if [ $ExtraArgs == "dev" ] || [ $ExtraArgs == "test" ] || [ $ExtraArgs == "prod" ]; then
           ExeArgs="$ExeArgs -Dspring.profiles.active=auth-$ExtraArgs,$ExtraArgs"
       else
           ExeArgs="$ExeArgs $ExtraArgs"
       fi
   fi
   MainClass=org.springframework.boot.loader.JarLauncher

   # 以对应用启动时，1）部分文件没有权限；2）/data/logs 挂载到容器后普通用户无写权限
   sudo chown -R ${AppUser}:${AppGroup} /data

   java $CpArgs $ExeArgs $MainClass
}

stop(){
   kill -15 `ps -ef|grep tailer|grep ${AppUser}|awk '{print $1,$2}'|grep -v root|awk '{print $2}'`
}
case "$1" in
   "stop")
      stop
      ;;
   *)
      start $2
      ;;
esac

