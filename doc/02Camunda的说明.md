### **Camunda 使用说明分类文档 

**摘要**: 本文档旨在对 Camunda 平台（涵盖 Camunda 7 和 Camunda 8/Zeebe）的使用说明进行全面、系统性的分类。内容从基础概念、建模设计，到开发实现、部署运维等，力求覆盖 Camunda 全生命周期的各个方面。

---

### **1. 入门与核心概念 (Introduction & Core Concepts)**

本章节介绍 Camunda 的基础知识，帮助初学者理解其核心价值、基本术语和架构组成，为后续深入学习打下坚实基础。

*   **1.1. Camunda 平台概述 (Camunda Platform Overview)**
    *   **1.1.1. 什么是 Camunda? (What is Camunda?)**
        *   Camunda 是一个开源的工作流和决策自动化平台，用于编排、自动化和改进业务流程，核心是其强大的 BPMN、DMN 和 CMMN 引擎。
    *   **1.1.2. Camunda 的核心价值 (Core Value of Camunda)**
        *   实现业务流程的端到端自动化，提高效率、透明度和灵活性。通过可视化建模与IT实现相结合，弥合业务与技术之间的鸿沟。
    *   **1.1.3. Camunda 的应用场景 (Use Cases)**
        *   适用于各种需要流程编排的场景，如用户注册、订单处理、贷款审批、客户服务工单流转、微服务编排等复杂业务逻辑。
    *   **1.1.4. Camunda 平台版本对比 (Platform Version Comparison)**
        *   主要分为 Camunda Platform 7 和 Camunda Platform 8。C7 是成熟的嵌入式引擎；C8 是为云原生和微服务架构设计的、基于 Zeebe 引擎的全新平台。

*   **1.2. 核心标准与规范 (Core Standards & Specifications)**
    *   **1.2.1. BPMN 2.0 (Business Process Model and Notation)**
        *   一种业务流程建模的全球标准图形化表示法。它定义了一套标准的图形元素，用于清晰、直观地描述业务流程的各个环节。
    *   **1.2.2. DMN 1.3 (Decision Model and Notation)**
        *   一种对业务决策逻辑进行建模和执行的标准。通过决策表等形式，将复杂的业务规则与业务流程分离，使其更易于管理和变更。
    *   **1.2.3. CMMN 1.1 (Case Management Model and Notation)**
        *   一种用于非结构化、事件驱动型工作的案例管理标准。适用于知识工作者处理的、路径不固定、依赖于人工判断的场景。

*   **1.3. Camunda 平台架构 (Platform Architecture)**
    *   **1.3.1. Camunda Platform 7 架构**
        *   **1.3.1.1. 流程引擎 (Process Engine)**
            *   是 Camunda 的核心，负责解析BPMN模型并执行流程实例。可以作为库嵌入到Java应用中，也可以作为独立服务运行。
        *   **1.3.1.2. REST API**
            *   提供了一套完整的 RESTful 接口，允许任何语言或客户端与流程引擎进行交互，如启动流程、查询任务、完成任务等。
        *   **1.3.1.3. Web 应用 (Web Applications)**
            *   包括 Cockpit（监控运维）、Tasklist（人工任务处理）、Admin（用户权限管理），为不同角色提供了图形化操作界面。
        *   **1.3.1.4. Modeler (建模工具)**
            *   一个桌面应用程序，用于创建和编辑 BPMN、DMN 和 CMMN 图。提供了 Camunda 特定属性的配置面板，是流程设计的起点。
    *   **1.3.2. Camunda Platform 8 架构 (基于 Zeebe)**
        *   **1.3.2.1. Zeebe 流程引擎 (Zeebe Process Engine)**
            *   C8 的核心，是一个为大规模、高吞吐量场景设计的分布式流程引擎。采用事件流和无共享架构，具备水平扩展和容错能力。
        *   **1.3.2.2. Operate (监控工具)**
            *   C8 中的流程实例监控和故障排查工具，提供了对正在运行和已完成流程实例的实时可见性，功能类似于 C7 的 Cockpit。
        *   **1.3.2.3. Tasklist (任务列表)**
            *   C8 中的人工任务处理应用，用户可以在此查看并完成分配给自己的任务，与 C7 的 Tasklist 功能类似。
        *   **1.3.2.4. Web Modeler (在线建模工具)**
            *   C8 的主要建模工具，一个基于 Web 的协作式建模环境，支持 BPMN 和 DMN，并能直接部署模型到 Zeebe 引擎。
        *   **1.3.2.5. Connectors (连接器)**
            *   C8 的标准集成方式，用于将流程与外部系统（如REST API、Kafka、Slack等）连接。简化了服务任务的实现。
        *   **1.3.2.6. Identity (身份认证)**
            *   C8 的统一身份和访问管理组件，负责处理用户认证和授权，为各个组件提供单点登录服务。
        *   **1.3.2.7. gRPC API**
            *   Zeebe 引擎与外界交互的主要方式。客户端通过 gRPC 协议与 Zeebe 网关通信，以部署流程、创建实例、激活作业等。

*   **1.4. 关键术语解释 (Key Terminology)**
    *   **1.4.1. 流程定义 (Process Definition)**
        *   一个业务流程的模型或蓝图，通常是一个 BPMN 2.0 XML 文件。它描述了流程的结构、步骤和规则，是创建流程实例的模板。
    *   **1.4.2. 流程实例 (Process Instance)**
        *   流程定义的一次具体执行。每当一个流程被启动，就会创建一个新的流程实例，该实例拥有自己独立的状态和数据。
    *   **1.4.3. 活动/任务 (Activity/Task)**
        *   流程中的一个工作单元或步骤。例如，一个用户任务代表需要人工完成的工作，一个服务任务代表系统自动执行的逻辑。
    *   **1.4.4. 网关 (Gateway)**
        *   流程中的决策点，用于控制流程的走向。常见的有排他网关（选择一条路径）、并行网关（并发执行多条路径）等。
    *   **1.4.5. 事件 (Event)**
        *   在流程中发生的事情，会触发或影响流程的执行。例如，启动事件触发流程开始，定时器事件在特定时间触发后续路径。
    *   **1.4.6. 序列流 (Sequence Flow)**
        *   BPMN 图中带箭头的实线，用于连接流程中的各个元素（活动、事件、网关），定义了它们的执行顺序。
    *   **1.4.7. 流程变量 (Process Variable)**
        *   与单个流程实例相关联的数据。这些变量可以在流程的整个生命周期中被创建、读取、更新和删除，用于传递信息和控制流程逻辑。
    *   **1.4.8. 作业/Job (C7) & 作业/Job (C8)**
        *   在 C7 中，Job 通常指引擎需要异步执行的工作，如定时器或异步任务。在 C8 中，Job 是外部任务工作者需要拉取并处理的工作单元。
    *   **1.4.9. 外部任务 (External Task - C7)**
        *   一种服务任务模式，流程引擎将任务发布到一个任务列表中，外部的工作者（Worker）可以独立于引擎拉取并完成这些任务。
    *   **1.4.10. 工作者 (Job Worker - C8)**
        *   在 C8 中，负责执行服务任务逻辑的外部客户端。它通过 gRPC 连接到 Zeebe 引擎，订阅特定类型的作业，执行后向引擎报告结果。


### **2. 建模与设计 (Modeling & Design)**

本章节详细介绍如何使用 Camunda 支持的标准（BPMN, DMN, CMMN）进行流程、决策和案例的建模，以及如何使用相关工具高效完成设计工作。

