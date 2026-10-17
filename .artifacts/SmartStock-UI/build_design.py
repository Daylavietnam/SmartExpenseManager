from pathlib import Path
import html, json, zipfile
from PIL import Image, ImageDraw, ImageFont

OUT=Path(__file__).parent
OUT.mkdir(exist_ok=True)
P='#087F72'; BG='#F3F6F8'; T='#172B36'; M='#647985'; B='#E1E9ED'; A='#DDF3EE'; W='#FFFFFF'; O='#D96545'
screens=[]; nodes=[]
def rect(x,y,w,h,c=W,r=14,stroke=None):
    nodes.append(dict(kind='rect',x=x,y=y,w=w,h=h,c=c,r=r,stroke=stroke))
def txt(x,y,s,size=14,c=T,bold=False):
    nodes.append(dict(kind='text',x=x,y=y,s=s,size=size,c=c,bold=bold))
def card(y,h=100): rect(20,y,350,h,W,20,B)
def button(y,s,x=20,w=350,c=P):
    rect(x,y,w,48,c); txt(x+16,y+15,s,15,W if c==P else P,True)
def field(y,label,value,x=20,w=350):
    txt(x,y,label,12,M); rect(x,y+22,w,46,W,12,B); txt(x+14,y+37,value,14)
def section(y,s,action=None):
    txt(20,y,s,17,T,True)
    if action: txt(294,y+2,action,12,P,True)
def nav(active=0):
    rect(0,766,390,78,W,0); rect(0,766,390,1,B,0)
    labels=['Trang chủ','Nhập–xuất','Báo cáo','Lịch sử']
    icons=['⌂','↕','','≡']
    for i,l in enumerate(labels):
        x=i*97.5
        if i==active: rect(x+23,774,52,28,A,14)
        txt(x+40,777,icons[i],20,P if i==active else M,True)
        if i==2:
            for j,h in enumerate([9,17,13]): rect(x+38+j*6,799-h,4,h,P if i==active else M,1)
        txt(x+17,810,l,12,P if i==active else M,i==active)
def start(name,title,sub,active=None):
    global nodes
    nodes=[]; screens.append(dict(name=name,nodes=nodes))
    rect(0,0,390,844,BG,0); txt(22,12,'9:41',12,T,True); txt(306,12,'LTE  100%',11,M)
    rect(20,44,28,28,P,8); txt(27,49,'S',18,W,True)
    txt(58,48,'SmartStock',18,T,True); txt(327,49,'•••',18,M)
    txt(20,98,title,26,T,True); txt(20,136,sub,13,M)
    if active is not None: nav(active)

start('01 - Tong quan','Tổng quan kho','Thứ Sáu, 16 tháng 10, 2026',0)
rect(20,174,350,148,'#075E54',22)
txt(40,195,'GIÁ TRỊ TỒN KHO',12,'#BCE8DB',True)
txt(40,224,'18.650.000 đ',32,W,True)
txt(40,278,'Theo giá tham chiếu của sản phẩm',12,'#BCE8DB')
for x,label,value in [(20,'Nhập tháng này','12.500.000 đ'),(201,'Xuất tháng này','8.200.000 đ')]:
    rect(x,336,169,90,W,16,B); txt(x+16,352,label,12,M); txt(x+16,379,value,19,P if x==20 else O,True)
section(451,'Quản lý kho')
for x,label,sub in [(20,'Sản phẩm','24 mặt hàng'),(201,'Đối tác','Khách hàng · NCC')]:
    rect(x,480,169,84,W,16,B); txt(x+16,496,label,16,T,True); txt(x+16,526,sub,12,M)
section(590,'Cần nhập thêm','Xem tất cả')
card(620,66); txt(36,635,'Bánh quy bơ',15,T,True); txt(36,659,'BK001 · Tối thiểu 10 hộp',12,M)
rect(280,639,72,28,'#FFF0E9',10); txt(291,646,'Còn 8',13,O,True)
button(704,'Trò chuyện với trợ lý AI  →')

start('02 - San pham','Sản phẩm','Theo dõi số lượng và giá tham chiếu')
field(176,'TÌM KIẾM','Tìm tên hoặc mã sản phẩm…')
rect(20,258,90,30,A,15); txt(34,266,'Tất cả (24)',12,P,True)
txt(126,266,'Sắp hết (3)',12,M); txt(245,266,'Đồ uống',12,M)
for y,name,sku,qty,price,low in [(310,'Nước suối 500ml','NS001 · Chai','120','5.000 đ',False),(430,'Bánh quy bơ','BK001 · Hộp','8','25.000 đ',True),(550,'Kẹo trái cây','KT001 · Gói','36','15.000 đ',False)]:
    card(y,104); rect(36,y+16,42,42,A,12); txt(47,y+25,sku[:2],16,P,True)
    txt(90,y+17,name,16,T,True); txt(90,y+42,sku,12,M)
    txt(36,y+77,'Giá: '+price,13,M); txt(257,y+77,'Tồn: '+qty,14,O if low else P,True)
