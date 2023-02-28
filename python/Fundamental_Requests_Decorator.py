#!/usr/bin/python3
# Filename: Test.py
from sys import argv,__name__
#__name__ has been covered by sys.__name__

#from sys import *
#__name__ is __main__ here
import time
import json
import requests

for i in range(len(argv)):
    print(argv[i])

print(__name__)

if __name__=="main":
    try:
        int("string")
    except:
        # Specific Error Name (or NUll the last one.)
        print("Error: {0}","not int value")
    else:
        print("impossible here")
    finally:
        print("just like java do here")
# elif __name__=="sys":
elif __name__=="s":
    print("ok")
else:
    print("Hello {0} ! Time: {date}".format("yue",date=time.strftime("%Y-%m-%d %H:%M:%S", time.localtime())))

class Test:
    # 类变量
    pCount =0;
    # 私有属性__name
    __privateAttr=12


    # 继承自 object 的新式类才有__new__
    def __new__(cls,name,age=12,other="0"):
        '在实例创建之前被调用的，因为它的任务就是创建实例然后返回该实例对象，是个静态方法。'
        print("__new__")
        return super(Test,cls).__new__(cls)


    def __init__(self,name,age=24,other="0"):
        '当实例对象创建完成后被调用的，然后设置对象属性的一些初始值，通常用在初始化一个类实例的时候。是一个实例方法。'
        # 实例变量
        pass
        # print("__init__ start").__init__()
        super(Test,self)
        self.name=name
        self.age=age
        self.other=other
        self.__priAttr=12# 私有属性
        Test.pCount+=1
        print("__init__ end")
        #  __new__先被调用，__init__后被调用，__new__的返回值（实例）将传递给__init__方法的第一个参数，
        # 然后__init__给这个实例设置一些参数。
    # 此方法覆盖了，上一方法的定义
    # def __init__(self,name,age,other):
    #     self.name=name
    #     self.age=age
    #     self.other=other
    #     Test.pCount+=1

    def __str__(self):
        # just like java toString()
        return "Test[name={0},age={1},other={2}]".format(self.name,self.age,self.other)

    def __del__(self):
        # 析构函数
        print("class:",self.__class__.__name__,"Object:",self,"deleted")

    def __add__(self, other):
        # C++运算符重载
        return Test(self.name,other.hasself.age+other.age)

    def __test(self):
        print("private Method do something here !!!")

    def testPub(self):
        print("ok, public !")

test= Test("yue")
print(str(test))
test2= Test("Helo",other="bye")
print(test2)
print("{name} ,count={count}".format(count=Test.pCount,name="Test"))
del test2
del test

# 获取0-threshold以内的所有阿姆斯特朗数
def getAMSTLNum(threshold):
    if type(threshold)==int:
        for i in range(1,threshold):
            length = len(str(i));
            temp = i
            sum = 0
            while temp>0:
                pos= temp % 10
                sum+=pos**length
                temp //=10
            if i==sum:
                print(i,end="  ")

    else:
        print("fuck !!!")
        raise Exception("threshold should be a number(int)...")

getAMSTLNum(1000)
print()


#############################json数据#################################json数据####################################
data={
    'name':'yue',
    'age':24,
    'url':'guanyue.com'
}
print(data['name'],type(data),data)
json_str=json.dumps(data)
print("data= ",json_str)
json_str=json.loads(json_str)
print(json_str)
print(json_str['name'])

###################requests库###############requests库##############requests库###############################
# 具体参考[Requests快速入门](https://2.python-requests.org//zh_CN/latest/user/quickstart.html#id4)
url="{base}/{path}".format(base="https://www.baidu.com",path="img/xinshouye_c9d9de2ff40fa160f807f75f34db4ad0.gif");
params={
}
headers={
    'User-Agent' : "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/71.0.3578.98 Safari/537.36",
    'Host': 'www.baidu.com'
}
response=requests.get(url,params=params,headers=headers)
# payload={
#   'key':'value'
# }
# requests.post(url,data=payload or data=json.dumps(payload) or json=payload)

file=open("./Hello.png","wb")
file.write(response.content)# response.content 二进制数据
file.close()

# response.json()
#
# response.raw
# response.raw.read(1024)
#
# with open(filename, 'wb') as fd:
#     for chunk in response.iter_content(chunk_size):
#         fd.write(chunk)

######################装饰器################装饰器###########################
def logging(level):
    def wrapper(func):
        # DEBUG: inner_wrapper...
        def inner_wrapper(*args, **kwargs):
            print("[{level}]: enter function {func}()".format(
                level=level,
                func=func.__name__))
            return func(*args, **kwargs)
        return inner_wrapper
    return wrapper

@logging(level='INFO')
def say(something):
    print("say {}!".format(something))

# 如果没有使用@语法，等同于
# say = logging(level='INFO')(say)

@logging(level='DEBUG')
def do(something):
    print("do {}...".format(something))

say('hello')
do("my work")

###################装饰器############装饰器##################
class msg(object):
    """docstring for msg."""

    def __init__(self, message,id=1):
        super(msg, self).__init__()
        self.id = id
        self.message = message
    def __str__(self):
        return "msg[id={0},message={1}]".format(self.id, self.message)

def msg_register(msgType):
    print("Outer decorator enter")
    msgType = [msgType]
    def _msg_register(fn):
        print("Inner decorator enter")
        def wrapper(*args, **kwargs):
            for _msgType in msgType:
                print("register msgType:",_msgType)
            return fn(*args, **kwargs)
        # DEBUG:
        print("Inner decorator exit")
        return wrapper
    # DEBUG:
    print("Outer decorator exit")
    return _msg_register

# 装饰器-->封装函数，增加额外功能
# @msg_register(msgType='TEXT')
def  text_reply(msg):
    print("msg","Hello")
    return msg

Func=msg_register(msgType='TEXT')
# Outer decorator enter
# Outer decorator exit

text_reply=Func(text_reply)
# Inner decorator enter
# Inner decorator exit

# 调用顺序
#   第一次调用：msg_register(msgType='TEXT')
#   第二次调用：Func(text_reply)
# text_reply =msg_register(msgType='TEXT')(text_reply)
# Outer decorator enter
# Outer decorator exit
# Inner decorator enter
# Inner decorator exit

print(text_reply(msg("hello")))
# register msgType: TEXT
# msg Hello
# msg[id=1,message=hello]

print(text_reply(msg("yue")))
# register msgType: TEXT
# msg Hello
# msg[id=1,message=yue]