*   **2.1. BPMN 2.0 流程建模 (BPMN 2.0 Process Modeling)**
    *   **2.1.1. BPMN 基础元素 (Basic BPMN Elements)**
        *   **2.1.1.1. 开始事件 (Start Event)**
            *   表示流程的起点。一个流程定义通常有一个或多个开始事件，用于定义流程如何被触发，例如无特定触发、消息触发或定时触发。
        *   **2.1.1.2. 结束事件 (End Event)**
            *   表示流程或某条路径的终点。可以有不同的类型，如普通结束、错误结束（抛出错误）或终止结束（强制终止整个流程实例）。
        *   **2.1.1.3. 序列流 (Sequence Flow)**
            *   定义流程中元素执行顺序的连接线。可以设置条件表达式，以根据流程变量的值决定是否沿着该路径继续执行。
        *   **2.1.1.4. 池与泳道 (Pools and Lanes)**
            *   池（Pool）代表一个独立的参与者（如一个公司或系统），泳道（Lane）用于组织池内不同角色或部门的活动。
    *   **2.1.2. 任务类型 (Task Types)**
        *   **2.1.2.1. 用户任务 (User Task)**
            *   定义流程中需要人工参与和处理的节点。通常与任务列表（Tasklist）应用集成，分配给特定用户或用户组，等待人工完成。
        *   **2.1.2.2. 服务任务 (Service Task)**
            *   定义流程中由系统自动执行的节点。其实现方式多样，可以是 Java Delegate、外部任务（C7）或 Job Worker（C8）。
        *   **2.1.2.3. 脚本任务 (Script Task)**
            *   允许在流程模型中直接嵌入一小段脚本（如 Groovy, JavaScript）来执行简单的业务逻辑，适合快速实现简单的自动化步骤。
        *   **2.1.2.4. 业务规则任务 (Business Rule Task)**
            *   用于在流程中调用一个 DMN 决策引擎来评估业务规则。根据输入数据，执行预定义的决策表并返回结果，实现业务逻辑解耦。
        *   **2.1.2.5. 发送任务 (Send Task)**
            *   一种特定类型的服务任务，专门用于向外部服务或消息中间件发送消息。在模型中语义更明确，表示一个单向的通信。
        *   **2.1.2.6. 接收任务 (Receive Task)**
            *   使流程实例暂停，等待接收一个特定的外部消息。当匹配的消息到达时，流程实例才会从该节点继续执行。
        *   **2.1.2.7. 人工任务 (Manual Task)**
            *   表示一个不由流程引擎管理和执行的任务，例如需要打电话或整理纸质文件。它仅作为流程文档的一部分，引擎会直接跳过。
        *   **2.1.2.8. 调用活动 (Call Activity)**
            *   用于在一个流程中调用另一个可复用的、独立的流程定义。这有助于实现流程的模块化和重用，使主流程更简洁。
    *   **2.1.3. 网关类型 (Gateway Types)**
        *   **2.1.3.1. 排他网关 (Exclusive Gateway / XOR)**
            *   决策点，流程会沿着第一个条件为真的出向序列流继续。如果多个条件为真，只选择一个；如果所有条件为假，可走默认流。
        *   **2.1.3.2. 并行网关 (Parallel Gateway / AND)**
            *   用于拆分或合并多条并发路径。拆分时，所有出向路径都会被同时激活；合并时，它会等待所有入向路径都到达后才继续。
        *   **2.1.3.3. 包容网关 (Inclusive Gateway / OR)**
            *   一种更灵活的决策点。它会评估所有出向序列流的条件，并激活所有条件为真的路径，可以同时执行一条或多条路径。
        *   **2.1.3.4. 事件驱动网关 (Event-Based Gateway)**
            *   流程在此等待，直到其后的某个中间事件被触发。第一个被触发的事件将决定流程的走向，其它事件监听器会被取消。
        *   **2.1.3.5. 复杂网关 (Complex Gateway)**
            *   用于处理复杂的同步和分支逻辑，不常用。其行为需要通过表达式来定义，通常建议使用其他网关组合来替代。
        *   **2.1.3.6. 并行事件驱动网关 (Parallel Event-Based Gateway)**
            *   与事件驱动网关不同，它会启动多个事件监听。但这个网关仅用于实例化流程，不能用于流程中间。
    *   **2.1.4. 事件类型 (Event Types)**
        *   **2.1.4.1. 定时器事件 (Timer Event)**
            *   在指定时间或周期性触发。可用作启动事件（定时启动流程）、中间事件（延迟执行）或边界事件（任务超时处理）。
        *   **2.1.4.2. 消息事件 (Message Event)**
            *   通过接收或发送具名消息来触发。可用于启动流程、在流程间通信或与外部系统通过消息进行交互。
        *   **2.1.4.3. 信号事件 (Signal Event)**
            *   用于广播通信。一个信号事件被抛出后，所有正在监听该信号的流程实例都会被触发，实现一对多的通信模式。
        *   **2.1.4.4. 错误事件 (Error Event)**
            *   用于在流程中处理已知的业务异常。可以由错误结束事件抛出，由错误边界事件或事件子流程捕获，进行专门的异常处理。
        *   **2.1.4.5. 升级事件 (Escalation Event)**
            *   类似于错误事件，但它不会中断抛出事件的子流程。通常用于将问题“升级”到上层流程进行处理，而原子流程继续执行。
        *   **2.1.4.6. 补偿事件 (Compensation Event)**
            *   用于实现事务的回滚逻辑。当一个事务被取消时，会触发已完成活动的补偿处理器，执行反向操作以撤销之前的影响。
        *   **2.1.4.7. 条件事件 (Conditional Event)**
            *   当一个预设的条件（通常涉及流程变量的变化）变为真时触发。可用于启动流程或在流程中间作为捕获事件。
        *   **2.1.4.8. 链接事件 (Link Event)**
            *   作为“Go-To”连接符使用，用于将一个复杂的流程图拆分成多个部分，以提高可读性。一个抛出链接对应一个捕获链接。
        *   **2.1.4.9. 终止事件 (Terminate End Event)**
            *   一个特殊的结束事件。当流程执行到此节点时，会立即强制终止整个流程实例，包括所有并发的分支和子流程。
    *   **2.1.5. 边界事件 (Boundary Events)**
        *   **2.1.5.1. 中断型边界事件 (Interrupting Boundary Event)**
            *   附加在活动（如任务或子流程）边界上的事件。当事件被触发时，会中断当前活动，并沿着边界事件引出的路径继续执行。
        *   **2.1.5.2. 非中断型边界事件 (Non-Interrupting Boundary Event)**
            *   与中断型类似，但当事件被触发时，不会中断当前活动。它会创建一个新的并发分支执行，原活动继续正常进行。
    *   **2.1.6. 子流程 (Sub-Processes)**
        *   **2.1.6.1. 嵌入式子流程 (Embedded Sub-Process)**
            *   将一组相关的活动聚合在一个可折叠的矩形框内，用于简化主流程图。子流程内的活动共享主流程的变量上下文。
        *   **2.1.6.2. 事件子流程 (Event Sub-Process)**
            *   一种特殊的子流程，没有入向序列流。它由其内部的开始事件触发（如错误、消息），用于处理在整个流程范围内的特定事件。
        *   **2.1.6.3. 事务子流程 (Transactional Sub-Process)**
            *   提供一个事务边界。如果子流程内部发生错误或被取消事件触发，所有已完成的活动都会执行补偿操作，实现ACID中的“A”（原子性）。
        *   **2.1.6.4. 多实例活动 (Multi-Instance Activity)**
            *   允许一个活动（任务或子流程）被多次执行。可以是并行的（同时为集合中每个元素创建实例）或串行的（依次执行）。
    *   **2.1.7. 数据对象 (Data Objects)**
        *   **2.1.7.1. 数据对象引用 (Data Object Reference)**
            *   在 BPMN 图上表示流程中产生、使用或存储的数据。它提供了关于流程所需数据及其状态的可视化文档。
        *   **2.1.7.2. 数据存储引用 (Data Store Reference)**
            *   表示流程需要访问的外部数据存储系统，如数据库或文件系统。它强调流程与外部持久化数据的交互。
    *   **2.1.8. Camunda 特定扩展属性 (Camunda-Specific Extensions)**
        *   **2.1.8.1. 执行监听器 (Execution Listener)**
            *   在流程执行的关键节点（如开始/结束活动、走过序列流）触发自定义逻辑。可用于数据初始化、日志记录或复杂逻辑处理。
        *   **2.1.8.2. 任务监听器 (Task Listener)**
            *   专门用于用户任务的生命周期事件（如创建、分配、完成）。常用于在任务分配时发送通知或在任务完成后进行数据校验。
        *   **2.1.8.3. 异步执行 (Asynchronous Continuations)**
            *   通过设置 `async before/after` 属性，在流程中创建事务边界和保存点。这对于处理长时间运行的流程和错误恢复至关重要。
        *   **2.1.8.4. 表单关联 (Form Association)**
            *   通过 `Form Key` 或 `Form Reference` 将用户任务与一个表单定义关联起来。这样在 Tasklist 中打开任务时，会自动渲染指定的表单。
        *   **2.1.8.5. 候选项与分配 (Candidate Users/Groups & Assignee)**
            *   用于定义用户任务的办理人。`Assignee` 直接指定给某个用户；`Candidate Users/Groups` 定义一个候选池，池中成员可以认领任务。
        *   **2.1.8.6. 输入/输出参数映射 (Input/Output Parameter Mapping)**
            *   控制活动（特别是服务任务和调用活动）与流程变量之间的数据流。可以精确地映射、转换和创建变量，避免变量污染。
        *   **2.1.8.7. 连接器配置 (Connector Configuration)**
            *   在服务任务中配置 Camunda Connectors，以声明式的方式与外部系统（如 REST API, SOAP Web Service）进行交互，无需编写 Java 代码。
        *   **2.1.8.8. 作业优先级 (Job Priority)**
            *   为异步作业（如定时器、异步任务）设置优先级。这允许作业执行器（Job Executor）优先处理更重要的任务。
        *   **2.1.8.9. 失败重试周期 (Failed Job Retry Time Cycle)**
            *   配置异步作业在失败后的重试策略。可以定义重试次数和间隔时间，例如 `R5/PT5M` 表示每5分钟重试一次，共重试5次。
        *   **2.1.8.10. 任务优先级 (Task Priority)**
            *   为用户任务设置优先级。这个优先级可以在 Tasklist 中显示，帮助用户优先处理更重要的待办事项。