button(706,'+  Thêm sản phẩm')
txt(20,778,'‹  Trở về tổng quan',14,P)

for outgoing in [False,True]:
    start('04 - Xuat kho' if outgoing else '03 - Nhap kho','Tạo phiếu xuất' if outgoing else 'Tạo phiếu nhập','Ghi nhận nhiều sản phẩm trong một phiếu',1)
    rect(20,172,350,42,B,12); rect(195 if outgoing else 24,176,171,34,W,10)
    txt(70,185,'Nhập kho',14,M if outgoing else P,not outgoing); txt(245,185,'Xuất kho',14,P if outgoing else M,outgoing)
    field(230,'Khách hàng' if outgoing else 'Nhà cung cấp','Chị Lan  ›' if outgoing else 'Công ty An Phát  ›')
    field(310,'Ngày lập phiếu','16/10/2026  ▾')
    section(397,'Sản phẩm (2)','+ Thêm')
    for y,name,detail,total in [(430,'Nước suối 500ml','10 chai × 5.000 đ' if outgoing else '50 chai × 3.000 đ','50.000 đ' if outgoing else '150.000 đ'),(516,'Bánh quy bơ','2 hộp × 25.000 đ' if outgoing else '20 hộp × 18.000 đ','50.000 đ' if outgoing else '360.000 đ')]:
        card(y,74); txt(36,y+14,name,15,T,True); txt(36,y+43,detail,13,M); txt(277,y+43,total,13,P,True); txt(338,y+13,'×',16,M)
    txt(20,611,'+ Thêm ghi chú',13,P)
    txt(20,650,'Tổng tiền',14,M); txt(245,645,'100.000 đ' if outgoing else '510.000 đ',22,T,True)
    button(698,'Lưu phiếu xuất' if outgoing else 'Lưu phiếu nhập')

start('05 - Doi tac','Đối tác','Khách hàng và nhà cung cấp')
field(176,'TÌM KIẾM','Tìm tên hoặc số điện thoại…')
rect(20,258,72,32,A,16); txt(35,267,'Tất cả',12,P,True); txt(111,267,'Khách hàng',12,M); txt(237,267,'Nhà cung cấp',12,M)
for y,name,role,phone,initial in [(314,'Công ty An Phát','Nhà cung cấp','0900 000 101','AP'),(432,'Chị Lan','Khách hàng','0900 000 202','L'),(550,'Cửa hàng Minh Anh','Khách hàng','0900 000 303','MA')]:
    card(y,102); rect(36,y+16,44,44,A,22); txt(47,y+29,initial,15,P,True)
    txt(94,y+18,name,16,T,True); txt(94,y+45,phone,13,M)
    txt(36,y+77,role,12,P); txt(265,y+77,'Xem phiếu  ›',12,M)
button(706,'+  Thêm đối tác'); txt(20,778,'‹  Trở về tổng quan',14,P)

start('06 - Lich su','Lịch sử phiếu','Tra cứu các lần nhập và xuất kho',3)
field(176,'TÌM PHIẾU','Mã phiếu, sản phẩm, đối tác…')
rect(20,258,350,40,W,12,B); txt(35,271,'Tháng 10, 2026',13,T,True); txt(255,271,'Tất cả  ▾',13,P)
section(321,'Hôm nay')
card(354,230); rect(36,370,72,26,A,8); txt(47,377,'Nhập kho',11,P,True)
txt(233,376,'NK-161026-01',12,M)
txt(36,413,'Công ty An Phát',18,T,True); txt(36,441,'09:30 · 2 sản phẩm',12,M)
txt(36,477,'Nước suối 500ml × 50',13); txt(36,504,'Bánh quy bơ × 20',13)
rect(36,533,318,1,B,0); txt(36,551,'Tổng tiền',13,M); txt(263,548,'510.000 đ',16,P,True)
card(600,128); rect(36,616,72,26,'#FFF0E9',8); txt(47,623,'Xuất kho',11,O,True); txt(233,622,'XK-161026-01',12,M)
txt(36,657,'Chị Lan',18,T,True); txt(36,693,'10:15 · 2 sản phẩm',12,M); txt(263,691,'100.000 đ',16,O,True)

