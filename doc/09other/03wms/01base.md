### 业务
### 备份数据-整个库
```
SCRIPT TO 'C:/temp/my_db_backup.sql';
```
### 备份数据-部分表
```
SCRIPT TO 'C:/temp/workflow_org_data.sql' TABLE app_user, app_department, app_user_group, app_role, user_groups;
```
### 流程图
1. 业务，对话生成文字版本的流程结构
2. 数据，通过到数据库导出关键表
3. 注意事项：
   1. Camunda 7.15 版本开始，为了更好地管理历史数据、防止数据库无限膨胀，引擎默认强制要求为每个流程定义设置一个“历史存活时间”（History TTL）
   2. 在排他网关（Exclusive Gateway）中，如果有一条路径没有设置流转条件（Condition），那么它应该被明确地标记为“默认流”（Default Flow）

### 进度
1. 创建部门
2. 创建角色
3. 创建用户
4. 创建表单
5. 创建流程图
