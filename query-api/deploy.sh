#!/bin/bash
# 配置启动 hebe
AppDir=/App/ds_task

cd $AppDir
chmod +rx *.sh
chmod -R 777 /data/logs
cp ds_task.service /usr/lib/systemd/system/
systemctl daemon-reload
systemctl enable ds_task
systemctl start ds_task