*   **2.2. DMN 1.3 决策建模 (DMN 1.3 Decision Modeling)**
    *   **2.2.1. 决策表 (Decision Table)**
        *   DMN 的核心表现形式，一个包含输入、输出和规则的表格。每一行代表一条规则，当所有输入条件满足时，执行对应的输出。
    *   **2.2.2. 命中策略 (Hit Policy)**
        *   定义当多条规则同时匹配时如何处理输出。常见的策略有 `UNIQUE`（只允许匹配一条）、`FIRST`（返回第一条匹配规则的结果）等。
    *   **2.2.3. FEEL 语言 (Friendly Enough Expression Language)**
        *   DMN 标准中用于编写输入项、输出项和规则条件的表达式语言。语法简洁，接近自然语言，方便业务人员理解和编写。
    *   **2.2.4. 决策需求图 (Decision Requirements Diagram - DRD)**
        *   用于可视化决策之间的依赖关系。一个 DRD 可以包含多个决策、输入数据、业务知识模型，清晰地展示复杂决策的整体结构。
    *   **2.2.5. 输入数据 (Input Data)**
        *   在 DRD 中表示决策所需要的外部输入信息。它定义了决策评估时必须提供的数据项及其类型。
    *   **2.2.6. 业务知识模型 (Business Knowledge Model - BKM)**
        *   封装了一段可复用的决策逻辑（如一个函数或一个决策表），可以在多个决策中被调用，提高了决策逻辑的模块化和复用性。
    *   **2.2.7. 字面量表达式 (Literal Expression)**
        *   除了决策表，DMN 还支持使用 FEEL 脚本编写的字面量表达式来定义决策逻辑，适用于简单的计算或条件判断。

*   **2.3. CMMN 1.1 案例建模 (CMMN 1.1 Case Modeling)**
    *   **2.3.1. 案例计划模型 (Case Plan Model)**
        *   CMMN 的顶层元素，相当于一个“案例文件夹”。它包含了处理一个案例所需的所有阶段、任务、里程碑和事件监听器。
    *   **2.3.2. 阶段与任务 (Stages and Tasks)**
        *   阶段（Stage）是组织案例项的容器，可以嵌套。任务（Task）是案例中需要完成的工作单元，可以是人工任务、流程任务等。
    *   **2.3.3. 入口与出口条件 (Entry and Exit Criteria)**
        *   通过 Sentry（哨兵）来定义的条件。当入口条件满足时，对应的阶段或任务被激活；当出口条件满足时，它们被终止。
    *   **2.3.4. 事件监听器 (Event Listener)**
        *   用于监听案例生命周期内发生的事件，如定时器事件、用户事件等。当事件发生时，可以触发新的任务或阶段。
    *   **2.3.5. 里程碑 (Milestone)**
        *   表示案例处理过程中达到的一个重要成就或状态。当其依赖的案例项完成后，里程碑被达成，可用于跟踪案例进度。

*   **2.4. 建模工具使用 (Modeling Tools Usage)**
    *   **2.4.1. Camunda Modeler (Desktop)**
        *   **2.4.1.1. 基本操作与界面 (Basic Operations & UI)**
            *   介绍桌面版建模器的界面布局，包括画布、属性面板、工具栏等。讲解如何创建、打开、保存 BPMN、DMN 和 CMMN 文件。
        *   **2.4.1.2. 属性面板详解 (Properties Panel in Depth)**
            *   详细解释属性面板中各个标签页（如 General, Listeners, Forms, Input/Output）的功能，以及如何配置 Camunda 特定属性。
        *   **2.4.1.3. 元素模板 (Element Templates)**
            *   使用和创建元素模板来封装特定任务的配置，简化建模过程。模板可以预设属性，并为用户提供友好的配置表单。
        *   **2.4.1.4. Modeler 插件 (Modeler Plugins)**
            *   介绍如何安装和使用社区或自定义插件来扩展 Modeler 的功能，例如 Linter 插件用于校验模型规范，Token Simulation 插件用于模拟流程执行。
        *   **2.4.1.5. 部署功能 (Deployment Feature)**
            *   讲解如何通过 Modeler 内置的部署功能，直接将设计好的模型部署到正在运行的 Camunda 7 引擎进行测试。
    *   **2.4.2. Camunda Web Modeler (Cloud)**
        *   **2.4.2.1. 协作建模 (Collaborative Modeling)**
            *   展示 Web Modeler 的核心优势，即多人实时协作编辑同一个流程模型，并提供评论和版本历史功能，提升团队协作效率。
        *   **2.4.2.2. 项目与文件管理 (Project & File Management)**
            *   介绍如何在 Web Modeler 中组织项目、文件夹和文件，以及如何管理不同版本的模型，支持更规范的模型资产管理。
        *   **2.4.2.3. 与 C8 集成 (Integration with Camunda 8)**
            *   演示如何将模型一键部署到 Camunda 8 (SaaS 或自建) 的 Zeebe 引擎，并与 Operate、Tasklist 等组件无缝集成。
        *   **2.4.2.4. 表单设计 (Form Design with Form Builder)**
            *   介绍 Web Modeler 内置的可视化表单构建器，通过拖拽组件的方式快速创建用户任务表单，并与流程数据进行绑定。
        *   **2.4.2.5. 连接器模板集成 (Connector Template Integration)**
            *   在 Web Modeler 中为服务任务选择和配置 C8 连接器。模板提供了预设的输入参数表单，极大地简化了与外部系统的集成工作。


### **3. 开发与实现 (Development & Implementation)**

本章节聚焦于如何将设计好的流程模型付诸实践。内容涵盖不同平台版本的开发环境搭建、核心API使用、任务逻辑实现、数据处理、错误处理及自动化测试等，是连接模型与可运行应用程序的桥梁。

