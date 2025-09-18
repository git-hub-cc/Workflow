// demo_helper.js

document.addEventListener('DOMContentLoaded', function() {
    const roles = [
        { name: '总经理 (张伟)', page: 'index.html', title: '数据看板' },
        { name: '调度员 (王芳)', page: 'dispatcher_dispatch_center.html', title: '派单中心' },
        { name: '查勘员 (陈浩)', page: 'surveyor_task_list.html', title: '我的任务 (App)' },
        { name: '审核员 (赵磊)', page: 'reviewer_case_audit.html', title: '案件审核' },
        { name: '财务 (李娜)', page: 'finance_settlement.html', title: '财务结算' },
        { name: '管理层', page: 'data_kpi_report.html', title: 'KPI报表' }
    ];

    // 获取当前页面文件名
    const currentPage = window.location.pathname.split('/').pop() || 'index.html';

    // 创建切换器HTML
    const switcherHTML = `
        <div class="demo-switcher">
            <div class="switcher-toggle">
                <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" fill="currentColor" class="bi bi-people-fill" viewBox="0 0 16 16"><path d="M7 14s-1 0-1-1 1-4 5-4 5 3 5 4-1 1-1 1H7zm4-6a3 3 0 1 0 0-6 3 3 0 0 0 0 6z"/><path fill-rule="evenodd" d="M5.216 14A2.238 2.238 0 0 1 5 13c0-1.355.68-2.75 1.936-3.72A6.325 6.325 0 0 0 5 9c-4 0-5 3-5 4s1 1 1 1h4.216zM4.5 8a2.5 2.5 0 1 0 0-5 2.5 2.5 0 0 0 0 5z"/></svg>
                <span>切换演示角色</span>
            </div>
            <div class="switcher-panel" id="switcher-panel">
                <ul>
                    ${roles.map(role => `
                        <li class="${role.page === currentPage ? 'active' : ''}">
                            <a href="${role.page}">${role.name}</a>
                        </li>
                    `).join('')}
                </ul>
            </div>
        </div>
    `;

    // 注入到页面
    document.body.insertAdjacentHTML('beforeend', switcherHTML);

    // 添加事件监听
    const toggleButton = document.querySelector('.switcher-toggle');
    const panel = document.getElementById('switcher-panel');

    toggleButton.addEventListener('click', function() {
        panel.classList.toggle('open');
    });
});