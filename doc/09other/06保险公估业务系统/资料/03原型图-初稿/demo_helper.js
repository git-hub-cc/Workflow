// demo_helper.js

// --- 全局模态框函数 ---
function createModal() {
    if (document.getElementById('global-modal')) return;

    const modalHTML = `
        <div class="modal-overlay" id="global-modal-overlay">
            <div class="modal-dialog" id="global-modal-dialog">
                <div class="modal-header">
                    <span class="modal-title" id="global-modal-title"></span>
                    <button class="modal-close" id="global-modal-close">&times;</button>
                </div>
                <div class="modal-body" id="global-modal-body"></div>
                <div class="modal-footer">
                    <button class="btn btn-primary modal-btn" id="global-modal-ok">确定</button>
                </div>
            </div>
        </div>
    `;
    document.body.insertAdjacentHTML('beforeend', modalHTML);

    const overlay = document.getElementById('global-modal-overlay');
    const closeBtn = document.getElementById('global-modal-close');
    const okBtn = document.getElementById('global-modal-ok');

    const closeModal = () => {
        overlay.classList.remove('visible');
    };

    overlay.addEventListener('click', (e) => (e.target === overlay) && closeModal());
    closeBtn.addEventListener('click', closeModal);
    okBtn.addEventListener('click', closeModal);
}

function showModal(title, message, type = 'info') {
    const titleEl = document.getElementById('global-modal-title');
    const bodyEl = document.getElementById('global-modal-body');
    const overlay = document.getElementById('global-modal-overlay');

    if (titleEl && bodyEl && overlay) {
        titleEl.textContent = title;
        bodyEl.innerHTML = message;
        overlay.classList.add('visible');
    }
}

// --- 响应式导航栏逻辑 ---
function setupResponsiveSidebar() {
    const hamburgerBtn = document.getElementById('hamburger-btn');
    const sidebar = document.querySelector('.sidebar');

    if (!hamburgerBtn || !sidebar) return;

    // Create overlay
    let overlay = document.querySelector('.sidebar-overlay');
    if (!overlay) {
        overlay = document.createElement('div');
        overlay.className = 'sidebar-overlay';
        document.body.appendChild(overlay);
    }

    const openSidebar = () => {
        sidebar.classList.add('open');
        overlay.classList.add('visible');
        document.body.style.overflow = 'hidden'; // Prevent background scroll
    };

    const closeSidebar = () => {
        sidebar.classList.remove('open');
        overlay.classList.remove('visible');
        document.body.style.overflow = '';
    };

    hamburgerBtn.addEventListener('click', openSidebar);
    overlay.addEventListener('click', closeSidebar);
}


document.addEventListener('DOMContentLoaded', function() {
    // --- 初始化 ---
    createModal();
    setupResponsiveSidebar();

    // --- 角色切换器逻辑 ---
    const roles = [
        { name: '总经理 (张伟)', page: 'index.html' },
        { name: '调度员 (王芳)', page: 'dispatcher_case_entry.html' },
        { name: '查勘员 (陈浩)', page: 'surveyor_task_list.html' },
        { name: '审核主管 (赵磊)', page: 'reviewer_case_audit.html' },
        { name: '财务 (李娜)', page: 'finance_settlement.html' },
    ];

    const currentPage = window.location.pathname.split('/').pop() || 'index.html';

    const switcherHTML = `
        <div class="demo-switcher">
            <div class="switcher-toggle">
                <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" fill="currentColor" viewBox="0 0 16 16"><path d="M7 14s-1 0-1-1 1-4 5-4 5 3 5 4-1 1-1 1H7zm4-6a3 3 0 1 0 0-6 3 3 0 0 0 0 6z"/><path fill-rule="evenodd" d="M5.216 14A2.238 2.238 0 0 1 5 13c0-1.355.68-2.75 1.936-3.72A6.325 6.325 0 0 0 5 9c-4 0-5 3-5 4s1 1 1 1h4.216zM4.5 8a2.5 2.5 0 1 0 0-5 2.5 2.5 0 0 0 0 5z"/></svg>
                <span>切换角色</span>
            </div>
            <div class="switcher-panel" id="switcher-panel">
                <ul>
                    ${roles.map(role => {
        // Handle multiple pages mapping to one role for 'active' state
        let isActive = false;
        if (role.name.includes('调度员')) {
            isActive = currentPage.startsWith('dispatcher_');
        } else if (role.name.includes('查勘员')) {
            isActive = currentPage.startsWith('surveyor_');
        } else if (role.name.includes('审核主管')) {
            isActive = currentPage.startsWith('reviewer_');
        } else if (role.name.includes('财务')) {
            isActive = currentPage.startsWith('finance_');
        } else {
            isActive = (role.page === currentPage);
        }
        return `<li class="${isActive ? 'active' : ''}"><a href="${role.page}">${role.name}</a></li>`;
    }).join('')}
                </ul>
            </div>
        </div>
    `;

    document.body.insertAdjacentHTML('beforeend', switcherHTML);

    const toggleButton = document.querySelector('.switcher-toggle');
    const panel = document.getElementById('switcher-panel');

    toggleButton.addEventListener('click', () => panel.classList.toggle('open'));

    // Close switcher if clicking outside
    document.addEventListener('click', (event) => {
        const isClickInside = toggleButton.contains(event.target) || panel.contains(event.target);
        if (!isClickInside && panel.classList.contains('open')) {
            panel.classList.remove('open');
        }
    });
});