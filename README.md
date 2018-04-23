# 代理tcp

    因为业务场景需要，基于netty4擼了一个这个东西
    
## 配置文件说明

```properties
# proxy为默认名称，也可以使用其他的，但是不能有下划线
# tcp侦听信息
proxy.listener.address=localhost
proxy.listener.port=4444
# 指定传输方式。目前支持基于文件系统传递和代理直连
proxy.listener.factory=org.coodex.tcpproxy.transporters.FileBasedTransporterFactory
# proxy.listener.factory=org.coodex.tcpproxy.transporters.ProxyTransporterFactory
# 文件系统方式配置，文件存哪
proxy.listener.path=/home/shenhainan/proxy
# 使用什么加密，BlankEncryptor为不加密，自行扩展即可
proxy.listener.encryptor=org.coodex.tcpproxy.transporters.BlankEncryptor
# 接收数据时的超时时长，单位为毫秒。
# 因为数据文件被切成小块了，且传递顺序不可控，当后面的数据块先到达时，最长可等待一段时间，保证数据的完整性
proxy.listener.timeout=5000

# 远端tcp信息
proxy.remote.address=www.telchina.com.cn
proxy.remote.port=80

# 基于文件系统传递时配置
# 数据放哪
proxy.remote.path=/home/shenhainan/proxy
proxy.remote.encryptor=org.coodex.tcpproxy.transporters.BlankEncryptor
proxy.remote.timeout=5000
```

## 使用

java org.coodex.tcpproxy.TCPListener name1 name2
name1 name2就是配置文件中的名称

如果是文件系统传递，请求接收端再起一个org.coodex.tcpproxy.InvokerSideDamon即可，参数传递同上
