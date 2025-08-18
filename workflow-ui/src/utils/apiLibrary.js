/**
 * @file src/utils/apiLibrary.js
 * @description
 * 预定义的、可供表单设计器中“数据选择器”组件使用的API列表。
 * 这样做的好处是集中管理，方便维护，并为表单设计者提供便利。
 */
export const predefinedApis = [
    {
        name: '系统用户列表',
        url: '/api/workflows/users',
        description: '获取所有系统用户的ID和姓名。',
    },
    {
        name: '系统角色列表',
        url: '/api/admin/roles',
        description: '获取所有已定义的角色。',
    },
    {
        name: '系统用户组列表',
        url: '/api/admin/groups',
        description: '获取所有已定义的用户组。',
    },
    // 在这里可以添加更多自定义的业务API
    // {
    //   name: '产品列表',
    //   url: '/api/products',
    //   description: '获取所有产品的基本信息。',
    // },
];