import os
from PIL import Image

def convert_png_to_jpg_and_delete(root_dir):
    """
    遍历指定目录及其所有子目录，将PNG图片转换为JPG，并删除原文件。

    :param root_dir: 要开始遍历的根目录。
    """
    converted_count = 0
    # os.walk 会遍历指定目录下的所有子目录和文件
    for dirpath, _, filenames in os.walk(root_dir):
        for filename in filenames:
            # 检查文件是否以 .png 结尾 (不区分大小写)
            if filename.lower().endswith('.png'):
                png_path = os.path.join(dirpath, filename)

                # 构建输出的JPG文件名
                # os.path.splitext(filename)[0] 会获取不带扩展名的文件名
                jpg_filename = os.path.splitext(filename)[0] + '.jpg'
                jpg_path = os.path.join(dirpath, jpg_filename)

                try:
                    # 打开PNG图片
                    with Image.open(png_path) as img:
                        print(f"正在转换: {png_path}")

                        # PNG图片可能有透明通道(RGBA)，JPG不支持
                        # 需要将其转换为RGB模式
                        if img.mode == 'RGBA' or img.mode == 'P':
                            # 创建一个白色背景的底图
                            background = Image.new('RGB', img.size, (255, 255, 255))
                            # 将PNG图像粘贴到底图上，处理透明度
                            background.paste(img, (0, 0), img.getchannel('A') if 'A' in img.getbands() else None)
                            img_to_save = background
                        else:
                            img_to_save = img.convert('RGB')

                        # 保存为JPG, quality可以调整图片质量 (1-100)
                        img_to_save.save(jpg_path, 'jpeg', quality=95)
                        print(f"成功保存: {jpg_path}")

                    # 转换成功后，删除原始的PNG文件
                    os.remove(png_path)
                    print(f"已删除源文件: {png_path}\n")
                    converted_count += 1

                except Exception as e:
                    print(f"!!!!!! 处理文件 {png_path} 时出错: {e}\n")

    if converted_count == 0:
        print("任务完成。未找到任何PNG文件。")
    else:
        print(f"任务完成！总共转换并删除了 {converted_count} 个PNG文件。")


if __name__ == "__main__":
    # 将 '.' 作为参数，表示从当前目录开始处理
    target_directory = '.'

    print("="*50)
    print("PNG to JPG 转换脚本")
    print("="*50)
    print(f"将在目录 '{os.path.abspath(target_directory)}' 及其子目录中查找PNG文件。")
    print("\n*** 警告: 此操作会删除原始的PNG文件！ ***\n")

    # 增加一个用户确认步骤，防止误操作
    confirm = input("您确定要继续吗？(输入 'yes' 继续): ")

    if confirm.lower() == 'yes':
        print("\n开始处理...\n")
        convert_png_to_jpg_and_delete(target_directory)
    else:
        print("操作已取消。")