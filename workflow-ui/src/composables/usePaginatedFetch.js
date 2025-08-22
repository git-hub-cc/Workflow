import { ref, reactive, toRaw } from 'vue';
import { message } from 'ant-design-vue';

/**
 * 一个用于处理分页表格数据的 Vue Composition API Hook.
 * @param {Function} apiFunc - 用于获取数据的 API 函数。它应该接受一个包含分页和筛选参数的对象。
 * @param {Object} initialFilters - 初始的筛选条件。
 * @param {Object} options - 配置项，例如默认排序。
 * @returns {Object} - 包含 loading, dataSource, pagination, handleTableChange, handleSearch, handleReset 的对象。
 */
export function usePaginatedFetch(apiFunc, initialFilters = {}, options = {}) {
    const loading = ref(false);
    const dataSource = ref([]);

    const pagination = reactive({
        current: 1,
        pageSize: 10,
        total: 0,
        showSizeChanger: true,
        showTotal: (total) => `共 ${total} 条`,
    });

    // 筛选条件的状态
    const filterState = reactive({ ...initialFilters });

    // 排序状态
    const sortState = reactive({
        sort: options.defaultSort || 'id,desc', // 默认排序
    });

    const fetchData = async () => {
        loading.value = true;
        try {
            const rawFilters = toRaw(filterState);
            const params = {
                page: pagination.current - 1,
                size: pagination.pageSize,
                sort: sortState.sort,
            };

            // 合并并清理空/null/undefined的筛选条件
            for (const key in rawFilters) {
                const value = rawFilters[key];
                if (value !== null && value !== undefined && value !== '') {
                    // 特殊处理日期范围
                    if (key.toLowerCase().includes('range') && Array.isArray(value) && value.length === 2) {
                        params.startTime = value[0].startOf('day').toISOString();
                        params.endTime = value[1].endOf('day').toISOString();
                    } else {
                        params[key] = value;
                    }
                }
            }

            const response = await apiFunc(params);
            dataSource.value = response.content;
            pagination.total = response.totalElements;
        } catch (error) {
            // 【核心优化】
            // 全局拦截器已经处理了错误消息的显示，这里不再重复提示。
            // 同时清空数据源，防止在加载失败时页面上仍显示旧数据。
            dataSource.value = [];
            pagination.total = 0;
        } finally {
            loading.value = false;
        }
    };

    const handleTableChange = (pager, filters, sorter) => {
        pagination.current = pager.current;
        pagination.pageSize = pager.pageSize;
        if (sorter && sorter.field) {
            const order = sorter.order === 'ascend' ? 'asc' : 'desc';
            sortState.sort = `${sorter.field},${order}`;
        } else {
            sortState.sort = options.defaultSort || 'id,desc';
        }
        fetchData();
    };

    const handleSearch = () => {
        pagination.current = 1;
        fetchData();
    };

    const handleReset = () => {
        Object.keys(initialFilters).forEach(key => {
            filterState[key] = initialFilters[key];
        });
        handleSearch();
    };

    return {
        loading,
        dataSource,
        pagination,
        filterState,
        handleTableChange,
        handleSearch,
        handleReset,
        fetchData,
    };
}