/**
 * 递归地将嵌套的字段结构（例如在布局容器中）扁平化为一个单层数组。
 * @param {Array} fields - 字段数组，可能包含嵌套结构。
 * @returns {Array} - 扁平化的字段数组。
 */
export function flattenFields(fields) {
    if (!fields || !Array.isArray(fields)) return [];

    let flatList = [];
    fields.forEach(field => {
        flatList.push(field);
        // 【修改】同时处理 GridRow 和 Collapse
        if (field.type === 'GridRow' && field.columns) {
            field.columns.forEach(col => {
                flatList = flatList.concat(flattenFields(col.fields));
            });
        } else if (field.type === 'Collapse' && field.panels) {
            field.panels.forEach(panel => {
                flatList = flatList.concat(flattenFields(panel.fields));
            });
        }
    });
    return flatList;
}

/**
 * 根据表单定义，初始化 formData 对象。
 * @param {Array} fields - 表单定义中的字段数组。
 * @param {Object} formData - 要被初始化的响应式 formData 对象。
 */
export function initFormData(fields, formData) {
    flattenFields(fields).forEach(field => {
        // 【修改】增加对新组件的类型判断
        if (['GridRow', 'GridCol', 'Collapse', 'CollapsePanel', 'DescriptionList'].includes(field.type)) return;

        if (['Subform', 'FileUpload', 'KeyValue'].includes(field.type)) {
            formData[field.id] = [];
        } else {
            formData[field.id] = undefined;
        }
    });
}