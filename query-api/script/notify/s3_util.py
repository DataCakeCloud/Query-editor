import gzip as gz
import shutil

import boto3
from urlparse import urlparse

def parse_s3_url(s3url):
    parsed_url = urlparse(s3url)
    if not parsed_url.netloc:
        raise Exception('Please provide a bucket_name instead of "%s"' % s3url)
    else:
        bucket_name = parsed_url.netloc
        key = parsed_url.path.strip('/')
        return bucket_name, key

def get_s3_path(bucket_name, key):
    s3_path = "https://s3.amazonaws.com/%s/%s" % (bucket_name, key)
    print ("s3_path: %s" % s3_path)
    return s3_path

def check_for_key(key, bucket_name=None):
        """
        Checks if a key exists in a bucket

        :param key: S3 key that will point to the file
        :type key: str
        :param bucket_name: Name of the bucket in which the file is stored
        :type bucket_name: str
        """
        if not bucket_name:
            (bucket_name, key) = parse_s3_url(key)
        client = boto3.resource("s3")

        try:
            client.head_object(Bucket=bucket_name, Key=key)
            return True
        except Exception as e:
            print(e.response["Error"]["Message"])
            return False

def load_file(filename,
              key,
              bucket_name=None,
              replace=False,
              encrypt=False,
              gzip=False,
              acl_policy=None):
    if not bucket_name:
        (bucket_name, key) = parse_s3_url(key)

    if not replace and check_for_key(key, bucket_name):
        raise ValueError("The key {key} already exists.".format(key=key))

    extra_args = {}
    if encrypt:
        extra_args['ServerSideEncryption'] = "AES256"
    if gzip:
        filename_gz = filename.name + '.gz'
        with open(filename.name, 'rb') as f_in:
            with gz.open(filename_gz, 'wb') as f_out:
                shutil.copyfileobj(f_in, f_out)
                filename = filename_gz
    if acl_policy:
        extra_args['ACL'] = acl_policy

    client = boto3.client("s3")
    client.upload_file(filename, bucket_name, key, ExtraArgs=extra_args)
