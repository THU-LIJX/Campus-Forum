# 后端设计文档
后端采用分层的设计方式，controller层负责转发请求给model层（完成参数校验，保证参数合法性）

model层是数据操作逻辑。

store层对数据库操作进行一定的抽象

路由注册在router.go中

counter用于维护全局的id（即总数）。可能最好是直接给定而不是用id去查询。而且每个都是单例

接口的设计更改一下，user开头的表示的是单个用户的操作。全都需要用户身份验证
其他的不需要

草稿界面完全由前端实现。后端不存

修改个人信息方便头像应该是一个单独的信息。

本人发布所有动态最好分页

获取动态采用get就可以。要满足几个筛选
时间、点赞数、是否已经关注、是否自己动态

提供统一的接口，前端只要在query中加上条件。

page limit id

## mongodb
mongodb管理员配置：https://blog.csdn.net/HH_KELE/article/details/105804643

用户名 campus 密码campus 数据库 test（测试用