*   **3.1. Camunda Platform 7 开发 (Development for Camunda Platform 7)**
    *   **3.1.1. 环境搭建与配置 (Setup & Configuration)**
        *   **3.1.1.1. Maven/Gradle 依赖 (Maven/Gradle Dependencies)**
            *   介绍在项目中添加 Camunda 核心依赖项。包括 `camunda-engine`、`camunda-engine-spring` 以及与数据库、Web 应用等集成的相关库。
        *   **3.1.1.2. Spring Boot Starter**
            *   最推荐的 C7 开发方式。通过引入 `camunda-bpm-spring-boot-starter-webapp` 依赖，快速搭建一个集成了流程引擎和 Web 应用的 Spring Boot 程序。
        *   **3.1.1.3. 流程引擎配置 (Process Engine Configuration)**
            *   讲解 `bpm-platform.xml` 或 Spring Boot 的 `application.yaml` 中核心配置项，如数据库连接、作业执行器、历史记录级别、插件等。
        *   **3.1.1.4. 流程应用 (Process Application)**
            *   定义一个“流程应用”的概念，通常是一个 Java 应用（如 WAR 包）。通过 `processes.xml` 文件声明要自动部署的流程模型。
    *   **3.1.2. 核心 Java API 服务 (Core Java API Services)**
        *   **3.1.2.1. RepositoryService**
            *   管理和操作流程定义与部署。主要用于部署流程文件（BPMN, DMN）、查询流程定义、获取模型资源等。
        *   **3.1.2.2. RuntimeService**
            *   用于启动流程实例和控制其执行。核心操作包括 `startProcessInstanceByKey`、发送消息、触发信号、设置/获取流程变量等。
        *   **3.1.2.3. TaskService**
            *   管理和处理用户任务。提供查询待办任务、认领（claim）、完成（complete）、设置任务变量、委托（delegate）等所有与人工任务相关的功能。
        *   **3.1.2.4. HistoryService**
            *   查询所有历史数据。当流程实例执行完成后，可以通过此服务查询已完成的流程实例、活动、任务以及历史变量等信息。
        *   **3.1.2.5. IdentityService**
            *   管理用户和用户组。虽然 Camunda 不强制使用其内置身份管理，但该服务提供了创建、查询、删除用户和组，以及管理他们之间关系的功能。
        *   **3.1.2.6. ManagementService**
            *   引擎的管理和维护服务。主要用于查询和管理作业（Job），例如查询失败作业、设置重试次数、执行作业等底层运维操作。
        *   **3.1.2.7. FormService**
            *   处理与流程表单相关的数据。用于获取启动表单或任务表单的数据，以及提交表单数据以启动或完成任务。
        *   **3.1.2.8. DecisionService**
            *   用于独立于流程来评估 DMN 决策。通过 `evaluateDecisionTableByKey` 方法，可以传入输入变量并直接获取决策结果。
    *   **3.1.3. 服务任务实现模式 (Service Task Implementation Patterns)**
        *   **3.1.3.1. Java Delegate**
            *   最常用的服务任务实现方式。创建一个实现 `JavaDelegate` 接口的类，在 `execute` 方法中编写业务逻辑。通过 `delegateExecution` 对象访问流程变量。
        *   **3.1.3.2. ActivityBehavior**
            *   比 Java Delegate 更底层的接口，提供了对流程执行行为更精细的控制，例如自定义节点的进入和离开逻辑，适用于高级定制场景。
        *   **3.1.3.3. Spring Bean 调用 (Spring Bean Invocation)**
            *   在 Spring 环境下，可以将服务任务直接委托给一个 Spring Bean 的方法执行。通过在模型中配置 `camunda:delegateExpression` 为 `${myBean.myMethod()}`。
        *   **3.1.3.4. 表达式 (Expression)**
            *   在服务任务中直接执行一段 JUEL 表达式。适用于非常简单的逻辑，例如调用一个已有对象的方法或设置一个流程变量。
        *   **3.1.3.5. 外部任务模式 (External Task Pattern)**
            *   将任务执行逻辑与流程引擎解耦。引擎将任务发布，外部的 `External Task Worker` 通过 REST API 长轮询拉取并锁定任务，执行后回报结果。
    *   **3.1.4. 流程变量与数据处理 (Process Variables & Data Handling)**
        *   **3.1.4.1. 变量作用域 (Variable Scopes)**
            *   流程变量可以存在于流程实例级别，或特定活动（如子流程）的局部范围。理解作用域对于避免变量冲突和正确传递数据至关重要。
        *   **3.1.4.2. 变量序列化 (Variable Serialization)**
            *   引擎将 Java 对象序列化后存入数据库。可以配置默认序列化格式，并支持自定义序列化，以处理复杂或遗留的 Java 对象。
        *   **3.1.4.3. 使用 Spin 处理 JSON/XML (Using Spin for JSON/XML)**
            *   Camunda Spin 是一个数据格式处理库，它简化了在流程中对 JSON 和 XML 数据的创建、解析和转换操作，无需手动进行对象映射。
        *   **3.1.4.4. 类型化变量 API (Typed Variable API)**
            *   提供了一种类型安全的方式来处理变量。可以直接将变量存取为文件、对象等特定类型，并设置序列化选项，避免了手动的类型转换。
    *   **3.1.5. 用户任务与表单 (User Tasks & Forms)**
        *   **3.1.5.1. 任务查询 (Task Queries)**
            *   使用 `TaskService` 创建丰富的查询，以根据分配人、候选组、流程变量、创建时间等多种条件来查找用户任务。
        *   **3.1.5.2. 任务生命周期 (Task Lifecycle)**
            *   涵盖任务从创建、分配（Assignment）、认领（Claim）、完成（Complete）到委托（Delegate）、转办（Set Assignee）的完整状态流转。
        *   **3.1.5.3. 嵌入式表单 (Embedded Forms)**
            *   将 HTML 表单文件与流程应用一起部署。通过 Form Key 关联，Tasklist 可以直接渲染这些表单，表单通过 Camunda JS API 与引擎交互。
        *   **3.1.5.4. 外部表单 (External Forms)**
            *   将任务表单的实现完全交给外部应用（如 Angular/React 应用）。在模型中配置一个 URL，Tasklist 会将用户重定向到该地址处理任务。
        *   **3.1.5.5. 生成式表单 (Generated Forms)**
            *   在 BPMN 模型中直接定义表单字段。Tasklist 会根据这些定义动态生成一个简单的表单，适合快速原型或内部管理等简单场景。
    *   **3.1.6. 错误与异常处理 (Error & Exception Handling)**
        *   **3.1.6.1. 抛出 BPMN 错误 (Throwing BPMN Errors)**
            *   在 Java Delegate 或脚本中，通过抛出 `new BpmnError("errorCode")` 来触发流程模型中的错误事件，将技术异常转化为业务异常处理。
        *   **3.1.6.2. 事故/事件 (Incidents)**
            *   当作业执行失败且重试次数耗尽时，引擎会创建一个 Incident。这表示一个需要人工干预的技术问题，会在 Cockpit 中高亮显示。
        *   **3.1.6.3. 失败作业重试 (Failed Job Retries)**
            *   可以全局或在具体活动上配置异步作业的重试策略。例如 `R3/PT5M` 表示失败后每隔5分钟重试一次，总共重试3次。
    *   **3.1.7. 流程测试 (Process Testing)**
        *   **3.1.7.1. 单元测试 (Unit Testing)**
            *   使用 JUnit 和 `ProcessEngineRule` 或 `ProcessEngineExtension` (JUnit5)，可以在内存数据库中运行引擎，对流程定义的执行路径进行单元测试。
        *   **3.1.7.2. Camunda BPM Assert 库**
            *   一个流式断言库，极大地简化了流程测试代码。可以方便地断言流程实例的状态、当前所在的活动、变量值等。
        *   **3.1.7.3. 模拟 (Mocking)**
            *   在测试中，通常需要模拟外部服务调用。结合 Mockito 等框架，可以替换 Java Delegate 或表达式中的 Bean，专注于测试流程逻辑本身。
        *   **3.1.7.4. 场景测试 (Scenario Testing)**
            *   结合 Camunda BPM Assert Scenario 扩展，可以模拟一个完整的流程场景，包括用户任务的执行，以端到端的方式验证流程的正确性。
    *   **3.1.8. 监听器与扩展点 (Listeners & Extension Points)**
        *   **3.1.8.1. 执行监听器实现 (Implementing Execution Listeners)**
            *   实现 `ExecutionListener` 接口，并将其配置在流程的开始/结束事件或活动的 `start`/`end` 事件上，用于执行通用的横切关注点逻辑。
        *   **3.1.8.2. 任务监听器实现 (Implementing Task Listeners)**
            *   实现 `TaskListener` 接口，并将其配置在用户任务的 `create`, `assignment`, `complete` 等事件上，常用于发送通知或进行权限校验。
        *   **3.1.8.3. 解析监听器 (Parse Listener)**
            *   一个强大的引擎扩展点，允许在 BPMN 模型被解析成可执行对象之前，动态地修改其结构或添加监听器，实现全局性的行为注入。

