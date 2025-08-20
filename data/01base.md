### 地址
http://localhost:8080/h2-console/
### 导出全部数据库数据
```
SCRIPT TO 'C:/temp/my_db_backup.sql';
```
### 导入全部数据
```
DROP ALL OBJECTS;
RUNSCRIPT FROM  'C:/temp/my_db_backup.sql';
```
### xx系统对应哪个sql
