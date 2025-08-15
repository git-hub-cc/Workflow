// src/utils/customTranslate.js
import translations from 'bpmn-js-i18n/translations/zn.js';

export default function customTranslate(template, replacements) {
    if (replacements) {
        template = template.replace(/{([^}]+)}/g, (_, key) => replacements[key] || `{${key}}`);
    }
    return translations[template] || template;
}