*   **3.2. Camunda Platform 8 开发 (Development for Camunda Platform 8)**
    *   **3.2.1. 环境与客户端设置 (Setup & Client Configuration)**
        *   **3.2.1.1. 获取客户端凭证 (Obtaining Client Credentials)**
            *   在 Camunda SaaS 或自建环境中创建 Zeebe 客户端，并获取连接所需的参数，包括集群地址、client ID 和 client secret。
        *   **3.2.1.2. Zeebe Java Client 依赖**
            *   在 Maven 或 Gradle 项目中引入 `camunda-zeebe-client-java` 依赖。这是与 Zeebe 引擎进行 gRPC 通信的核心 Java 库。
        *   **3.2.1.3. 客户端构建与配置 (Building the ZeebeClient)**
            *   使用 Builder 模式创建 `ZeebeClient` 实例，配置网关地址、认证凭证以及其他选项，如请求超时、Keep-alive 等。
        *   **3.2.1.4. Spring Zeebe Starter**
            *   推荐在 Spring Boot 中使用 `spring-zeebe-starter`。它能自动配置 `ZeebeClient`，并提供注解驱动的方式来开发 Job Worker。
    *   **3.2.2. Zeebe Client API (gRPC)**
        *   **3.2.2.1. 部署流程 (Deploying Processes)**
            *   通过 `client.newDeployResourceCommand().addResourceFile("process.bpmn").send().join()` 将 BPMN 或 DMN 模型部署到 Zeebe 集群。
        *   **3.2.2.2. 创建流程实例 (Creating Process Instances)**
            *   使用 `client.newCreateInstanceCommand().bpmnProcessId("...").latestVersion().variables({...}).send().join()` 来启动一个新的流程实例。
        *   **3.2.2.3. 发布消息 (Publishing Messages)**
            *   通过 `client.newPublishMessageCommand().messageName("...").correlationKey("...").send().join()` 来发送消息，可以用于启动流程或触发中间捕获事件。
        *   **3.2.2.4. 设置/更新变量 (Setting/Updating Variables)**
            *   使用 `client.newSetVariablesCommand().elementInstanceKey(...).variables({...}).send().join()` 可以为正在运行的流程实例更新变量。
        *   **3.2.2.5. 广播信号 (Broadcasting Signals)**
            *   C8.1+ 支持。使用 `newBroadcastSignalCommand()` 向所有监听该信号的流程实例广播信号，触发相应的信号事件。
    *   **3.2.3. Job Worker 开发 (Job Worker Development)**
        *   **3.2.3.1. Job Worker 概念**
            *   Job Worker 是一个外部应用，它通过客户端连接到 Zeebe，订阅特定类型的作业，执行业务逻辑，然后向 Zeebe 报告作业的完成或失败。
        *   **3.2.3.2. 创建 Job Worker (Creating a Job Worker)**
            *   通过 `client.newWorker().jobType("...").handler(...)` 来创建一个工作者。可以配置工作者名称、超时时间、要拉取的最大作业数等。
        *   **3.2.3.3. 作业处理器 (Job Handler)**
            *   处理器是实现业务逻辑的地方，它接收 `JobClient` 和 `ActivatedJob` 作为参数。`ActivatedJob` 包含作业信息和变量。
        *   **3.2.3.4. 完成/失败作业 (Completing/Failing Jobs)**
            *   在处理器中，业务逻辑执行成功后调用 `jobClient.newCompleteCommand(job.getKey()).send()`；失败时调用 `newFailCommand()`，可设置重试次数和错误消息。
        *   **3.2.3.5. 使用 @ZeebeWorker 注解 (Using @ZeebeWorker Annotation)**
            *   在 Spring Boot 环境中，只需在一个方法上添加 `@ZeebeWorker(type = "...")` 注解，该方法就会自动成为一个对应类型的作业处理器。
        *   **3.2.3.6. 变量获取与映射 (Fetching and Mapping Variables)**
            *   通过 `activatedJob.getVariablesAsMap()` 或 `getVariablesAsType(MyClass.class)` 获取流程变量。Spring Zeebe 还支持将变量直接映射到方法参数。
    *   **3.2.4. 用户任务与表单 (User Tasks & Forms)**
        *   **3.2.4.1. Camunda Forms**
            *   在 Web Modeler 中使用拖拽式表单构建器创建表单，并将其与用户任务关联。表单定义会随 BPMN 模型一起部署。
        *   **3.2.4.2. Tasklist API**
            *   Camunda 8 提供了一套独立的 GraphQL API 用于与 Tasklist 交互，可以用来查询、分配和完成用户任务，适用于构建自定义的任务列表应用。
        *   **3.2.4.3. 用户任务的本质 (Nature of User Tasks)**
            *   在 Zeebe 内部，一个用户任务也是一个 Job，但有特殊的 `zeebe:userTask` 类型。Tasklist 组件本质上是这类 Job 的一个专用 Worker。
    *   **3.2.5. Connectors 使用与开发 (Using & Developing Connectors)**
        *   **3.2.5.1. 使用开箱即用 Connectors (Using Out-of-the-Box Connectors)**
            *   在服务任务的属性面板中选择一个连接器（如 REST, Kafka），并填写输入参数。引擎会自动调用相应的连接器运行时执行集成逻辑。
        *   **3.2.5.2. 自定义 Connectors - 运行时 (Custom Connectors - Runtime)**
            *   开发自定义连接器需要实现其运行时逻辑，通常是一个独立的 Spring Boot 或 Java 应用，它扮演 Job Worker 的角色，处理连接器类型的作业。
        *   **3.2.5.3. 自定义 Connectors - 元素模板 (Custom Connectors - Element Template)**
            *   创建一个 JSON 格式的元素模板，定义连接器的输入参数和在 Modeler 中的显示样式，使用户可以在图形界面上方便地配置你的连接器。
    *   **3.2.6. 错误与事件处理 (Error & Event Handling)**
        *   **3.2.6.1. 抛出 BPMN 错误 (Throwing BPMN Errors)**
            *   在 Job Worker 中，通过 `jobClient.newThrowErrorCommand(job.getKey()).errorCode("...").send()` 来触发流程中的错误边界事件或事件子流程。
        *   **3.2.6.2. 作业超时与重试 (Job Timeouts and Retries)**
            *   Worker 在激活作业时会锁定一段时间（`timeout`）。若超时未完成，作业会重新可见。失败时可通过 `fail` 命令控制剩余重试次数。
        *   **3.2.6.3. C8 中的事件 (Incidents in C8)**
            *   当作业失败且重试次数为0，或流程中出现无法继续的错误时（如表达式计算失败），Zeebe 会创建事件，这些事件会在 Operate 中可见。
    *   **3.2.7. 流程测试 (Process Testing)**
        *   **3.2.7.1. zeebe-process-test 库**
            *   一个用于 Zeebe 流程单元测试和集成测试的 Java 库。它可以在测试中启动一个轻量级的内存 Zeebe 引擎。
        *   **3.2.7.2. 编写测试用例 (Writing Test Cases)**
            *   使用 `@ZeebeProcessTest` 注解 (JUnit5)，自动注入 `ZeebeTestEngine` 和 `ZeebeClient`。然后使用 client 部署流程、创建实例，并断言结果。
        *   **3.2.7.3. 断言流程状态 (Asserting Process State)**
            *   使用 `RecordStream` 或社区的 `camunda-platform-8-process-test-assertions` 库，可以方便地断言流程实例已完成、处于特定节点或具有特定变量。
        *   **3.2.7.4. 控制测试引擎时钟 (Controlling Test Engine Clock)**
            *   测试引擎提供了控制虚拟时钟的能力，可以轻松地快进时间来触发定时器事件，而无需在测试中实际等待。

---

### **4. 部署与运维 (Deployment & Operations)**

本章节介绍如何部署 Camunda 平台到不同环境，以及在生产环境中进行监控、维护和扩展。内容覆盖了 Camunda 7 和 Camunda 8 两种平台截然不同的部署和运维模式。

*   **4.1. Camunda Platform 7 部署与运维 (C7 Deployment & Operations)**
    *   **4.1.1. 部署模式 (Deployment Scenarios)**
        *   **4.1.1.1. 嵌入式引擎 (Embedded Engine)**
            *   将流程引擎作为库嵌入到您的 Java 应用中（例如 Spring Boot）。应用和引擎在同一个 JVM 中运行，部署简单，适合微服务架构。
        *   **4.1.1.2. 共享引擎 (Shared Engine)**
            *   在应用服务器（如 Tomcat, WildFly）上部署一个共享的流程引擎实例，多个流程应用（WARs）可以共用此引擎，实现资源隔离与共享。
        *   **4.1.1.3. 独立引擎 (Standalone Engine)**
            *   将引擎作为一个独立的服务运行（如使用 Camunda Run），应用程序通过 REST API 与其远程交互，实现了语言和架构的解耦。
    *   **4.1.2. 数据库管理 (Database Management)**
        *   **4.1.2.1. 支持的数据库 (Supported Databases)**
            *   Camunda 7 支持多种关系型数据库，如 PostgreSQL, MySQL, Oracle, SQL Server 等。选择时需参考官方文档的版本兼容性矩阵。
        *   **4.1.2.2. 数据库表结构 (Database Schema)**
            *   引擎在数据库中创建一系列表（通常以 ACT_ 开头）来持久化流程定义、实例、历史等数据。核心表分为运行时、历史、身份和管理几类。
        *   **4.1.2.3. 数据库 Schema 创建与更新 (Schema Creation & Update)**
            *   通过配置 `databaseSchemaUpdate` 参数，可以控制引擎启动时是否自动创建、更新或校验数据库表结构，简化了首次部署和版本升级。
    *   **4.1.3. 集群化部署 (Clustering)**
        *   **4.1.3.1. 高可用性设置 (High Availability Setup)**
            *   通过部署多个指向同一个数据库的 Camunda 节点来实现高可用。当一个节点宕机时，其他节点可以接管其工作，保证服务连续性。
        *   **4.1.3.2. 作业执行器集群感知 (Job Executor in a Cluster)**
            *   集群中的作业执行器通过数据库的乐观锁机制来获取和执行异步作业，确保同一个作业在同一时间只会被一个节点执行。
        *   **4.1.3.3. 负载均衡 (Load Balancing)**
            *   在集群前端通常需要一个负载均衡器，将来自客户端的请求（如 REST API调用、Web应用访问）分发到不同的 Camunda 节点。
    *   **4.1.4. Camunda Run 发行版**
        *   **4.1.4.1. 简介 (Introduction to Camunda Run)**
            *   一个预打包、可配置的 Camunda 7 发行版，无需传统应用服务器即可独立运行。它内置了 Web 应用和 REST API，通过 YAML 文件进行配置。
        *   **4.1.4.2. 配置与启动 (Configuration & Startup)**
            *   通过修改 `default.yml` 文件来配置数据库连接、LDAP、Web 应用安全等。通过执行 `start.sh` 或 `start.bat` 脚本来启动服务。
    *   **4.1.5. 核心运维任务 (Core Operational Tasks)**
        *   **4.1.5.1. 历史数据清理 (History Cleanup)**
            *   随着流程执行，历史数据会不断增长，影响性能。必须配置并启用历史数据清理作业，定期删除已完成且超过保留时间的实例数据。
        *   **4.1.5.2. 流程实例迁移 (Process Instance Migration)**
            *   当更新流程定义后，可以使用迁移 API 将正在运行的流程实例从旧版本迁移到新版本，无需中断业务即可应用流程变更。
        *   **4.1.5.3. 引擎版本升级 (Engine Version Upgrades)**
            *   升级 Camunda 版本通常涉及替换项目依赖库、执行数据库更新脚本，并测试现有流程的兼容性。官方提供详细的升级指南。
        *   **4.1.5.4. 备份与恢复 (Backup & Restore)**
            *   Camunda 7 的状态完全存储在数据库中。因此，备份和恢复策略就是对底层关系型数据库进行标准的备份和恢复操作。

