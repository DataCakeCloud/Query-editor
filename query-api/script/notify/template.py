class Template:
    def __init__(self, task_id, dependency_check_paths, group_ding_token, msg_title, dashboard_name, server_address, is_at_all, msg_expire_min, save_path):
        """
        init tableau picture send obj
        :param task_id: task_id
        :param dependency_check_paths: dependency check paths:list
        :param group_ding_token: dingding group webhook token
        :param msg_title: send msg title
        :param dashboard_name: prepare send picture name, unique
        :param server_address: tableau url address
        :param is_at_all:
        :param msg_expire_min: picture expire time, unit: minute
        :param save_path: aws s3 save address
        """
        self.dependency_check_paths = dependency_check_paths if dependency_check_paths is not None else []
        self.group_ding_token = group_ding_token
        self.msg_title = msg_title
        self.dashboard_name = dashboard_name
        self.task_id = task_id if task_id is not None else id(server_address)
        self.server_address = server_address
        self.is_at_all = is_at_all
        self.msg_expire_min = msg_expire_min
        self.save_path = save_path
        pass

class Templates:
    """
    Responsible for keeping all objects
    """
    temp_list = []

    def __init__(self, server_address, is_at_all, msg_expire_min, save_path):
        """
        :param server_address: tableau url address
        :param is_at_all:
        :param msg_expire_min: picture expire time, unit: minute
        :param save_path: aws s3 save address
        """
        self.server_address = server_address
        self.is_at_all = is_at_all
        self.msg_expire_min = msg_expire_min
        self.save_path = save_path

    def add(self, task_id, dependency_check_paths, group_ding_token, msg_title, dashboard_name, server_address=None, is_at_all=None, msg_expire_min=None, save_path=None):
        template = Template(task_id,
                            dependency_check_paths,
                            group_ding_token,
                            msg_title,
                            dashboard_name,
                            self.server_address if server_address is None else server_address,
                            self.is_at_all if is_at_all is None else is_at_all,
                            self.msg_expire_min if msg_expire_min is None else msg_expire_min,
                            self.save_path if save_path is None else save_path)
        self.temp_list.append(template)
