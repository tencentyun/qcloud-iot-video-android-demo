# error信息说明

以下是常见的返回错误吗对应的信息
--------

* @brief 操作成功
* WXERROR_OK = 0

* @brief 操作被取消
* 通常是被调用方取消.
* WXERROR_CANCELLED = 1

* @brief 未知错误
* 通常，你应该尽可能返回其他更加详细的错误码。如果实在没有相关可以使用的错误码，使用
* WXERROR_UNKNOWN.
* WXERROR_UNKNOWN = 2

* @brief 非法参数
* 这表示方法调用方传递了非法参数，如需要接受一个非空指针，但是调用方实际传递了个空指针；或者调用方传递了一个错误的文件路径.
* 请注意，该错误仅应用于参数能被简单地校验失败的情况下.
* 对于传入的参数不符合系统状态的情况，你应该返回
* WXERROR_FAILED_PRECONDITION.
* WXERROR_INVALID_ARGUMENT = 3

* @brief 调用超时
* 这表示操作应该在指定时间内完成，但并未完成.
* WXERROR_TIMEOUT = 4

* @brief 被请求的实体不存在
* 如文件不存在.
* WXERROR_NOT_FOUND = 5

* @brief 希望创建的实体已存在
* 如要创建的文件已经存在.
* WXERROR_ALREADY_EXISTS = 6

* @brief 没有权限
* 这表示调用方无权调用指定的操作.
* 需要与 WXERROR_UNAUTHENTICATED 进行区分.
* WXERROR_PERMISSION_DENIED = 7

* @brief 资源不足
* 可能表示磁盘空间已满, 或网络不连通.
* WXERROR_RESOURCE_EXHAUSTED = 8

* @brief 前置条件不满足
* 这表示当前操作因系统不在可以运行的状态中而被拒绝执行.
* 比如要删除文件夹时，指定的路径上实际是个普通文件.
* WXERROR_FAILED_PRECONDITION = 9

* @brief 操作被中断
* WXERROR_ABORTED = 10

* @brief 超出范围
* 这表示当前操作超出了允许的范围，如读取文件时超出了文件长度.
* WXERROR_OUT_OF_RANGE = 11

* @brief 操作未实现
* 这表示当前操作尚未被实现/被支持. 因此当前操作不应该重试.
* WXERROR_UNIMPLEMENTED = 12

* @brief 内部错误
* 这表示当前操作因为系统状态不正常而失败. 这通常表示系统内部有 bug.
* WXERROR_INTERNAL = 13

* @brief 服务不可用
* 这表示操作尚不可用.
* 比如要播放音频时，音频设备未连接；或者要采集音频时，话筒设备未连接.
* WXERROR_UNAVAILABLE = 14

* @brief 数据丢失
* WXERROR_DATA_LOSS = 15

* WXERROR_UNAUTHENTICATED = 16

* @brief IO 错误
* 一般是文件创建、读、写。
* WXERROR_IO = 17

* @brief 回复错误
* 一般是网络返回内容不对
* WXERROR_RESPONSE = 18

* @brief 设备信息不匹配
* 一般是传入的 sn、modelid 与原注册信息不匹配
* WXERROR_INVALID_DEVICEID = 19