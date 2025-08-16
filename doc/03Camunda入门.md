### **Camunda 7 入门实践指南 (基于 Spring Boot)**

本指南旨在帮助初学者快速上手 Camunda 7，特别是如何在 Spring Boot 环境中进行嵌入式开发。我们将从搭建环境开始，一步步创建、部署并执行您的第一个业务流程。

### **第 1 章：环境准备与第一个“Hello World”流程**

在本章中，我们将完成以下目标：
1.  搭建一个集成了 Camunda 7 的 Spring Boot 项目。
2.  下载并熟悉流程建模工具 Camunda Modeler。
3.  设计一个包含“开始-审批-结束”的简单流程。
4.  运行项目，并通过 Camunda 的 Web 应用启动和处理这个流程。

#### **1.1 项目搭建 (基于提供的 `pom.xml`)**

您的 `pom.xml` 文件已经为我们奠定了坚实的基础。让我们来理解其中的关键部分。

1.  **核心依赖**：
    `pom.xml` 中最重要的依赖是 `camunda-bpm-spring-boot-starter-webapp`。

    ```xml
    <dependency>
        <groupId>org.camunda.bpm.springboot</groupId>
        <artifactId>camunda-bpm-spring-boot-starter-webapp</artifactId>
    </dependency>
    ```

    这个 “starter” 会自动为您的 Spring Boot 应用做以下事情：
    *   **内嵌流程引擎**: 将 Camunda 7 流程引擎作为库集成到您的应用中。
    *   **配置数据源**: 自动配置一个内存数据库（H2），用于存储流程数据，无需额外配置即可启动。
    *   **集成 Web 应用**: 内嵌了 Camunda 的三大 Web 应用：**Cockpit** (运维监控)、**Tasklist** (任务处理) 和 **Admin** (用户管理)。
    *   **启用 REST API**: 提供了完整的 RESTful 接口，方便外部程序与流程引擎交互。

2.  **创建 Spring Boot 项目**:
    您可以直接使用您提供的 `pom.xml`。在一个新的 Maven 项目中替换 `pom.xml` 文件，并确保您的项目结构如下：

    ```
    workflow-engine/
    ├── src/
    │   ├── main/
    │   │   ├── java/
    │   │   │   └── club/ppmc/
    │   │   │       └── WorkflowEngineApplication.java  <-- Spring Boot 启动类
    │   │   └── resources/
    │   │       ├── application.yml                 <-- 配置文件
    │   │       └── static/
    ├── pom.xml                                     <-- 您的 pom.xml
    ```

3.  **添加配置文件 (`application.yml`)**:
    在 `src/main/resources/` 目录下创建一个 `application.yml` 文件，并添加一些基础配置。

    ```yaml
    server:
      port: 8080 # 应用端口

    spring:
      application:
        name: workflow-engine

    camunda:
      bpm:
        # 启用 Camunda Webapps
        webapp:
          enabled: true
        # 流程引擎常规配置
        generic-properties:
          properties:
            # 自动创建和更新数据库表结构
            database-schema-update: true
        # 管理员用户配置
        admin-user:
          id: admin
          password: admin
          firstName: Admin
          lastName: User
    ```
    **说明**:
    *   `database-schema-update: true` 告诉 Camunda 在启动时自动检查并创建所需的数据库表，对于开发非常方便。
    *   我们创建了一个默认的管理员用户 `admin/admin`，用于首次登录 Web 应用。

#### **1.2 下载并熟悉 Camunda Modeler**

Camunda Modeler 是我们设计流程图的可视化工具。它是一个免费的桌面应用。

