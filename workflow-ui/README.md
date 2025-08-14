### 使用
1.  **启动后端服务**：
    *   确保 Docker 正在运行，并且 Camunda 容器已启动 (`docker start camunda`)。
    *   在 IDE 中启动 Spring Boot 应用 (`WorkflowApplication.java`)，它将在 `http://localhost:8081` 运行。

2.  **启动前端服务**：
    *   在前端项目目录 (`/workflow-ui`) 下，打开终端。
    *   运行 `npm install` (或 `yarn install`) 安装所有依赖。
    *   运行 `npm run dev` (或 `yarn dev`) 启动 Vite 开发服务器。
    *   控制台会提示前端应用正在 `http://localhost:5173` (或其他端口) 运行。

3.  **使用应用**：
    *   打开浏览器，访问 `http://localhost:5173`。
    *   你的 Vue 应用界面将会呈现。
    *   右上角会自动选择第一个用户。
    *   现在，你可以按照第三阶段第四部分提供的**端到端测试指南**进行操作，所有流程和逻辑都是一样的，只是界面换成了更现代、更流畅的 Vue + Ant Design 版本。
