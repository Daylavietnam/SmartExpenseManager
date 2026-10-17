"""Render the editable wireframes in the user's reference palette."""
from pathlib import Path

root = Path(__file__).parent
source = (root / 'build_sketch.py').read_text(encoding='utf-8-sig')
source = source.replace("'SmartStock-PhacThao'", "'SmartStock-MauThamKhao'")
source = source.replace("PAPER='#FFFEFA'; INK='#494943'; LIGHT='#F0EFE9'; MUTED='#77766E'", "PAPER='#FFFFFF'; INK='#292B36'; LIGHT='#DADADA'; MUTED='#777985'")
source = source.replace("'arialbi.ttf'", "'arialbd.ttf'").replace("'ariali.ttf'", "'arial.ttf'")
source = source.replace('arialbi.ttf','arialbd.ttf').replace('ariali.ttf','arial.ttf')
source = source.replace('font-style="italic"', 'font-style="normal"')
source = source.replace("'#F1EFE7'", "'#EDEEF4'")
source = source.replace("'SmartStock - phác thảo màn hình'", "'SmartStock - bản nháp màu'")
source = source.replace("    for n in s['nodes']:", """    d.rectangle((0,34,390,82),fill='#6B70F6')
    elements.append('<rect x="0" y="34" width="390" height="48" fill="#6B70F6"/>')
    for n in s['nodes']:""")
source = source.replace("            fill=LIGHT if n['c'] not in ['#FFFFFF','#F3F6F8','#E1E9ED'] else PAPER", """            colors={'#087F72':'#A3CDF6','#075E54':'#A3CDF6','#DDF3EE':'#DADADA','#E1E9ED':'#E5E5E5','#FFF0E9':'#FBE5E9'}
            fill=colors.get(n['c'],PAPER)
            if w==390 and y==766: fill='#6B70F6'
            if n['c']=='#087F72' and y==698: fill='#A8E5B7'
            if n['c']=='#D96545': fill='#EAA2AE'
            if y==44 and w==28: fill='#6B70F6'""")
source = source.replace("            col=INK if bold or n['c'] in ['#172B36','#FFFFFF'] else MUTED", """            col=INK if bold or n['c'] in ['#172B36','#FFFFFF'] else MUTED
            if 44<=y<=70: col='#FFFFFF'
            if n['c']=='#D96545': col='#B94C66'
            if n['c']=='#087F72' and ('đ' in text or 'triệu' in text): col='#348649'
            if y>=774 and y<=822: col=INK
            if n['c']=='#BCE8DB': col='#3D526A'""")
source = source.replace("'#B9B6AC'", "'#C4C5CD'").replace('#B9B6AC','#C4C5CD')
source = source.replace('SmartStock-phac-thao','SmartStock-mau-tham-khao')
source = source.replace('SmartStock-PhacThao-Figma.zip','SmartStock-MauThamKhao-Figma.zip')
source = source.replace('Font: Arial Italic','Font: Arial')
source = source.replace('Nét lệch và màu chì là chủ ý của phong cách phác thảo.', 'Bảng màu dựa trên ảnh mẫu: tím xanh #6B70F6, xanh nhạt #A3CDF6, xám #DADADA, trắng #FFFFFF. Khung thẳng, nét đơn, giữ bố cục quản lý kho.')
# Use exact orthogonal edges and remove the duplicate pencil strokes.
source = source.replace('return [(round(a+rng.uniform(-1.2,1.2),2),round(b+rng.uniform(-1.2,1.2),2)) for a,b in pts]', 'return pts')
source = source.replace('if w>100 and h>40:', 'if False:')
source = source.replace('(x+w,y+.7)', '(x+w,y)').replace('{y+.7}', '{y}')
source = source.replace('SmartStock-MauThamKhao', 'SmartStock-KHUNG-THANG-v2')
source = source.replace('SmartStock-mau-tham-khao', 'SmartStock-KHUNG-THANG-v2')
source = source.replace('SmartStock - bản nháp màu', 'SmartStock - KHUNG THANG v2')
exec(compile(source, str(root/'build_sketch.py'), 'exec'))
