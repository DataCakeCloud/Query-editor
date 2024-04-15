# -*- coding: utf-8 -*-
import tableauserverclient as TSC
import binascii
import sys

from template import Templates
from notify import Notify

from s3_util import get_s3_path, load_file

site_id = ""

S3_REGION_NAME = "us-east-1"
bucket_name = "da.results.prod.us-east-1"
key_prefix = "SHAREIT_A/tableau_picture_daily/"
file_sep = "/"

import time
is_set_expire_picture = False
reload(sys)
sys.setdefaultencoding("utf-8")

def download_picture(dashboard_name, server_address):
    tableau_auth = TSC.TableauAuth("bpd_Creator",
                                   binascii.a2b_hex("59366b363238").decode(),
                                   site_id=site_id)
    server = TSC.Server(server_address)
    server.session.headers = {'Connection':'close'}
    print ("Tableauserverclient auth pass.")
    file_name = "%s_%s.png" % (str(dashboard_name), time.strftime("%Y%m%d.%H%M%S", time.localtime()))
    # file_name = "C端商业化日收入监控_20230315.144925.png"
    with server.auth.sign_in(tableau_auth):
        server.use_server_version()
        req_option = TSC.RequestOptions()
        req_option.filter.add(
            TSC.Filter(TSC.RequestOptions.Field.Name, TSC.RequestOptions.Operator.Equals, dashboard_name))
        all_views, pagination_item = server.views.get(req_option)
        print([view.name for view in all_views])
        view_item = all_views[0]
        image_req_option = TSC.ImageRequestOptions(maxage=0)
        server.views.populate_image(view_item, image_req_option)
        with open(file_name, "wb") as image_file:
            image_file.write(view_item.image)
            image_file.close()
            print("download success.")
    return file_name

def get_key(key_prefix, save_path, file_name):
    if save_path is None:
        return key_prefix + file_name
    if save_path.startswith(file_sep):
        save_path = save_path.replace(file_sep, "", 1)
    if not save_path.endswith(file_sep):
        save_path = save_path + file_sep
    return key_prefix + save_path + file_name


def send_daily_report_picture(group_ding_token, msg_title, dashboard_name, server_address, is_at_all, msg_expire_min,
                              save_path, **kwargs):
    start = time.clock()
    file_name = download_picture(dashboard_name, server_address)
    end = time.clock()
    duration = end - start
    print duration
    key = get_key(key_prefix, save_path, file_name)
    s3_path = get_s3_path(bucket_name, key)
    print file_name
    print key
    print bucket_name
#    load_file(filename=file_name, key=key, bucket_name=bucket_name, replace=True, acl_policy="public-read")
#   Notify().notify_md_by_robot(group_ding_token, msg_title, "![日报](%s)" % s3_path, is_at_all=is_at_all)
#    if is_set_expire_picture:
#        time.sleep(int(msg_expire_min) * 60)
#        load_file(filename=file_name, key=key, bucket_name=bucket_name, replace=True, acl_policy="private")




if __name__ == '__main__':
    # tableau看板地址
    default_server_address = "https://127.0.0.1"
    # 是否@群内所有人，默认为True
    default_is_at_all = True
    # 默认过期时间 单位:分钟
    default_msg_expire_min = 20
    # 默认保存路径
    default_save_path = ""
    reports_obj = Templates(server_address=default_server_address, is_at_all=default_is_at_all,
                            msg_expire_min=default_msg_expire_min, save_path=default_save_path)

    reports_obj.add(task_id="task_Mars",
                    # 检查依赖的路径, 可以为 None
                    dependency_check_paths=None,
                    group_ding_token="xxx",  # 测试群
                    msg_title="C端商业化日收入监控日报", dashboard_name="C端商业化日收入监控",
                    msg_expire_min=20, save_path="zhangteng/dashboard")
    template =  reports_obj.temp_list[0]
    send_daily_report_picture(group_ding_token=template.group_ding_token,msg_title=template.msg_title,dashboard_name=template.dashboard_name,\
                              server_address=template.server_address,is_at_all=template.is_at_all,msg_expire_min=template.msg_expire_min,save_path=template.save_path)