*   **4.2. Camunda Platform 8 部署与运维 (C8 Deployment & Operations)**
    *   **4.2.1. 部署模式 (Deployment Models)**
        *   **4.2.1.1. Camunda Cloud (SaaS)**
            *   由 Camunda官方提供和维护的全托管云服务。用户只需通过 Web 界面创建和管理集群，即可获得高可用的流程自动化平台。
        *   **4.2.1.2. 自建部署 (Self-Managed)**
            *   在自己的基础设施（如私有云、本地数据中心）上部署 Camunda 8。官方推荐并主要支持在 Kubernetes 上使用 Helm Chart 进行部署。
    *   **4.2.2. 自建部署 - Kubernetes & Helm**
        *   **4.2.2.1. Helm Chart 简介 (Introduction to Helm Charts)**
            *   Helm 是 Kubernetes 的包管理器。Camunda 官方提供了 Helm Chart，它封装了部署所有 C8 组件所需的 Kubernetes 资源和配置模板。
        *   **4.2.2.2. 环境要求 (Prerequisites)**
            *   需要一个 Kubernetes 集群、Helm 客户端、动态存储卷（Persistent Volume）支持，以及用于对外暴露服务的 Ingress Controller。
        *   **4.2.2.3. 核心组件配置 (Core Component Configuration)**
            *   通过修改 Helm Chart 的 `values.yaml` 文件，可以配置 Zeebe, Operate, Tasklist, Identity 等所有组件的副本数、资源、镜像版本等。
        *   **4.2.2.4. Zeebe 集群配置 (Zeebe Cluster Configuration)**
            *   关键配置项包括 Broker 数量、分区数（Partitions）和复制因子（Replication Factor），这些共同决定了集群的性能和容错能力。
        *   **4.2.2.5. 身份认证配置 (Identity Configuration)**
            *   可以配置 Identity 组件与外部身份提供商（IDP）如 Keycloak, Auth0, Okta 进行集成，实现单点登录（SSO）。
    *   **4.2.3. 核心运维任务 (Core Operational Tasks)**
        *   **4.2.3.1. 伸缩 (Scaling)**
            *   可以水平扩展 Zeebe Broker、Gateway 和其他无状态组件（如 Operate）的副本数，以应对不断增长的负载和用户访问量。
        *   **4.2.3.2. 备份与恢复 (Backup & Restore)**
            *   Zeebe 支持将状态快照备份到外部存储（如 S3, Google Cloud Storage）。在发生灾难时，可以从备份中恢复整个集群的状态。
        *   **4.2.3.3. 升级 (Upgrading)**
            *   使用 `helm upgrade` 命令可以安全地进行滚动升级。通常需要遵循官方文档中的版本间升级路径和注意事项，以确保平滑过渡。
        *   **4.2.3.4. 数据导出与归档 (Data Export & Archiving)**
            *   Zeebe 的流程数据通过 Exporter 实时导出到 Elasticsearch。运维工作包括管理 Elasticsearch 的索引生命周期（ILM）以归档或删除旧数据。
    *   **4.2.4. 监控与告警 (Monitoring & Alerting)**
        *   **4.2.4.1. Prometheus 指标 (Prometheus Metrics)**
            *   所有 Camunda 8 组件都原生暴露了 Prometheus 格式的监控指标，涵盖了延迟、吞吐量、错误率、资源使用等关键性能数据。
        *   **4.2.4.2. Grafana 仪表盘 (Grafana Dashboards)**
            *   Camunda 官方提供了预构建的 Grafana 仪表盘模板，可以直观地展示集群的健康状况、流程执行统计和潜在的性能瓶颈。
        *   **4.2.4.3. 健康检查端点 (Health Check Endpoints)**
            *   每个组件都提供了健康检查端点（liveness/readiness），Kubernetes 可以利用这些端点来自动管理 Pod 的生命周期，实现自愈。

### **5. 用户界面与集成 (User Interfaces & Integration)**

本章节介绍 Camunda 提供的开箱即用的 Web 应用，以及如何对它们进行定制、扩展和集成到您自己的应用生态中。

*   **5.1. Camunda Platform 7 Web 应用 (C7 Web Applications)**
    *   **5.1.1. Cockpit**
        *   **5.1.1.1. 流程监控与分析 (Process Monitoring & Analysis)**
            *   为技术人员和运维团队提供强大的监控视图。可以查看正在运行和已完成的流程实例、热力图分析、变量状态和历史记录。
        *   **5.1.1.2. 运维操作 (Operational Interventions)**
            *   允许在运行时对流程实例进行干预，如手动重试失败的作业、解决事件、修改变量、取消实例或在流程中跳转节点。
        *   **5.1.1.3. Cockpit 插件 (Cockpit Plugins)**
            *   通过开发自定义插件（基于 AngularJS），可以扩展 Cockpit 的功能，例如添加新的仪表盘、在流程图上显示特定信息或集成外部数据。
    *   **5.1.2. Tasklist**
        *   **5.1.2.1. 任务处理中心 (Task Handling Center)**
            *   面向业务用户的待办事项列表。用户可以在这里查看、过滤、认领并完成分配给他们的人工任务，是人机交互的核心界面。
        *   **5.1.2.2. 过滤器与自定义视图 (Filters & Custom Views)**
            *   用户可以创建和保存自己的任务过滤器，以便快速访问符合特定条件（如流程类型、优先级、变量值）的任务列表。
        *   **5.1.2.3. Tasklist 插件 (Tasklist Plugins)**
            *   与 Cockpit 类似，也可以为 Tasklist 开发插件来定制 UI，例如集成自定义的表单渲染引擎或在任务视图中添加额外的操作按钮。
    *   **5.1.3. Admin**
        *   **5.1.3.1. 用户与组管理 (User & Group Management)**
            *   提供了一个基础的界面来管理存储在 Camunda 内部数据库中的用户、用户组以及它们之间的关系。
        *   **5.1.3.2. 授权管理 (Authorization Management)**
            *   核心功能。管理员可以在此为用户和组分配对各种资源的访问权限，如特定流程定义的启动权、某个任务的查看权等。
    *   **5.1.4. Web 应用定制与集成 (Webapp Customization & Integration)**
        *   **5.1.4.1. UI 品牌化 (UI Branding)**
            *   可以通过修改 CSS 文件或使用 `user-styles.css` 文件来轻松地更改 Web 应用的Logo、颜色主题和字体，以匹配企业形象。
        *   **5.1.4.2. 使用 REST API 集成 (Integration via REST API)**
            *   所有 Web 应用的功能都建立在强大的 REST API 之上。您可以完全不使用官方前端，而是通过调用 API 将流程功能集成到自己的应用中。
        *   **5.1.4.3. 单点登录 (Single Sign-On - SSO)**
            *   可以通过配置 Spring Security 或使用容器管理的安全性，将 Camunda Web 应用与 LDAP、OAuth2/OIDC 等企业级认证系统集成。

*   **5.2. Camunda Platform 8 Web 应用 (C8 Web Applications)**
    *   **5.2.1. Operate**
        *   **5.2.1.1. 实时流程监控 (Real-time Process Monitoring)**
            *   C8 的核心监控工具，提供了对 Zeebe 中流程实例的实时可见性。可以搜索实例、查看其路径、变量和事件历史。
        *   **5.2.1.2. 故障排查与解决 (Troubleshooting & Resolution)**
            *   当流程中发生事件（Incident）时，Operate 会高亮显示问题所在。运维人员可以在此分析错误原因并执行解决操作，如更新变量并重试。
    *   **5.2.2. Tasklist**
        *   **5.2.2.1. 现代化任务界面 (Modern Task Interface)**
            *   C8 的 Tasklist 提供了现代化的、响应式的用户界面，用于处理人工任务。它与 Camunda Forms 无缝集成，提供丰富的表单体验。
        *   **5.2.2.2. 任务声明与完成 (Task Claiming & Completion)**
            *   用户可以查看分配给他们或其所属组的任务，声明任务所有权，并填写表单以完成任务，推动流程继续进行。
    *   **5.2.3. Web Modeler (作为用户界面)**
        *   **5.2.3.1. 协作与部署中心 (Collaboration & Deployment Hub)**
            *   除了建模，Web Modeler 也是一个管理流程定义的中心。团队可以在此协作、评论、查看版本历史，并一键将模型部署到开发或生产环境。
    *   **5.2.4. Optimize (通常与 C8 结合使用)**
        *   **5.2.4.1. 业务流程分析 (Business Process Analysis)**
            *   Optimize 从引擎（C7或C8）获取数据，提供深入的业务分析能力。可以创建仪表盘、报告，分析流程瓶颈、持续时间、成本等。
            *   *注：Optimize 是一个独立但与平台紧密集成的产品。*

