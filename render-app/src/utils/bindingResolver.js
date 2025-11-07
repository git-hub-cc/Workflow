/**
 * @description 一个简单的工具函数，用于解析低代码组件中的数据绑定。
 * @param {any} value - 可能包含绑定表达式的属性值 (e.g., "{{ product.name }}", "静态文本").
 * @param {object | null} dataContext - 用于解析绑定的数据上下文 (e.g., { product: { name: '电脑' } }).
 * @returns {any} - 解析后的真实值。
 */
export function resolveBinding(value, dataContext) {
    // 如果值不是字符串，或者数据上下文不存在，直接返回值
    if (typeof value !== 'string' || !dataContext) {
        return value;
    }

    // 正则表达式匹配 {{ a.b.c }} 格式的绑定
    const match = value.match(/^{{\s*([\w.-]+)\s*}}$/);
    if (!match) {
        return value; // 没有匹配到绑定语法，返回原始字符串
    }

    const path = match[1]; // 提取路径，例如 'product.name'

    // 使用 reduce 安全地访问深层嵌套的对象属性
    try {
        const resolvedValue = path.split('.').reduce((obj, key) => {
            return obj && obj[key] !== 'undefined' ? obj[key] : undefined;
        }, dataContext);

        return resolvedValue === undefined ? `[绑定错误: ${path}]` : resolvedValue;
    } catch (error) {
        console.warn(`解析数据绑定路径 "${path}" 时出错:`, error);
        return `[绑定错误: ${path}]`;
    }
}