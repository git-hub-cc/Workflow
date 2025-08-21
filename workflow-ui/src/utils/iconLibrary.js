/**
 * @file src/utils/iconLibrary.js
 * @description
 * 这是一个专门用于管理和提供 Ant Design Vue 图标的模块。
 * 为了避免 Vite 等构建工具的 tree-shaking 问题，我们在这里显式导入所有需要用到的图标。
 * 同时，这也提供了一个精选的、与本应用主题相关的图标库，减小了打包体积。
 */

// 1. 从 @ant-design/icons-vue 显式导入所有需要的图标
import {
    // ===================================================================
    // 通用图标 (General)
    // ===================================================================
    AppstoreOutlined,
    CheckCircleOutlined,
    ClockCircleOutlined,
    CloseCircleOutlined,
    DashboardOutlined,
    ExclamationCircleOutlined,
    HomeOutlined,
    InfoCircleOutlined,
    ProfileOutlined,
    QuestionCircleOutlined,
    SettingOutlined,
    SmileOutlined,
    SyncOutlined,
    WarningOutlined,

    // ===================================================================
    // 方向图标 (Directional)
    // ===================================================================
    ArrowDownOutlined,
    ArrowLeftOutlined,
    ArrowRightOutlined,
    ArrowUpOutlined,
    DownOutlined,
    LeftOutlined,
    RightOutlined,
    UpOutlined,

    // ===================================================================
    // 操作图标 (Actions)
    // ===================================================================
    CopyOutlined,
    DeleteOutlined,
    DownloadOutlined,
    DragOutlined,
    EditOutlined,
    EyeOutlined,
    EyeInvisibleOutlined,
    FilterOutlined,
    FullscreenOutlined,
    FullscreenExitOutlined,
    PlusOutlined,
    PlusCircleOutlined,
    RedoOutlined,
    ReloadOutlined,
    SaveOutlined,
    SearchOutlined,
    UndoOutlined,
    UploadOutlined,
    ZoomInOutlined,
    ZoomOutOutlined,

    // ===================================================================
    // 用户与安全 (User & Security)
    // ===================================================================
    CrownOutlined,
    IdcardOutlined,
    KeyOutlined,
    LockOutlined,
    SafetyCertificateOutlined,
    TeamOutlined,
    UnlockOutlined,
    UserOutlined,
    UsergroupAddOutlined,

    // ===================================================================
    // 文档与文件 (Document & File)
    // ===================================================================
    FileDoneOutlined,
    FileImageOutlined,
    FilePdfOutlined,
    FileSearchOutlined,
    FileTextOutlined,
    FolderOpenOutlined,
    FolderOutlined,
    SnippetsOutlined,
    SolutionOutlined,

    // ===================================================================
    // 表单与编辑器 (Form & Editor)
    // ===================================================================
    AlignCenterOutlined,
    AlignLeftOutlined,
    AlignRightOutlined,
    BoldOutlined,
    FormOutlined,
    ItalicOutlined,
    LinkOutlined,
    OrderedListOutlined,
    PaperClipOutlined,
    TableOutlined,
    UnorderedListOutlined,

    // ===================================================================
    // 工作流与组织 (Workflow & Organization)
    // ===================================================================
    ApartmentOutlined,
    BranchesOutlined,
    BuildOutlined,
    DeploymentUnitOutlined,
    ForkOutlined,
    GatewayOutlined,
    NodeIndexOutlined,
    PauseCircleOutlined,
    PlayCircleOutlined,
    StopOutlined,

    // ===================================================================
    // 消息与通知 (Message & Notification)
    // ===================================================================
    BellOutlined,
    CommentOutlined,
    MailOutlined,
    MessageOutlined,
    SendOutlined,

    // ===================================================================
    // 数据与图表 (Data & Chart)
    // ===================================================================
    AreaChartOutlined,
    BarChartOutlined,
    DatabaseOutlined,
    DotChartOutlined,
    LineChartOutlined,
    PieChartOutlined,

    // ===================================================================
    // 商业与金融 (Business & Finance)
    // ===================================================================
    AccountBookOutlined,
    BankOutlined,
    CalculatorOutlined,
    CreditCardOutlined,
    GiftOutlined,
    ShopOutlined,
    ShoppingCartOutlined,
    TagsOutlined,
    WalletOutlined,

    // ===================================================================
    // 设备与硬件 (Device & Hardware)
    // ===================================================================
    CameraOutlined,
    DesktopOutlined,
    LaptopOutlined,
    MobileOutlined,
    PrinterOutlined,

} from '@ant-design/icons-vue';

// 2. 将所有图标组件放入一个对象/Map 中，方便按名称查找
export const iconMap = {
    // General
    AppstoreOutlined, CheckCircleOutlined, ClockCircleOutlined, CloseCircleOutlined, DashboardOutlined,
    ExclamationCircleOutlined, HomeOutlined, InfoCircleOutlined, ProfileOutlined, QuestionCircleOutlined,
    SettingOutlined, SmileOutlined, SyncOutlined, WarningOutlined,

    // Directional
    ArrowDownOutlined, ArrowLeftOutlined, ArrowRightOutlined, ArrowUpOutlined, DownOutlined,
    LeftOutlined, RightOutlined, UpOutlined,

    // Actions
    CopyOutlined, DeleteOutlined, DownloadOutlined, DragOutlined, EditOutlined, EyeOutlined,
    EyeInvisibleOutlined, FilterOutlined, FullscreenOutlined, FullscreenExitOutlined, PlusOutlined,
    PlusCircleOutlined, RedoOutlined, ReloadOutlined, SaveOutlined, SearchOutlined, UndoOutlined,
    UploadOutlined, ZoomInOutlined, ZoomOutOutlined,

    // User & Security
    CrownOutlined, IdcardOutlined, KeyOutlined, LockOutlined, SafetyCertificateOutlined,
    TeamOutlined, UnlockOutlined, UserOutlined, UsergroupAddOutlined,

    // Document & File
    FileDoneOutlined, FileImageOutlined, FilePdfOutlined, FileSearchOutlined, FileTextOutlined,
    FolderOpenOutlined, FolderOutlined, SnippetsOutlined, SolutionOutlined,

    // Form & Editor
    AlignCenterOutlined, AlignLeftOutlined, AlignRightOutlined, BoldOutlined, FormOutlined,
    ItalicOutlined, LinkOutlined, OrderedListOutlined, PaperClipOutlined, TableOutlined,
    UnorderedListOutlined,

    // Workflow & Organization
    ApartmentOutlined, BranchesOutlined, BuildOutlined, DeploymentUnitOutlined, ForkOutlined,
    GatewayOutlined, NodeIndexOutlined, PauseCircleOutlined, PlayCircleOutlined, StopOutlined,

    // Message & Notification
    BellOutlined, CommentOutlined, MailOutlined, MessageOutlined, SendOutlined,

    // Data & Chart
    AreaChartOutlined, BarChartOutlined, DatabaseOutlined, DotChartOutlined, LineChartOutlined,
    PieChartOutlined,

    // Business & Finance
    AccountBookOutlined, BankOutlined, CalculatorOutlined, CreditCardOutlined, GiftOutlined,
    ShopOutlined, ShoppingCartOutlined, TagsOutlined, WalletOutlined,

    // Device & Hardware
    CameraOutlined, DesktopOutlined, LaptopOutlined, MobileOutlined, PrinterOutlined,
};

// 3. 创建一个数组，用于在 IconPickerModal 中循环渲染
export const iconList = Object.keys(iconMap).map(name => ({
    name: name,
    component: iconMap[name],
}));