---

### **6. 高级主题与最佳实践 (Advanced Topics & Best Practices)**

本章节深入探讨 Camunda 使用过程中的一些高级概念、设计模式和推荐的最佳实践。掌握这些内容可以帮助您构建更健壮、可扩展和易于维护的流程自动化解决方案。

*   **6.1. 流程设计模式 (Process Design Patterns)**
    *   **6.1.1. 流程编排 vs. 流程协同 (Orchestration vs. Choreography)**
        *   编排（Orchestration）由一个中央控制器（如Camunda流程）明确调用各个服务。协同（Choreography）则通过事件驱动，服务间相互订阅事件，无中央协调者。理解两者的区别有助于选择合适的架构模式。
    *   **6.1.2. 事务处理模式 (Transaction Handling Patterns)**
        *   **6.1.2.1. 保存点与异步接续 (Save Points & Asynchronous Continuations)**
            *   通过在模型中策略性地设置 `async before/after`，可以创建事务边界。这确保了流程状态在关键步骤后被持久化，对于长流程和错误恢复至关重要。
        *   **6.1.2.2. Saga 模式 (Saga Pattern)**
            *   在分布式系统中实现长事务的模式。使用 Camunda BPMN 的补偿事件，可以优雅地为一系列服务调用定义补偿操作（回滚），确保最终一致性。
    *   **6.1.3. 流程版本管理 (Process Versioning)**
        *   **6.1.3.1. 迁移策略 (Migration Strategies)**
            *   当需要更新一个有正在运行实例的流程时，必须制定迁移策略。可以选择让旧实例自然结束，或使用 Camunda 的迁移 API 将它们迁移到新版本。
        *   **6.1.3.2. 版本标签与绑定 (Version Tag & Binding)**
            *   部署时可以为流程定义指定版本标签（Version Tag）。在调用活动中，可以绑定到特定标签的流程，确保调用的是稳定版本，而非最新的开发版。
    *   **6.1.4. 可重用性与模块化 (Reusability & Modularity)**
        *   **6.1.4.1. 使用调用活动 (Using Call Activities)**
            *   将通用的、可复用的业务逻辑抽取成独立的子流程，并通过调用活动（Call Activity）在主流程中引用。这大大提高了流程定义的可维护性和重用性。
        *   **6.1.4.2. 共享业务逻辑 (Sharing Business Logic)**
            *   通过 Spring Bean 或共享的 Java 类库来实现可在多个 Java Delegate 或 Job Worker 中复用的业务逻辑，避免代码重复。
    *   **6.1.5. 无状态与有状态的交互 (Stateless vs. Stateful Interaction)**
        *   理解外部任务（External Task）和 Job Worker 模式如何帮助实现与无状态服务的交互，而流程引擎本身则负责维护整个交互过程中的“状态”。

*   **6.2. 性能与扩展性 (Performance & Scalability)**
    *   **6.2.1. Camunda 7 性能调优**
        *   **6.2.1.1. 作业执行器调优 (Job Executor Tuning)**
            *   调整线程池大小、队列容量、每次获取的作业数等参数，以匹配您的硬件资源和负载特性，是 C7 性能优化的关键。
        *   **6.2.1.2. 数据库索引与优化 (Database Indexing & Optimization)**
            *   确保数据库有合适的索引，并定期进行性能分析。对于高负载场景，监控和优化数据库的查询性能至关重要。
        *   **6.2.1.3. 历史记录级别 (History Level)**
            *   根据业务需求选择合适的历史记录级别。`FULL` 级别记录所有信息，但对性能影响最大；`AUDIT` 或 `ACTIVITY` 可能是更好的折衷。
        *   **6.2.1.4. 异构部署 (Heterogeneous Deployment)**
            *   在集群中，可以配置某些节点专门处理用户请求（无作业执行器），而另一些节点专门作为作业执行器，实现职责分离和资源优化。
    *   **6.2.2. Camunda 8 性能考量**
        *   **6.2.2.1. 分区与复制因子 (Partitions & Replication Factor)**
            *   理解 Zeebe 的分区机制是性能和容错的关键。增加分区数可以提高并行处理能力（吞吐量），而复制因子则决定了数据的冗余度和可用性。
        *   **6.2.2.2. 网关扩展 (Gateway Scaling)**
            *   Zeebe Gateway 是无状态的，可以独立于 Broker 进行水平扩展，以应对大量的客户端连接和请求。
        *   **6.2.2.3. 背压机制 (Backpressure)**
            *   Zeebe 内置了背压机制。当系统过载时，它会主动拒绝新的请求，以保护自身稳定性。客户端需要实现适当的重试逻辑来应对这种情况。
        *   **6.2.2.4. Job Worker 最佳实践 (Job Worker Best Practices)**
            *   合理设置 Job Worker 的 `maxJobsToActivate` 和并发处理能力，避免一次性拉取过多作业导致内存压力或处理延迟。

*   **6.3. 安全性 (Security)**
    *   **6.3.1. C7 授权管理 (Authorization in C7)**
        *   **6.3.1.1. 授权概念 (Authorization Concepts)**
            *   C7 提供了非常细粒度的授权系统，可以控制用户/组对资源（如流程定义、任务、实例）的特定操作权限（如读、写、创建）。
        *   **6.3.1.2. 开启授权 (Enabling Authorization)**
            *   需要在流程引擎配置中显式开启授权检查。一旦开启，所有 API 调用都会经过权限校验。
    *   **6.3.2. C8 身份与认证 (Identity & Authentication in C8)**
        *   **6.3.2.1. Identity 组件集成**
            *   Identity 是 C8 的中央认证授权服务，基于 Keycloak。所有组件（Operate, Tasklist等）和客户端都通过 OAuth 2.0 与 Identity 进行认证。
    *   **6.3.3. REST API 安全 (Securing the REST API)**
        *   可以通过 Spring Security、HTTP Basic Auth 或其他 Web 安全机制来保护 C7 的 REST API 端点，确保只有授权的客户端可以访问。
    *   **6.3.4. 表达式语言安全 (Expression Language Security)**
        *   在允许用户输入影响表达式执行的场景下，需要注意防范表达式注入攻击。Camunda 提供了相关配置来限制或禁用某些危险的表达式特性。

*   **6.4. 最佳实践总结 (Best Practices Summary)**
    *   **6.4.1. 建模最佳实践 (Modeling Best Practices)**
        *   保持模型简洁易懂，使用业务术语命名元素，避免在图中嵌入过多技术细节，利用子流程和调用活动进行分解。
    *   **6.4.2. 开发最佳实践 (Development Best Practices)**
        *   优先使用外部任务/Job Worker 模式实现服务任务，保持流程定义和业务逻辑代码的解耦。编写全面的单元和集成测试。
    *   **6.4.3. 运维最佳实践 (Operations Best Practices)**
        *   建立完善的监控和告警体系，制定清晰的历史数据清理和流程版本迁移策略，定期进行性能评估和容量规划。

### **7. 生态系统与社区资源 (Ecosystem & Community Resources)**

本章节介绍 Camunda 庞大的生态系统，包括官方和社区提供的各种工具、扩展和资源，它们可以极大地提升您的开发效率和解决问题的能力。

*   **7.1. 官方文档与资源 (Official Documentation & Resources)**
    *   **7.1.1. Camunda 官方文档 (Camunda Docs)**
        *   最权威、最全面的信息来源。详细介绍了平台的所有功能、API、配置和概念，是所有 Camunda 用户的首要参考。
    *   **7.1.2. Camunda 官方博客 (Camunda Blog)**
        *   发布关于新版本特性、最佳实践、用例研究和技术深度文章的地方，有助于了解平台的最新动态和高级用法。
    *   **7.1.3. Camunda Academy**
        *   提供免费和付费的在线培训课程，内容从入门到高级，是系统学习 Camunda 的绝佳途径。
    *   **7.1.4. Camunda 示例库 (Camunda Examples Repository)**
        *   GitHub 上的官方示例库，包含了大量针对特定功能或集成场景的代码示例，是学习和解决问题的宝贵资源。

*   **7.2. 社区支持与交流 (Community Support & Interaction)**
    *   **7.2.1. Camunda 论坛 (Camunda Forum)**
        *   官方维护的社区论坛，是提问、寻求帮助和与其他 Camunda 用户交流经验的主要场所。Camunda 的工程师也会在此解答问题。
    *   **7.2.2. Camunda 社区 Slack/Discord**
        *   提供一个实时交流的平台，可以在这里与社区成员进行更即时的讨论和互动。

