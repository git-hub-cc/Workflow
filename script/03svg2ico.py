from PIL import Image
import cairosvg
import os

# 输入SVG路径和输出ICO路径
svg_path = "logo.svg"
png_path = svg_path.replace(".svg", ".png")
ico_path = svg_path.replace(".svg", ".ico")

# 将SVG转换为PNG
cairosvg.svg2png(url=svg_path, write_to=png_path, output_width=256, output_height=256)

# 读取PNG并保存为ICO（多尺寸）
img = Image.open(png_path)
img.save(ico_path, format='ICO', sizes=[(16, 16), (32, 32), (48, 48), (64, 64), (128, 128), (256, 256)])

ico_path
