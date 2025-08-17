/**
 * 递归地将嵌套的字段结构（例如在布局容器中）扁平化为一个单层数组。
 * @param {Array} fields - 字段数组，可能包含嵌套结构。
 * @returns {Array} - 扁平化的字段数组。
 */
export function flattenFields(fields) {
    let flatList = [];
    fields.forEach(field => {
        flatList.push(field);
        if (field.type === 'Layout' && field.props.columns) {
            field.props.columns.forEach(col => {
                flatList = flatList.concat(flattenFields(col));
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
    const allFields = flattenFields(fields);
    allFields.forEach(field => {
        if (field.type === 'Subform') {
            formData[field.id] = [];
        } else if (field.type === 'FileUpload') {
            formData[field.id] = [];
        } else {
            formData[field.id] = undefined;
        }
    });
}