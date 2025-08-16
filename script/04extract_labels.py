import re

def extract_labels_from_file(filename):
    """
    从指定文件中读取内容，并提取所有 label: translate('...') 中的文本。
    """
    try:
        # 以只读模式打开文件，使用utf-8编码
        with open(filename, 'r', encoding='utf-8') as file:
            content = file.read()
    except FileNotFoundError:
        print(f"错误：文件 '{filename}' 未找到。请确保文件存在且路径正确。")
        return []

    # 正则表达式，用于匹配 "label: translate('...')" 模式并捕获引号内的内容
    # \s* 用于匹配可选的空格，增加匹配的灵活性
    # (.*?) 用于非贪婪地捕获单引号内的任何字符
    pattern = re.compile(r"label: translate\(\s*'(.*?)'\s*\)")

    # 使用 findall 方法查找所有匹配项
    matches = pattern.findall(content)

    return matches

# --- 主程序执行部分 ---
if __name__ == "__main__":
    # 定义要读取的文件名
    input_file = 'bpmn-js-properties-panel.js.map'

    # 调用函数提取数据
    extracted_data = extract_labels_from_file(input_file)

    # 打印结果
    if extracted_data:
        print("成功提取到以下内容：")
        # 遍历列表并打印每一个提取到的内容
        for item in extracted_data:
            print(item)
    else:
        print("在文件中未找到匹配的内容。")