start('07 - Bao cao','Báo cáo kho','Theo dõi giá trị nhập và xuất',2)
rect(20,174,350,42,W,12,B); txt(36,188,'‹',14,P); txt(128,188,'Tháng 10, 2026',14,T,True); txt(341,188,'›',14,P)
for x,label,value,col in [(20,'Giá trị nhập','12,5 triệu',P),(201,'Giá trị xuất','8,2 triệu',O)]:
    rect(x,230,169,90,W,16,B); txt(x+16,247,label,12,M); txt(x+16,274,value,22,col,True)
card(340,242); txt(36,359,'Nhập – xuất theo tuần',17,T,True); txt(36,390,'● Nhập',12,P); txt(117,390,'● Xuất',12,O); txt(269,391,'Triệu đồng',11,M)
for i,(a,b) in enumerate([(3,2),(4.5,3),(5,3.2)]):
    x=77+i*102; rect(x,538-a*22,22,a*22,P,4); rect(x+28,538-b*22,22,b*22,O,4); txt(x+4,549,'Tuần '+str(i+1),11,M)
card(598,130); txt(36,616,'Chênh lệch nhập – xuất',15,T,True); txt(36,644,'+4.300.000 đ',24,P,True); txt(36,688,'Chênh lệch giá trị, không phải lợi nhuận.',12,M)

start('08 - Tro ly AI','Trợ lý kho AI','Nhập – xuất nhanh bằng tin nhắn')
rect(20,174,350,54,A,14); txt(34,186,'AI sẽ hỏi thêm khi thiếu giá hoặc đối tác.',12,P); txt(34,205,'Tồn kho chỉ đổi khi đủ thông tin.',12,P)
rect(68,252,302,76,P,18); txt(84,269,'Xuất 10 chai nước suối 500ml,',14,W); txt(84,294,'giá 5.000 đồng mỗi chai.',14,W)
txt(20,353,'SMARTSTOCK AI',11,P,True)
rect(20,375,320,102,W,18,B); txt(36,392,'Bạn bán cho khách hàng nào?',15,T,True); txt(36,419,'Hãy gửi tên, số điện thoại hoặc',14,M); txt(36,443,'mã khách hàng đã lưu.',14,M)
rect(246,499,124,48,P,16); txt(263,515,'Khách Chị Lan',14,W)
rect(20,574,320,128,W,18,B); txt(36,590,'Đã ghi nhận xuất kho',16,P,True); txt(36,619,'10 chai Nước suối 500ml',14); txt(36,644,'Khách hàng: Chị Lan',14,M); txt(36,672,'Tổng tiền: 50.000 đ',14,T,True)
rect(20,744,294,54,W,16,B); txt(36,763,'Nhập tin nhắn…',14,M); rect(326,744,44,54,P,16); txt(339,759,'↑',24,W,True)
txt(20,817,'‹  Trở về tổng quan',12,P)

start('09 - Cai dat','Cài đặt','Bảo mật và dữ liệu của bạn')
section(184,'Bảo mật')
card(218,162); txt(36,239,'Khóa bằng mã PIN',16,T,True); txt(36,266,'Yêu cầu mã khi mở ứng dụng',12,M)
rect(304,241,48,26,P,13); rect(328,244,20,20,W,10)
rect(36,298,318,1,B,0); txt(36,317,'Đăng nhập vân tay',16,T,True); txt(36,345,'Dùng cảm biến của thiết bị',12,M)
rect(304,322,48,26,B,13); rect(308,325,20,20,W,10)
section(411,'Dữ liệu')
card(446,88); txt(36,466,'Xuất dữ liệu Excel',16,T,True); txt(36,494,'Lưu danh sách và giao dịch về máy',12,M); txt(339,478,'›',20,P)
card(560,95); txt(36,579,'SmartStock',18,P,True); txt(36,610,'Quản lý kho đơn giản, rõ ràng.',13,M)
button(706,'Lưu cài đặt'); txt(20,778,'Hủy và trở về',14,M)

def font(n): return ImageFont.truetype('C:/Windows/Fonts/arialbd.ttf' if n.get('bold') else 'C:/Windows/Fonts/arial.ttf',n['size'])
for s in screens:
    for n in s['nodes']:
        if n['kind']=='text': n['s']=n['s'].replace('▾','v')

def svg_body(ns):
    out=[]
    for n in ns:
        x,y=n['x'],n['y']
        if n['kind']=='rect': out.append(f'<rect x="{x}" y="{y}" width="{n["w"]}" height="{n["h"]}" rx="{n["r"]}" fill="{n["c"]}" stroke="{n.get("stroke") or "none"}"/>')
        else: out.append(f'<text x="{x}" y="{y+n["size"]*.88}" font-family="Arial, sans-serif" font-size="{n["size"]}" font-weight="{700 if n["bold"] else 400}" fill="{n["c"]}">{html.escape(n["s"])}</text>')
    return ''.join(out)