*   **下载地址**: [Camunda Modeler 官网](https://camunda.com/download/modeler/)
*   **界面简介**: 打开 Modeler 后，界面主要分为三个区域：
    1.  **左侧工具栏**: 提供了各种 BPMN 元素（如任务、网关、事件），可以拖拽到画布上。
    2.  **中间画布**: 您的流程设计区域。
    3.  **右侧属性面板**: 当您选中画布上的任何一个元素时，这里会显示并允许您配置它的所有属性（如 ID, 名称, 处理人等）。**这是我们工作的核心区域**。



#### **1.3 创建第一个流程图：“请假申请”**

现在，让我们动手创建一个简单的请假申请流程。

1.  **新建 BPMN 图**:
    打开 Camunda Modeler，点击 `Create a new BPMN diagram`。

2.  **设计流程**:
    *   **拖拽元素**: 从左侧工具栏拖拽一个 `用户任务 (User Task)` 到开始事件（圆圈）和结束事件（加粗圆圈）之间。
    *   **连接元素**: 您的图看起来应该是：`开始事件 -> 用户任务 -> 结束事件`。它们之间由带箭头的 `顺序流 (Sequence Flow)` 连接。



3.  **配置流程属性 (核心步骤)**:
    这是将图纸变为可执行程序的关键。

    *   **配置流程本身 (Process)**:
        *   点击画布的空白处，右侧属性面板会显示流程的属性。
        *   在 `General` 标签页下：
            *   **Id**: `leave-application-process` (**非常重要!** 这是流程的唯一标识符，代码中通过它来启动流程。必须是驼峰式或用-连接的字符串)。
            *   **Name**: `请假申请流程` (这是给人看的名字，可以随便起)。
            *   **Executable**: **必须勾选!** 否则引擎会认为这只是一个图纸，不会执行它。



    *   **配置用户任务 (User Task)**:
        *   点击中间的那个矩形任务节点。
        *   在 `General` 标签页下：
            *   **Id**: `task_approve_leave`
            *   **Name**: `审批请假单`
        *   切换到 `Assignments` (分配) 标签页：
            *   **Assignee (处理人)**: 输入 `demo`。这表示这个任务将直接分配给一个用户名为 `demo` 的人来处理。（Camunda Webapp 默认会创建一个 `demo` 用户，方便我们测试）。



4.  **保存流程文件**:
    *   点击 `File -> Save File As...`。
    *   将文件保存在您的 Spring Boot 项目的 `src/main/resources/` 目录下。
    *   **文件名**: `leave-process.bpmn`。
    *   **说明**: 放在 `resources` 目录下的 `.bpmn` 文件，Spring Boot 在启动时会自动扫描并部署到流程引擎中。

#### **1.4 启动应用并与流程交互**

现在，一切准备就绪！

1.  **运行项目**:
    *   回到您的 IDE (如 IntelliJ IDEA 或 Eclipse)，找到 `WorkflowEngineApplication.java` 文件。
    *   右键点击，选择 `Run 'WorkflowEngineApplication'`。
    *   等待控制台输出 Spring Boot 和 Camunda 的启动日志，没有报错即表示成功。

2.  **登录 Camunda Web 应用**:
    *   打开浏览器，访问 `http://localhost:8080`。
    *   您会被重定向到登录页面。输入我们之前配置的管理员账号：`admin` / `admin`。
    *   登录后，您会看到 Camunda Welcome 页面，上面有三个应用入口：**Cockpit**, **Admin**, **Tasklist**。

3.  **切换到 `demo` 用户 (可选但推荐)**:
    为了模拟真实的用户操作，我们退出管理员，用 `demo` 用户登录。
    *   点击右上角 `admin` -> `Logout`。
    *   在登录页输入 `demo` / `demo` 并登录。

4.  **通过 Tasklist 启动并完成流程**:
    *   **进入 Tasklist**: 点击 `Tasklist` 应用。
    *   **启动流程**: 点击右上角的 `Start process` 按钮。在弹出的列表中，您应该能看到我们刚才部署的 “请假申请流程”。点击它启动一个流程实例。
    *   **处理任务**:
        *   流程启动后，因为我们将任务分配给了 `demo` 用户，所以 `demo` 的任务列表里会立刻出现一个名为 “审批请假单” 的任务。
        *   点击左侧 `All tasks`，您会看到这个任务。
        *   点击任务，右侧会显示任务详情。
        *   点击右下角的 `Complete` 按钮来完成任务。



5.  **在 Cockpit 中验证流程状态**:
    *   登出 `demo` 用户，重新用 `admin` / `admin` 登录。
    *   进入 **Cockpit** 应用。
    *   在主界面，您可以看到 “请假申请流程” 的定义。点击它。
    *   您会看到该流程有一个 “已完成实例 (Finished Instances)”。点击查看，您可以看到刚才我们完成的那个流程实例的完整执行路径，所有节点都变成了绿色。

    

### **第 2 章：核心开发 - 连接代码与模型**

在本章中，我们将学习 Camunda 开发中最核心的两个概念：**服务任务 (Service Task)** 和 **流程变量 (Process Variable)**。我们将扩展上一章的“请假申请”流程，实现以下目标：

1.  理解并使用流程变量来传递数据。
2.  通过 **服务任务**，在流程中自动执行一段 Java 代码（例如，发送通知）。
3.  通过 **生成式表单 (Generated Forms)**，让用户在启动流程和处理任务时能够输入和查看数据。

#### **2.1 流程变量 (Process Variable) - 流程的记忆**

想象一下，一个真实的请假流程，我们需要知道是谁在请假、请假事由是什么、请了几天假。这些信息需要在流程的各个环节中传递。**流程变量**就是用来存储和传递这些数据的载体。

*   **它的生命周期**: 每个流程实例都有自己独立的一套变量。变量在流程实例启动时被创建，随着流程的流转被读取和修改，直到流程实例结束。
*   **它的作用**:
    *   存储业务数据（如订单号、客户ID、申请金额）。
    *   控制流程走向（例如，在网关处根据变量 `amount > 1000` 来决定是否需要更高级别的审批）。

我们很快就会在实践中使用到它。

#### **2.2 实现服务任务 (Service Task) - 让系统自动干活**

**服务任务**代表流程中一个由系统自动完成的步骤，无需人工干预。例如：调用一个外部API、向数据库写入记录、发送一封邮件等。

在 Spring Boot 环境中，最推荐的实现方式是 **Delegate Expression**。

**场景**: 我们来修改流程，在审批任务创建后，系统自动“发送一个通知”，告诉相关人员有新的审批单。

1.  **修改 BPMN 模型**:
    *   打开上一章创建的 `leave-process.bpmn` 文件。
    *   在 “开始事件” 和 “审批请假单” 任务之间，插入一个 **服务任务 (Service Task)**。可以先删除它们之间的连线，然后从左侧工具栏拖入服务任务，再重新连接它们。
    *   流程图现在应该是：`开始事件 -> 服务任务 -> 用户任务 -> 结束事件`。



2.  **配置服务任务**:
    *   选中新添加的服务任务。
    *   在右侧的 **属性面板 (Properties Panel)** 中：
        *   **General** 标签页:
            *   **Id**: `task_send_notification`
            *   **Name**: `发送通知`
        *   **Implementation** 标签页 (核心配置):
            *   **Implementation**: 从下拉框中选择 `Delegate Expression`。
            *   **Delegate Expression**: 输入 `${sendNotificationDelegate}`。

    **配置解读**:
    *   `Delegate Expression` 告诉 Camunda 引擎：“当流程执行到这个节点时，请到 Spring 的应用上下文中，找到一个名为 `sendNotificationDelegate` 的 Bean，并执行它的逻辑。”
    *   `${...}` 是 JUEL 表达式的语法。



3.  **编写 Java 代码 (JavaDelegate)**:
    现在，我们需要在 Java 代码中提供这个名为 `sendNotificationDelegate` 的 Bean。

    *   在 `club.ppmc` 包下创建一个新的子包 `delegates`。
    *   在 `delegates` 包下创建一个新的 Java 类 `SendNotificationDelegate`。
    *   让这个类实现 `org.camunda.bpm.engine.delegate.JavaDelegate` 接口，并实现其 `execute` 方法。

    ```java
    package club.ppmc.delegates;

    import org.camunda.bpm.engine.delegate.DelegateExecution;
    import org.camunda.bpm.engine.delegate.JavaDelegate;
    import org.slf4j.Logger;
    import org.slf4j.LoggerFactory;
    import org.springframework.stereotype.Component;

    // @Component注解让Spring能够发现这个Bean
    // "sendNotificationDelegate" 是这个Bean的名字，必须与BPMN模型中的Delegate Expression完全一致
    @Component("sendNotificationDelegate")
    public class SendNotificationDelegate implements JavaDelegate {

        private final Logger logger = LoggerFactory.getLogger(SendNotificationDelegate.class);

        @Override
        public void execute(DelegateExecution execution) throws Exception {
            // DelegateExecution 对象是与流程引擎交互的桥梁
            // 我们可以通过它获取流程变量、流程实例ID等信息

            // 从流程变量中获取申请人和请假事由
            String requesterName = (String) execution.getVariable("requesterName");
            String leaveReason = (String) execution.getVariable("leaveReason");

            // 模拟发送通知
            logger.info("--- 发送通知任务 ---");
            logger.info("通知：{} 提交了请假申请，事由为：{}", requesterName, leaveReason);
            logger.info("流程实例ID: {}", execution.getProcessInstanceId());
            logger.info("--- 通知发送完毕 ---");
        }
    }
    ```
    **代码解读**:
    *   `@Component("sendNotificationDelegate")`: 这是最关键的一步，它将这个 Java 类注册为 Spring Bean，并赋予其一个名字，这个名字与我们在 BPMN 中配置的 `Delegate Expression` 完美对应。
    *   `implements JavaDelegate`: 这是 Camunda 规定的，所有服务任务的逻辑类都必须实现这个接口。
    *   `execute(DelegateExecution execution)`: 这是逻辑的入口。当流程走到这个服务任务时，Camunda 就会调用这个方法。
    *   `execution.getVariable("...")`: 我们通过 `execution` 对象，使用 `getVariable` 方法来获取名为 `requesterName` 和 `leaveReason` 的流程变量。

#### **2.3 用户任务与表单 - 实现数据交互**

我们的服务任务现在需要 `requesterName` 和 `leaveReason` 这两个变量，但是目前流程里还没有地方可以输入它们。接下来，我们将通过 **生成式表单 (Generated Forms)** 来解决这个问题。

1.  **配置启动表单 (Start Event Form)**:
    我们希望用户在启动流程时就能填写请假信息。

    *   在 Camunda Modeler 中，选中 **开始事件 (Start Event)**。
    *   在右侧属性面板，切换到 **Forms** 标签页。
    *   点击 `Add Field` 按钮，添加两个字段：
        *   **第一个字段**:
            *   **Id**: `requesterName` (**重要**: 这将成为流程变量的名称)
            *   **Type**: `string`
            *   **Label**: `申请人姓名` (显示在表单上的标签)
        *   **第二个字段**:
            *   **Id**: `leaveReason`
            *   **Type**: `string`
            *   **Label**: `请假事由`



2.  **配置审批表单 (User Task Form)**:
    审批人需要看到申请人提交的信息。

    *   选中 **“审批请假单”** 这个用户任务。
    *   同样在 **Forms** 标签页，添加和上面 **完全相同** 的两个字段（`Id`, `Type`, `Label` 都一样）。
    *   **关键区别**: 勾选这两个字段后面的 **Read only** 复选框。这样，审批人就只能查看而不能修改这些信息了。



3.  **保存并重启**:
    *   点击 `File -> Save` 保存您的 `leave-process.bpmn` 文件。
    *   回到 IDE，**停止** 正在运行的应用，然后 **重新运行** `WorkflowEngineApplication`。Camunda 在重启时会自动部署我们修改后的新版本流程。

#### **2.4 再次运行与验证**

现在，我们的流程已经变得更加智能了。让我们来测试一下。

1.  **登录并启动流程**:
    *   用 `demo` / `demo` 登录 `http://localhost:8080`。
    *   进入 **Tasklist**，点击 `Start process` -> `请假申请流程`。
    *   **看！** 这次弹出的不再是一个空对话框，而是我们刚刚定义的表单。
    *   在 “申请人姓名” 中输入 “张三”，在 “请假事由” 中输入 “参加朋友婚礼”。
    *   点击 `Start` 按钮。



2.  **检查服务任务的执行**:
    *   立刻切换到您的 IDE 控制台。
    *   您应该能看到 `SendNotificationDelegate` 中打印的日志！

    ```
    --- 发送通知任务 ---
    通知：张三 提交了请假申请，事由为：参加朋友婚礼
    流程实例ID: ...
    --- 通知发送完毕 ---
    ```
    这证明了流程启动后，我们填写的表单数据成功地变成了流程变量，并且服务任务正确地获取并使用了这些变量。

3.  **处理审批任务**:
    *   回到浏览器的 Tasklist 页面。
    *   `demo` 用户的任务列表里出现了 “审批请假单” 任务。
    *   点击该任务，右侧的表单中会显示我们刚才输入的 “张三” 和 “参加朋友婚礼”，并且是只读的。
    *   点击 `Complete` 完成任务。

4.  **在 Cockpit 中最终验证**:
    *   登出 `demo`，用 `admin` / `admin` 登录。
    *   进入 **Cockpit**，找到刚刚完成的那个 “请假申请流程” 实例。
    *   点击进入实例详情，切换到 **Variables** 标签页。
    *   您可以看到，`requesterName` 和 `leaveReason` 这两个变量作为历史记录被完整地保存了下来。

    

### **第 3 章：流程控制与前端集成**

在本章中，我们将掌握流程中最强大的特性之一：**网关 (Gateway)**，它能让我们的流程根据条件“思考”和“决策”。最后，我们将利用您提供的汉化文件，构建一个定制化的中文流程设计器。

本章目标：
1.  使用 **排他网关 (Exclusive Gateway)** 来处理“批准”或“驳回”的决策。
2.  在用户任务表单中添加决策控件，并将其结果存为流程变量。
3.  根据流程变量的值，控制流程的走向。
4.  集成 `bpmn-js` 和您的汉化脚本，在浏览器中呈现一个全中文的流程建模器。

#### **3.1 使用排他网关实现流程决策**

**场景**: 在我们的“请假申请”流程中，审批人看完请假单后，应该有两个选择：【批准】或【驳回】。流程需要根据这个选择走向不同的后续步骤。

1.  **修改 BPMN 模型**:
    *   打开 `leave-process.bpmn` 文件。
    *   删除 “审批请假单” 和 “结束事件” 之间的连线。
    *   从左侧工具栏拖拽一个 **排他网关 (Exclusive Gateway)** (空心菱形图标) 放到 “审批请假单” 后面。
    *   从网关出发，拉出两条路径，分别连接到两个新的 **服务任务 (Service Task)** 上。
    *   最后，让这两个服务任务都汇合到一个 **结束事件 (End Event)** 上。



2.  **配置新元素**:
    *   **第一个服务任务**:
        *   **Id**: `task_notify_approval`
        *   **Name**: `通知申请人-已批准`
        *   **Implementation**: `Delegate Expression` -> `${notifyApprovalDelegate}`
    *   **第二个服务任务**:
        *   **Id**: `task_notify_rejection`
        *   **Name**: `通知申请人-已驳回`
        *   **Implementation**: `Delegate Expression` -> `${notifyRejectionDelegate}`
    *   **配置从网关出来的连线 (核心步骤)**:
        *   **选中通往“已批准”任务的连线** (Sequence Flow)。
        *   在右侧属性面板的 **General** 标签页下：
            *   **Name**: `批准`
        *   切换到 **Condition** 标签页：
            *   **Condition Type**: 选择 `Expression`。
            *   **Expression**: 输入 `${approved == true}`。
        *   **选中通往“已驳回”任务的连线**。
        *   在 **General** 标签页下：
            *   **Name**: `驳回`
        *   切换到 **Condition** 标签页：
            *   **Condition Type**: 选择 `Expression`。
            *   **Expression**: 输入 `${approved == false}`。

    **配置解读**:
    *   当流程执行到排他网关时，它会检查一个名为 `approved` 的流程变量。
    *   如果 `approved` 的值是 `true`，流程就会沿着条件为 `${approved == true}` 的那条线继续。
    *   如果值是 `false`，则走另一条线。
    *   **注意**: 这两条路径是互斥的，只会选择一条。

3.  **更新审批表单**:
    审批人需要一个方式来设置 `approved` 这个变量。

    *   选中 “审批请假单” 用户任务。
    *   在 **Forms** 标签页，点击 `Add Field` 添加一个新字段：
        *   **Id**: `approved` (**必须与网关条件中的变量名一致**)
        *   **Type**: `boolean` (布尔类型，会生成一个复选框)
        *   **Label**: `是否批准？`



4.  **编写新的 Java Delegate**:
    我们需要为 `notifyApprovalDelegate` 和 `notifyRejectionDelegate` 提供 Java 实现。

    *   在 `delegates` 包下创建 `NotifyApprovalDelegate.java`:
    ```java
    package club.ppmc.delegates;

    import org.camunda.bpm.engine.delegate.DelegateExecution;
    import org.camunda.bpm.engine.delegate.JavaDelegate;
    import org.slf4j.Logger;
    import org.slf4j.LoggerFactory;
    import org.springframework.stereotype.Component;

    @Component("notifyApprovalDelegate")
    public class NotifyApprovalDelegate implements JavaDelegate {
        private final Logger logger = LoggerFactory.getLogger(NotifyApprovalDelegate.class);

        @Override
        public void execute(DelegateExecution execution) throws Exception {
            String requesterName = (String) execution.getVariable("requesterName");
            logger.info("--- 发送批准通知 ---");
            logger.info("通知：{}，您的请假申请已被批准。", requesterName);
            logger.info("--- 通知发送完毕 ---");
        }
    }
    ```

    *   在 `delegates` 包下创建 `NotifyRejectionDelegate.java`:
    ```java
    package club.ppmc.delegates;

    import org.camunda.bpm.engine.delegate.DelegateExecution;
    import org.camunda.bpm.engine.delegate.JavaDelegate;
    import org.slf4j.Logger;
    import org.slf4j.LoggerFactory;
    import org.springframework.stereotype.Component;

    @Component("notifyRejectionDelegate")
    public class NotifyRejectionDelegate implements JavaDelegate {
        private final Logger logger = LoggerFactory.getLogger(NotifyRejectionDelegate.class);

        @Override
        public void execute(DelegateExecution execution) throws Exception {
            String requesterName = (String) execution.getVariable("requesterName");
            logger.info("--- 发送驳回通知 ---");
            logger.info("通知：{}，抱歉，您的请假申请已被驳回。", requesterName);
            logger.info("--- 通知发送完毕 ---");
        }
    }
    ```

5.  **测试决策流程**:
    *   保存 BPMN 文件，然后重启 Spring Boot 应用。
    *   **测试批准路径**:
        1.  用 `demo` 登录，启动一个 “请假申请流程”，填写 “张三” 和 “旅游”。
        2.  在 Tasklist 中处理 “审批请假单” 任务。您会看到多了一个 “是否批准？” 的复选框。**勾选它**。
        3.  点击 `Complete`。
        4.  检查 IDE 控制台，您应该看到 “--- 发送批准通知 ---” 的日志。
    *   **测试驳回路径**:
        1.  再次启动一个新流程，填写 “李四” 和 “休息”。
        2.  处理审批任务时，**不要勾选** “是否批准？” 复选框。
        3.  点击 `Complete`。
        4.  检查 IDE 控制台，这次您应该看到 “--- 发送驳回通知 ---” 的日志。
    *   在 **Cockpit** 中，您可以看到两个已完成的流程实例，它们的执行路径清晰地显示了不同的决策结果。

#### **3.2 集成汉化版 Web 流程设计器**

现在，让我们来完成最后一个激动人心的任务：构建一个我们自己的、基于网页的、全中文的 BPMN 设计器。

`bpmn-js` 是一个强大的 JavaScript 库，它正是 Camunda Modeler 背后的渲染和编辑引擎。我们可以用它在任何网页中嵌入流程图。

1.  **准备文件**:
    *   在 `src/main/resources/static/` 目录下创建一个 `index.html` 文件。
    *   将您提供的 `customTranslate.js` 文件也放到 `src/main/resources/static/` 目录下。

2.  **编写 `index.html`**:
    将以下内容完整地复制到 `index.html` 文件中。

    ```html
    <!DOCTYPE html>
    <html lang="en">
    <head>
        <meta charset="UTF-8">
        <title>中文 Camunda 流程设计器</title>
        <!-- 引入 bpmn-js 的样式文件 -->
        <link rel="stylesheet" href="https://unpkg.com/bpmn-js@17.0.2/dist/assets/bpmn-js.css">
        <link rel="stylesheet" href="https://unpkg.com/bpmn-js-properties-panel/dist/assets/properties-panel.css">
        
        <style>
            html, body {
                height: 100%;
                margin: 0;
                font-family: sans-serif;
            }
            .container {
                display: flex;
                height: 100%;
                width: 100%;
            }
            #canvas {
                flex-grow: 1;
                height: 100%;
            }
            #properties {
                width: 300px;
                background: #f8f8f8;
                height: 100%;
                overflow-y: auto;
            }
        </style>
    </head>
    <body>

    <div class="container">
        <div id="canvas"></div>
        <div id="properties"></div>
    </div>

    <!-- 引入 bpmn-js 核心库 -->
    <script src="https://unpkg.com/bpmn-js@17.0.2/dist/bpmn-modeler.development.js"></script>
    <!-- 引入属性面板库 -->
    <script src="https://unpkg.com/bpmn-js-properties-panel/dist/bpmn-js-properties-panel.umd.js"></script>
    <script src="https://unpkg.com/camunda-bpmn-js-behaviors/dist/camunda-bpmn-js-behaviors.umd.js"></script>
    <script src="https://unpkg.com/camunda-bpmn-js-properties-panel-provider/dist/camunda-bpmn-js-properties-panel-provider.umd.js"></script>

    <script type="module">
        // 1. 从我们本地的 customTranslate.js 文件中导入汉化模块
        import customTranslateModule from './customTranslate.js';

        // 2. 获取 DOM 元素
        const canvas = document.getElementById('canvas');
        const propertiesPanel = document.getElementById('properties');
        
        // 3. 实例化 BpmnJS Modeler
        const modeler = new BpmnJS({
            container: canvas,
            propertiesPanel: {
                parent: propertiesPanel
            },
            // 4. 注入我们的汉化模块和属性面板模块
            additionalModules: [
                customTranslateModule, // 注入汉化模块！
                BpmnJSPropertiesPanelModule,
                BpmnJSPropertiesPanelProviderModule,
                CamundaBpmnJsBehaviorsModule,
            ],
            // 5. 配置属性面板
            moddleExtensions: {
                camunda: CamundaBpmnJsBehaviorsModule.camundaModdleDescriptor
            }
        });

        // 示例 BPMN XML
        const defaultBpmnXML = `<?xml version="1.0" encoding="UTF-8"?>
        <bpmn:definitions xmlns:bpmn="http://www.omg.org/spec/BPMN/20100524/MODEL" xmlns:bpmndi="http://www.omg.org/spec/BPMN/20100524/DI" xmlns:dc="http://www.omg.org/spec/DD/20100524/DC" xmlns:camunda="http://camunda.org/schema/1.0/bpmn" xmlns:di="http://www.omg.org/spec/DD/20100524/DI" id="Definitions_1" targetNamespace="http://bpmn.io/schema/bpmn">
          <bpmn:process id="Process_1" isExecutable="true">
            <bpmn:startEvent id="StartEvent_1" name="开始"/>
          </bpmn:process>
          <bpmndi:BPMNDiagram id="BPMNDiagram_1">
            <bpmndi:BPMNPlane id="BPMNPlane_1" bpmnElement="Process_1">
              <bpmndi:BPMNShape id="_BPMNShape_StartEvent_2" bpmnElement="StartEvent_1">
                <dc:Bounds x="173" y="102" width="36" height="36" />
                <bpmndi:BPMNLabel>
                  <dc:Bounds x="179" y="145" width="22" height="14" />
                </bpmndi:BPMNLabel>
              </bpmndi:BPMNShape>
            </bpmndi:BPMNPlane>
          </bpmndi:BPMNDiagram>
        </bpmn:definitions>`;

        // 导入示例图
        async function openDiagram(bpmnXML) {
            try {
                await modeler.importXML(bpmnXML);
                console.log('流程图导入成功!');
            } catch (err) {
                console.error('导入BPMN时出错', err);
            }
        }
        
        openDiagram(defaultBpmnXML);
    </script>
    </body>
    </html>
    ```

3.  **最终验证**:
    *   再次重启您的 Spring Boot 应用。
    *   这次，在浏览器中打开 `http://localhost:8080/index.html`。
    *   **见证奇迹！** 您会看到一个功能齐全的流程设计器出现在您的网页中。
        *   **左侧工具栏**: 所有元素的提示文字都是中文（例如，“创建用户任务”）。
        *   **右侧属性面板**: 所有的标签、字段、按钮都是中文（例如，“常规”、“名称”、“ID”、“监听器”）。
        *   这一切都得益于您提供的 `customTranslate.js` 和我们刚刚的集成工作。

### **教程总结**

**恭喜您！** 通过这三章的学习，您已经从零开始，完整地体验了 Camunda 7 嵌入式开发的全过程：

*   **环境搭建**: 您成功地在 Spring Boot 中内嵌了 Camunda 7 引擎和 Web 应用。
*   **流程建模**: 您学会了使用 Camunda Modeler 设计包含了 **用户任务**、**服务任务** 和 **排他网关** 的业务流程。
*   **后端开发**: 您掌握了通过 `JavaDelegate` 将业务逻辑与流程模型绑定的核心技巧，并理解了 **流程变量** 的重要性。
*   **人机交互**: 您学会了使用 **生成式表单** 来实现简单的数据输入和展示。
*   **前端集成**: 您甚至构建了一个属于自己的、**汉化** 的 Web 流程设计器。

您现在已经具备了使用 Camunda 7 解决实际业务问题的基础能力。您最初提供的那份详尽的 `02Camunda的使用说明.md` 文档，现在对您来说不再是天书，而是一本可以随时查阅的、宝贵的参考手册。当您遇到更高级的需求，如定时器事件、子流程、错误处理等，都可以去查阅它，并结合本教程学到的实践方法去实现它们。

