### Camunda说明
输出文档，对Camunda的使用说明进行分类，要求种类齐全，每一种用50字简单介绍即可，因为内容过多，分多次输出，每次1000行内容。
### Camunda入门
对内容进行补充，特别是操作方面，支持初学者阅读，版本为Camunda7，按钮汉化了，已内嵌 Camunda 7，因为要作为文档，内容过多，分多次输出，首次说明分几次。
### Camunda详细说明
对内容进行补充，特别是操作方面，版本为Camunda7，按钮汉化版本，内嵌 Camunda 7，因为输出内容要作为文档，要求内容完整，因为内容过多，分多次输出，每次1000行内容，同一个模块放在同一次回答，首次说明分几次。
### 笔记
1. 排他网关之后的线再设置条件
2. ${day>3}
3. 表单有内置和外置表单
4. 设计器的汉化能否继续优化
5. 要把外部系统与Camunda数据打通
6. 总结Camunda的几个系统的所有api
7. 熟悉camunda操作
    ```
        User user1 = new User();
        user1.setId("user001");
        user1.setName("普通员工-张三");
        user1.setRole("USER");
        user1.setPassword(defaultPassword);
    
        User user2 = new User();
        user2.setId("manager001");
        user2.setName("部门经理-李四");
        user2.setRole("USER");
        user2.setPassword(defaultPassword);
    
        User user3 = new User();
        user3.setId("hr001");
        user3.setName("财务总监-王五");
        user3.setRole("USER");
        user3.setPassword(defaultPassword);
    
        User admin = new User();
        admin.setId("admin");
        admin.setName("系统管理员");
        admin.setRole("ADMIN");
    ```
8. 用户没有授权为什么能够新建用户，默认拥有全部权限
9. 控制台没有可以部署流程图的位置
10. Camunda 8：不支持嵌入式，只能作为独立服务运行，通过 API（gRPC/REST）交互
11. 过滤器用于筛选任务
12. 较新版本的 Camunda 引擎为了防止历史数据无限膨胀，强制要求为每个流程定义设置一个“存活时间”（Time To Live, TTL）。当一个流程实例完成后，它会在历史数据库中再保留 TTL 所指定的时间，之后 Camunda 的历史清理作业会自动删除它。
13. ${taskAssignmentListener}
14. ${nextAssignee} 指定人
15. 
