#stage2 runtime image
FROM swr.ap-southeast-3.myhuaweicloud.com/shareit-common/java-base:oracle-jdk-1.8.0_202

RUN sudo chown -R ${AppUser}:${AppGroup} /data

#从build_image.sh传进来工程目录
#ARG PROJECT_NAME

# copy run jar file
COPY query-api/target/*.jar /data/code/
COPY query-api/run.sh  /data/code/run.sh

RUN sudo chmod o+x /data/code/run.sh    && \
    sudo mkdir -p /data/logs/gc         && \
    sudo mkdir -p /data/data/csv        && \
    sudo mkdir -p /data/config        && \
    sudo chown -R ${AppUser}:${AppGroup} /data && \
    sudo chmod 777 /data/code/run.sh

# set workdir
WORKDIR /data

# modify timezone
#RUN sudo ln -sf /usr/share/zoneinfo/Asia/Shanghai /etc/localtime
#RUN sudo echo 'Asia/Shanghai' >/etc/timezone

CMD ["/bin/bash","-c","bash /data/code/run.sh start $Env"]