*   **7.3. 社区扩展与工具 (Community Extensions & Tools)**
    *   **7.3.1. Awesome Camunda 列表**
        *   一个由社区维护的 GitHub 项目，收集了大量有用的 Camunda 相关库、工具、插件和文章，是发现社区宝藏的好地方。
    *   **7.3.2. Camunda Modeler 插件 (Community Modeler Plugins)**
        *   社区贡献了许多增强 Modeler 功能的插件，如 Token Simulation（模拟执行）、Linter（模型校验）、属性信息提示等。
    *   **7.3.3. 社区客户端库 (Community Client Libraries)**
        *   除了官方的 Java 客户端，社区还为多种语言（如 Python, C#, Node.js）开发了客户端库，方便非 Java 应用与 Camunda 交互。
    *   **7.3.4. 流程测试扩展 (Process Testing Extensions)**
        *   例如 `camunda-bpm-assert-scenario` 和 `camunda-platform-8-process-test-assertions` 等社区库，进一步简化和增强了流程的自动化测试。
    *   **7.3.5. 集成框架 (Integration Frameworks)**
        *   社区提供了与流行框架（如 Axon, Spring Cloud Stream）的集成项目，帮助您在更广泛的架构中无缝使用 Camunda。

*   **7.4. 贡献与参与 (Contribution & Participation)**
    *   **7.4.1. 如何贡献 (How to Contribute)**
        *   Camunda 是一个开源项目，欢迎社区贡献。可以通过报告 Bug、提交 Pull Request、完善文档或在论坛上帮助他人来参与其中。
    *   **7.4.2. Camunda Champion Program**
        *   一个表彰和支持在 Camunda 社区中做出杰出贡献的个人的项目，为社区专家提供了一个展示和交流的平台。


### **8. 迁移指南 (Migration Guides)**

本章节为计划升级或迁移的用户提供指导，特别是从成熟的 Camunda 7 平台迁移到现代化的 Camunda 8 平台。迁移是一个复杂的过程，需要仔细规划。

*   **8.1. 从 Camunda 7 迁移到 Camunda 8 (Migrating from C7 to C8)**
    *   **8.1.1. 为何要迁移？(Why Migrate?)**
        *   阐述迁移的核心驱动力：为云原生、微服务架构而生，提供无限水平扩展、更高的性能和故障恢复能力，以及 SaaS 托管选项。
    *   **8.1.2. 核心概念差异 (Key Conceptual Differences)**
        *   **8.1.2.1. 架构差异 (Architecture)**
            *   C7 是基于关系型数据库的单体或集群架构；C8 是基于事件流的分布式系统，核心组件（Zeebe, Operate等）解耦并独立运行。
        *   **8.1.2.2. 执行语义差异 (Execution Semantics)**
            *   C7 的 Java Delegate 在引擎线程内同步执行；C8 的 Job Worker 是异步的、外部的，通过拉取模式与引擎交互，实现了完全解耦。
        *   **8.1.2.3. 数据处理差异 (Data Handling)**
            *   C7 将状态存储在数据库中；C8 将状态作为事件日志流进行处理和持久化，并通过 Exporter 将数据推送到外部系统（如Elasticsearch）。
        *   **8.1.2.4. 监听器与扩展点 (Listeners & Extension Points)**
            *   C7 的执行/任务监听器在 C8 中已不存在。类似功能需通过 Zeebe Exporter 或在 Job Worker 外部实现。
    *   **8.1.3. 迁移路径与策略 (Migration Path & Strategy)**
        *   **8.1.3.1. 评估与规划 (Assessment & Planning)**
            *   评估现有 C7 流程的复杂性，识别使用了哪些 C8 不支持或实现方式不同的特性（如 CMMN、脚本任务、监听器）。
        *   **8.1.3.2. 分阶段迁移 (Phased Migration)**
            *   建议采用分阶段策略，例如先迁移新的流程到 C8，或选择一个简单的现有流程作为试点，而不是一次性“大爆炸”式迁移。
        *   **8.1.3.3. 并行运行模式 (Parallel Run Pattern)**
            *   在过渡期间，可以同时运行 C7 和 C8 平台。可以使用一个反向代理或事件总线在新旧系统之间路由流程启动请求或事件。
    *   **8.1.4. 技术迁移步骤 (Technical Migration Steps)**
        *   **8.1.4.1. BPMN 模型迁移**
            *   大部分 BPMN 2.0 元素是兼容的。主要工作是替换服务任务的实现方式：将 `camunda:class` 或 `delegateExpression` 修改为 `zeebe:taskDefinition` 的 `type`。
        *   **8.1.4.2. 业务逻辑代码迁移 (Migrating Business Logic)**
            *   将原有的 Java Delegate 或 Spring Bean 逻辑，重构为独立的、可部署的 Job Worker。
        *   **8.1.4.3. API 调用迁移 (Migrating API Calls)**
            *   将原先调用 C7 Java API 或 REST API 的客户端代码，修改为使用 C8 的 Zeebe gRPC 客户端。API 的方法和语义有很大不同。
        *   **8.1.4.4. 用户任务与表单迁移 (Migrating User Tasks & Forms)**
            *   将原有的表单（如嵌入式HTML）迁移到 Camunda Forms (JSON-based)，或继续使用外部表单，但需要适配 C8 的 Tasklist API。
    *   **8.1.5. 迁移辅助工具 (Migration Tooling)**
        *   **8.1.5.1. Camunda 7 to 8 Migration Tooling**
            *   Camunda 官方提供了一些社区支持的工具，可以帮助扫描 C7 的 BPMN 模型，识别出与 C8 不兼容的元素和属性，并提供迁移建议。

### **9. 附录 (Appendix)**

本附录提供一些常用信息和快速参考，方便开发者在日常工作中快速查找。

*   **9.1. 常用 FEEL 表达式 (Common FEEL Expressions)**
    *   **9.1.1. 比较操作 (Comparison)**
        *   `age > 18`, `status = "approved"`, `amount <= 1000`
    *   **9.1.2. 逻辑操作 (Logical)**
        *   `age > 18 and riskScore < 50`, `order.cancelled or not customer.premium`
    *   **9.1.3. 算术操作 (Arithmetic)**
        *   `price * quantity * (1 - discount)`, `items[1].value + items[2].value`
    *   **9.1.4. 范围与列表 (Ranges and Lists)**
        *   `age in (18..65)`, `item in ["apple", "banana"]`, `some item in list satisfies item.price > 10`
    *   **9.1.5. 内置函数 (Built-in Functions)**
        *   `substring(customerName, 1, 5)`, `date and time("2023-10-27T10:00:00Z")`, `count(items)`

*   **9.2. Camunda 7 REST API 常用端点 (Common C7 REST API Endpoints)**
    *   **9.2.1. 流程定义 (Process Definition)**
        *   `GET /process-definition`: 获取流程定义列表。
        *   `POST /process-definition/{id}/start`: 启动一个流程实例。
    *   **9.2.2. 流程实例 (Process Instance)**
        *   `GET /process-instance`: 查询流程实例。
        *   `DELETE /process-instance/{id}`: 删除一个运行中的流程实例。
    *   **9.2.3. 用户任务 (User Task)**
        *   `GET /task`: 查询任务。
        *   `POST /task/{id}/complete`: 完成一个任务。
        *   `POST /task/{id}/claim`: 认领一个任务。
    *   **9.2.4. 消息 (Message)**
        *   `POST /message`: 发送一个消息以关联或启动流程。
    *   **9.2.5. 外部任务 (External Task)**
        *   `POST /external-task/fetchAndLock`: 拉取并锁定外部任务。
        *   `POST /external-task/{id}/complete`: 完成一个外部任务。

*   **9.3. 常见问题解答 (Frequently Asked Questions - FAQ)**
    *   **9.3.1. 如何在服务任务中获取当前流程实例ID？**
        *   C7: `delegateExecution.getProcessInstanceId()`。 C8: `activatedJob.getProcessInstanceKey()` (key是long类型)。
    *   **9.3.2. Camunda 是否支持动态流程？**
        *   C7 提供了 `ProcessInstanceModification` API，允许在运行时动态地添加/删除活动实例，实现非常灵活的流程跳转和修改。C8 目前不支持此功能。
    *   **9.3.3. 我应该选择 Camunda 7 还是 Camunda 8？**
        *   新项目、追求云原生和大规模部署，优先考虑 C8。现有成熟项目、依赖 C7 特定功能（如 CMMN、脚本任务的便利性）或环境限制，C7 仍是稳定选择。
    *   **9.3.4. Camunda 是免费的吗？**
        *   Camunda 社区版是免费且开源的 (Camunda 7 基于 Apache 2.0, Zeebe 基于 Camunda Platform Public License)。企业版提供额外的工具（如 Optimize）和官方技术支持。
    *   **9.3.5. 如何在流程之间传递数据？**
        *   使用调用活动（Call Activity）时，通过输入/输出参数映射来精确控制父子流程之间的数据传递，避免不必要的变量污染。
