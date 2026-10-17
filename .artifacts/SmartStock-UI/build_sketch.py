from pathlib import Path
import json, random, html, zipfile
from PIL import Image, ImageDraw, ImageFont

ROOT=Path(__file__).parent
OUT=ROOT.parent/'SmartStock-PhacThao'
OUT.mkdir(exist_ok=True)
screens=json.loads((ROOT/'design.json').read_text(encoding='utf-8'))
rng=random.Random(42)
PAPER='#FFFEFA'; INK='#494943'; LIGHT='#F0EFE9'; MUTED='#77766E'
def font(size,bold=False):
    return ImageFont.truetype('C:/Windows/Fonts/arialbi.ttf' if bold else 'C:/Windows/Fonts/ariali.ttf',size)
def rough(x,y,w,h):
    pts=[(x,y),(x+w*.5,y),(x+w,y),(x+w,y+h*.5),(x+w,y+h),(x+w*.5,y+h),(x,y+h),(x,y+h*.5),(x,y)]
    return [(round(a+rng.uniform(-1.2,1.2),2),round(b+rng.uniform(-1.2,1.2),2)) for a,b in pts]
board=Image.new('RGB',(1350,2880),'#F1EFE7'); draw=ImageDraw.Draw(board)
draw.text((40,28),'SmartStock - phác thảo màn hình',font=font(30,True),fill=INK)
draw.text((42,86),'Bản nháp / 9 màn hình / dữ liệu minh họa',font=font(17),fill=MUTED)
groups=[]
for i,s in enumerate(screens):
    im=Image.new('RGB',(390,844),PAPER); d=ImageDraw.Draw(im)
    elements=[f'<rect width="390" height="844" fill="{PAPER}"/>']
    for n in s['nodes']:
        x,y=n['x'],n['y']
        if n['kind']=='rect':
            w,h=n['w'],n['h']
            if w==390 and h==844: continue
            fill=LIGHT if n['c'] not in ['#FFFFFF','#F3F6F8','#E1E9ED'] else PAPER
            if h<=2:
                pts=[(x,y),(x+w,y+.7)]
                d.line(pts,fill=MUTED,width=1)
                elements.append(f'<path d="M{x},{y} L{x+w},{y+.7}" stroke="{MUTED}" fill="none"/>')
                continue
            pts=rough(x+1,y+1,w-2,h-2)
            d.polygon(pts,fill=fill); d.line(pts,fill=MUTED,width=1)
            path='M'+' L'.join(f'{a},{b}' for a,b in pts)+' Z'
            elements.append(f'<path d="{path}" fill="{fill}" stroke="{MUTED}" stroke-width="1.1" stroke-linejoin="round"/>')
            # A second, incomplete pencil pass adds natural variation without obscuring controls.
            if w>100 and h>40:
                xx=x+5; yy=y+h+1
                d.line([(xx,yy),(xx+w*.62,yy-.8)],fill='#B9B6AC',width=1)
                elements.append(f'<path d="M{xx},{yy} l{w*.62},-.8" stroke="#B9B6AC" stroke-width=".6" fill="none"/>')
        else:
            text=n['s']; size=n['size']; bold=n['bold']
            original=ImageFont.truetype('C:/Windows/Fonts/arialbd.ttf' if bold else 'C:/Windows/Fonts/arial.ttf',size)
            limit=min(374-x,max(14,d.textlength(text,font=original)+7))
            sz=size
            while sz>9 and d.textlength(text,font=font(sz,bold))>limit: sz-=1
            f=font(sz,bold)
            col=INK if bold or n['c'] in ['#172B36','#FFFFFF'] else MUTED
            yy=y-2 if sz>19 else y-1
            d.text((x,yy),text,font=f,fill=col,anchor='lt')
            elements.append(f'<text x="{x}" y="{yy+sz}" font-family="Arial, sans-serif" font-style="italic" font-size="{sz}" font-weight="{700 if bold else 400}" fill="{col}">{html.escape(text)}</text>')
    edge=rough(1,1,388,842); d.line(edge,fill=INK,width=1)
    elements.append('<path d="M'+' L'.join(f'{a},{b}' for a,b in edge)+' Z" fill="none" stroke="'+INK+'"/>')
    body=''.join(elements)
    (OUT/(s['name']+'.svg')).write_text(f'<svg xmlns="http://www.w3.org/2000/svg" width="390" height="844" viewBox="0 0 390 844">'+body+'</svg>',encoding='utf-8')
    im.save(OUT/(s['name']+'.png'))
    x=40+i%3*440; y=190+i//3*890
    board.paste(im,(x,y)); draw.text((x,y-32),s['name'],font=font(15,True),fill=INK)
    groups.append(f'<g transform="translate({x},{y})">{body}</g>')
board.save(OUT/'SmartStock-phac-thao.png')
(OUT/'SmartStock-phac-thao.svg').write_text('<svg xmlns="http://www.w3.org/2000/svg" width="1350" height="2880" viewBox="0 0 1350 2880">'+''.join(groups)+'</svg>',encoding='utf-8')
(OUT/'HUONG-DAN.txt').write_text('Bản phác thảo SmartStock\n\nKéo SmartStock-phac-thao.svg vào canvas của một file Figma Design để nhập cả 9 màn hình. Hoặc nhập từng SVG riêng.\nFont: Arial Italic (có sẵn trên Windows). Đây là SVG vector, không phải .fig gốc; Figma có thể chuyển chữ thành đường nét hoặc thay font khi nhập. PNG dùng xem trước.\nNét lệch và màu chì là chủ ý của phong cách phác thảo. Đây là bản nháp thiết kế, chưa phải prototype bấm được.\n',encoding='utf-8')
with zipfile.ZipFile(ROOT.parent/'SmartStock-PhacThao-Figma.zip','w',zipfile.ZIP_DEFLATED) as z:
    for p in OUT.iterdir(): z.write(p,p.name)
print(OUT)

