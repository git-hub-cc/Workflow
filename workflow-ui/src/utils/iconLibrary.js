/**
 * @file src/utils/iconLibrary.js
 * @description
 * 这是一个专门用于管理和提供 Ant Design Vue 图标的模块。
 * 为了避免 Vite 等构建工具的 tree-shaking 问题，我们在这里显式导入所有需要用到的图标。
 * 同时，这也提供了一个精选的、与本应用主题相关的图标库，减小了打包体积。
 */

// 1. 从 @ant-design/icons-vue 显式导入所有需要的图标
import {
    // 通用
    HomeOutlined, SettingOutlined, AppstoreOutlined, ProfileOutlined, DashboardOutlined,
    QuestionCircleOutlined, InfoCircleOutlined, ExclamationCircleOutlined, SmileOutlined,
    SearchOutlined, PlusOutlined, DeleteOutlined, EditOutlined, SaveOutlined,
    UploadOutlined, DownloadOutlined, CopyOutlined, EyeOutlined, SyncOutlined,
    ArrowUpOutlined, ArrowDownOutlined, ArrowLeftOutlined, ArrowRightOutlined,

    // 用户与权限
    UserOutlined, TeamOutlined, UsergroupAddOutlined, CrownOutlined, IdcardOutlined,
    SafetyCertificateOutlined, LockOutlined, UnlockOutlined,

    // 表单与文档
    FormOutlined, FileTextOutlined, FilePdfOutlined, FileImageOutlined, FileDoneOutlined,
    FolderOutlined, FolderOpenOutlined, SnippetsOutlined, SolutionOutlined,

    // 工作流与组织
    ApartmentOutlined, BranchesOutlined, NodeIndexOutlined, DeploymentUnitOutlined,
    CheckCircleOutlined, CloseCircleOutlined, ClockCircleOutlined, PlayCircleOutlined,
    PauseCircleOutlined, StopOutlined,
    // 【核心新增】
    ForkOutlined, PlusCircleOutlined,

    // 消息与通知
    MailOutlined, BellOutlined, MessageOutlined, CommentOutlined,

    // 数据与图表
    BankOutlined, AccountBookOutlined, CalculatorOutlined, BarChartOutlined, LineChartOutlined,
    PieChartOutlined, AreaChartOutlined, DatabaseOutlined,

} from '@ant-design/icons-vue';

// 2. 将所有图标组件放入一个对象/Map 中，方便按名称查找
export const iconMap = {
    HomeOutlined, SettingOutlined, AppstoreOutlined, ProfileOutlined, DashboardOutlined,
    QuestionCircleOutlined, InfoCircleOutlined, ExclamationCircleOutlined, SmileOutlined,
    SearchOutlined, PlusOutlined, DeleteOutlined, EditOutlined, SaveOutlined,
    UploadOutlined, DownloadOutlined, CopyOutlined, EyeOutlined, SyncOutlined,
    ArrowUpOutlined, ArrowDownOutlined, ArrowLeftOutlined, ArrowRightOutlined,
    UserOutlined, TeamOutlined, UsergroupAddOutlined, CrownOutlined, IdcardOutlined,
    SafetyCertificateOutlined, LockOutlined, UnlockOutlined,
    FormOutlined, FileTextOutlined, FilePdfOutlined, FileImageOutlined, FileDoneOutlined,
    FolderOutlined, FolderOpenOutlined, SnippetsOutlined, SolutionOutlined,
    ApartmentOutlined, BranchesOutlined, NodeIndexOutlined, DeploymentUnitOutlined,
    CheckCircleOutlined, CloseCircleOutlined, ClockCircleOutlined, PlayCircleOutlined,
    PauseCircleOutlined, StopOutlined,
    ForkOutlined, PlusCircleOutlined, // 【核心新增】
    MailOutlined, BellOutlined, MessageOutlined, CommentOutlined,
    BankOutlined, AccountBookOutlined, CalculatorOutlined, BarChartOutlined, LineChartOutlined,
    PieChartOutlined, AreaChartOutlined, DatabaseOutlined,
};

// 3. 创建一个数组，用于在 IconPickerModal 中循环渲染
export const iconList = Object.keys(iconMap).map(name => ({
    name: name,
    component: iconMap[name],
}));