def svg(ns,w=390,h=844): return f'<svg xmlns="http://www.w3.org/2000/svg" width="{w}" height="{h}" viewBox="0 0 {w} {h}">'+svg_body(ns)+'</svg>'
def render(ns):
    im=Image.new('RGB',(390,844),BG); d=ImageDraw.Draw(im)
    for n in ns:
        x,y=n['x'],n['y']
        if n['kind']=='rect': d.rounded_rectangle((x,y,x+n['w'],y+n['h']),radius=n['r'],fill=n['c'],outline=n.get('stroke'))
        else:
            f=font(n)
            assert x+d.textlength(n['s'],font=f)<=391,(n['s'],'text overflows')
            d.text((x,y),n['s'],font=f,fill=n['c'],anchor='lt')
    return im
board=Image.new('RGB',(1350,2880),'#E7EFEE'); d=ImageDraw.Draw(board)
d.text((40,35),'SmartStock / Giao diện cơ bản',font=ImageFont.truetype('C:/Windows/Fonts/arialbd.ttf',36),fill=T)
d.text((40,89),'9 màn hình · 390 × 844 · Dữ liệu minh họa',font=ImageFont.truetype('C:/Windows/Fonts/arial.ttf',20),fill=M)
groups=[]
for i,s in enumerate(screens):
    path=OUT/(s['name']+'.svg'); path.write_text(svg(s['nodes']),encoding='utf-8')
    im=render(s['nodes']); im.save(OUT/(s['name']+'.png'))
    x=40+(i%3)*440; y=190+(i//3)*890
    board.paste(im,(x,y)); d.text((x,y-30),s['name'],font=ImageFont.truetype('C:/Windows/Fonts/arialbd.ttf',16),fill=T)
    groups.append(f'<g transform="translate({x},{y})">{svg_body(s["nodes"])}</g>')
board.save(OUT/'SmartStock-overview.png')
(OUT/'SmartStock-all-screens.svg').write_text('<svg xmlns="http://www.w3.org/2000/svg" width="1350" height="2880" viewBox="0 0 1350 2880">'+''.join(groups)+'</svg>',encoding='utf-8')
(OUT/'design.json').write_text(json.dumps(screens,ensure_ascii=False,indent=2),encoding='utf-8')
(OUT/'preview.html').write_text('<!doctype html><html lang="vi"><meta charset="utf-8"><title>SmartStock — UI</title><style>body{background:#e7efee;font-family:Arial;color:#172b36;margin:40px}main{display:grid;grid-template-columns:repeat(3,390px);gap:40px}h1{font-size:32px}h2{font-size:16px}svg{border-radius:18px;box-shadow:0 8px 24px #172b3610}</style><h1>SmartStock · Giao diện cơ bản</h1><p>9 màn hình · Dữ liệu minh họa · Thiết kế dựa trên ứng dụng hiện tại</p><main>'+''.join('<section><h2>'+s['name']+'</h2>'+svg(s['nodes'])+'</section>' for s in screens)+'</main></html>',encoding='utf-8')
(OUT/'HUONG-DAN.txt').write_text('SMARTSTOCK — BỘ GIAO DIỆN CƠ BẢN\n\n9 màn hình Android 390 × 844. Dữ liệu minh họa.\n\nCách dùng trong Figma:\n1. Tạo một file Figma Design trống.\n2. Kéo từng file SVG vào canvas, hoặc kéo SmartStock-all-screens.svg để nhập toàn bộ.\n3. Các hình được nhập dưới dạng vector. Figma có thể chuyển chữ SVG thành đường nét; dùng design.json để tham khảo nội dung chữ gốc.\n4. Sau khi nhập, lưu bản sao cục bộ từ menu Figma nếu bạn cần file .fig.\n\nSmartStock-overview.png và preview.html dùng để xem trước. Đây là bộ SVG, không phải file .fig gốc; chưa có prototype tương tác. Không thay đổi mã nguồn ứng dụng.\n',encoding='utf-8')
with zipfile.ZipFile(OUT.parent/'SmartStock-UI-Figma.zip','w',zipfile.ZIP_DEFLATED) as z:
    for p in OUT.iterdir():
        if p.is_file() and p.suffix!='.py': z.write(p,p.name)
print('Created',len(screens),'screens;',sum(len(s['nodes']) for s in screens),'editable SVG elements')
