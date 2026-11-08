/*
    CSDL QuanLyKho - SQL Server / T-SQL
    Phien ban chinh sua mo hinh lich su nhap-xuat.
    Tao database moi QuanLyKho_LichSuPhieu_v2 de khong sua/xoa database cu.
    PhieuKho la phan dau phieu; LichSuKho la cac dong san pham trong phieu.
    Chay toan bo file trong SSMS.
*/
IF DB_ID(N'QuanLyKho_LichSuPhieu_v2') IS NULL
    EXEC(N'CREATE DATABASE [QuanLyKho_LichSuPhieu_v2]');
GO
USE [QuanLyKho_LichSuPhieu_v2];
GO

IF OBJECT_ID(N'dbo.NhomHang', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.NhomHang (
        MaNhomHang INT IDENTITY(1,1) NOT NULL CONSTRAINT PK_NhomHang PRIMARY KEY,
        TenNhomHang NVARCHAR(100) NOT NULL,
        MoTa NVARCHAR(500) NULL,
        CONSTRAINT UQ_NhomHang_Ten UNIQUE (TenNhomHang)
    );
END;
GO

IF OBJECT_ID(N'dbo.SanPham', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.SanPham (
        MaSanPham INT IDENTITY(1,1) NOT NULL CONSTRAINT PK_SanPham PRIMARY KEY,
        MaHang NVARCHAR(50) NOT NULL,
        TenSanPham NVARCHAR(200) NOT NULL,
        MaNhomHang INT NOT NULL,
        DonViTinh NVARCHAR(50) NOT NULL,
        GiaThamChieu DECIMAL(18,2) NOT NULL CONSTRAINT DF_SanPham_Gia DEFAULT (0),
        MucTonToiThieu DECIMAL(18,3) NOT NULL CONSTRAINT DF_SanPham_TonMin DEFAULT (0),
        CONSTRAINT UQ_SanPham_MaHang UNIQUE (MaHang),
        CONSTRAINT CK_SanPham_GiaTon CHECK (GiaThamChieu >= 0 AND MucTonToiThieu >= 0),
        CONSTRAINT FK_SanPham_NhomHang FOREIGN KEY (MaNhomHang) REFERENCES dbo.NhomHang(MaNhomHang)
    );
END;
GO

IF OBJECT_ID(N'dbo.DoiTac', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.DoiTac (
        MaDoiTac INT IDENTITY(1,1) NOT NULL CONSTRAINT PK_DoiTac PRIMARY KEY,
        TenDoiTac NVARCHAR(200) NOT NULL,
        DienThoai NVARCHAR(30) NULL,
        DiaChi NVARCHAR(500) NULL,
        LoaiDoiTac NVARCHAR(20) NOT NULL,
        CONSTRAINT CK_DoiTac_Loai CHECK (LoaiDoiTac IN (N'customer', N'supplier'))
    );
END;
GO

/* Mot dong dau phieu cho moi phieu nhap hoac xuat. */
IF OBJECT_ID(N'dbo.PhieuKho', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.PhieuKho (
        MaPhieuKho INT IDENTITY(1,1) NOT NULL CONSTRAINT PK_PhieuKho PRIMARY KEY,
        SoPhieu NVARCHAR(100) NOT NULL,
        LoaiPhieu NVARCHAR(20) NOT NULL,
        ThoiGianUnixMs BIGINT NOT NULL,
        MaDoiTac INT NULL,
        TenDoiTacLuu NVARCHAR(200) NOT NULL CONSTRAINT DF_PhieuKho_TenDoiTac DEFAULT (N''),
        DienThoaiLuu NVARCHAR(30) NOT NULL CONSTRAINT DF_PhieuKho_DienThoai DEFAULT (N''),
        GhiChu NVARCHAR(1000) NOT NULL CONSTRAINT DF_PhieuKho_GhiChu DEFAULT (N''),
        CONSTRAINT UQ_PhieuKho_SoPhieu UNIQUE (SoPhieu),
        CONSTRAINT CK_PhieuKho_Loai CHECK (LoaiPhieu IN (N'income', N'expense')),
        CONSTRAINT FK_PhieuKho_DoiTac FOREIGN KEY (MaDoiTac) REFERENCES dbo.DoiTac(MaDoiTac)
    );
END;
GO

/* Moi dong LichSuKho la mot san pham tren phieu; cac dong cung MaPhieuKho la mot phieu. */
IF OBJECT_ID(N'dbo.LichSuKho', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.LichSuKho (
        MaLichSu INT IDENTITY(1,1) NOT NULL CONSTRAINT PK_LichSuKho PRIMARY KEY,
        MaPhieuKho INT NOT NULL,
        MaSanPham INT NOT NULL,
        MaHangLuu NVARCHAR(50) NOT NULL,
        TenSanPhamLuu NVARCHAR(200) NOT NULL,
        DonViTinhLuu NVARCHAR(50) NOT NULL,
        SoLuong DECIMAL(18,3) NOT NULL,
        DonGia DECIMAL(18,2) NOT NULL,
        ThanhTien DECIMAL(18,2) NOT NULL,
        CONSTRAINT CK_LichSuKho_SoLieu CHECK (SoLuong > 0 AND DonGia >= 0 AND ThanhTien >= 0),
        CONSTRAINT FK_LichSuKho_PhieuKho FOREIGN KEY (MaPhieuKho) REFERENCES dbo.PhieuKho(MaPhieuKho),
        CONSTRAINT FK_LichSuKho_SanPham FOREIGN KEY (MaSanPham) REFERENCES dbo.SanPham(MaSanPham)
    );
END;
GO

IF OBJECT_ID(N'dbo.TinNhanAI', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.TinNhanAI (
        MaTinNhan INT IDENTITY(1,1) NOT NULL CONSTRAINT PK_TinNhanAI PRIMARY KEY,
        NoiDung NVARCHAR(MAX) NULL,
        LoaiNguoiGui INT NOT NULL,
        ThoiGianUnixMs BIGINT NOT NULL,
        CONSTRAINT CK_TinNhanAI_NguoiGui CHECK (LoaiNguoiGui IN (0,1))
    );
END;
GO

IF OBJECT_ID(N'dbo.BaoCaoKho', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.BaoCaoKho (
        MaBaoCao INT IDENTITY(1,1) NOT NULL CONSTRAINT PK_BaoCaoKho PRIMARY KEY,
        TenBaoCao NVARCHAR(200) NOT NULL,
        TuNgay DATE NOT NULL,
        DenNgay DATE NOT NULL,
        TongTienNhap DECIMAL(18,2) NOT NULL CONSTRAINT DF_BaoCao_TongNhap DEFAULT (0),
        TongTienXuat DECIMAL(18,2) NOT NULL CONSTRAINT DF_BaoCao_TongXuat DEFAULT (0),
        ChenhLech DECIMAL(18,2) NOT NULL CONSTRAINT DF_BaoCao_ChenhLech DEFAULT (0),
        ThoiGianLapUnixMs BIGINT NOT NULL,
        GhiChu NVARCHAR(1000) NULL,
        CONSTRAINT CK_BaoCao_KhoangNgay CHECK (DenNgay >= TuNgay)
    );
END;
GO

IF OBJECT_ID(N'dbo.CaiDatBaoMat', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.CaiDatBaoMat (
        MaCaiDat INT NOT NULL CONSTRAINT PK_CaiDatBaoMat PRIMARY KEY,
        BatKhoaPIN BIT NOT NULL CONSTRAINT DF_CaiDat_BatPIN DEFAULT (0),
        MaPINHash NVARCHAR(256) NULL,
        BatKhoaVanTay BIT NOT NULL CONSTRAINT DF_CaiDat_VanTay DEFAULT (0),
        KhoaKhiMoUngDung BIT NOT NULL CONSTRAINT DF_CaiDat_KhiMo DEFAULT (0),
        SoPhutTuDongKhoa INT NULL,
        CapNhatUnixMs BIGINT NOT NULL,
        CONSTRAINT CK_CaiDat_MaDon CHECK (MaCaiDat = 1),
        CONSTRAINT CK_CaiDat_ThoiGianKhoa CHECK (SoPhutTuDongKhoa IS NULL OR SoPhutTuDongKhoa > 0)
    );
END;
GO

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name=N'IX_PhieuKho_MaDoiTac' AND object_id=OBJECT_ID(N'dbo.PhieuKho'))
    CREATE INDEX IX_PhieuKho_MaDoiTac ON dbo.PhieuKho(MaDoiTac);
GO
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name=N'IX_PhieuKho_ThoiGian' AND object_id=OBJECT_ID(N'dbo.PhieuKho'))
    CREATE INDEX IX_PhieuKho_ThoiGian ON dbo.PhieuKho(ThoiGianUnixMs);
GO
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name=N'IX_LichSuKho_MaPhieuKho' AND object_id=OBJECT_ID(N'dbo.LichSuKho'))
    CREATE INDEX IX_LichSuKho_MaPhieuKho ON dbo.LichSuKho(MaPhieuKho);
GO
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name=N'IX_LichSuKho_MaSanPham' AND object_id=OBJECT_ID(N'dbo.LichSuKho'))
    CREATE INDEX IX_LichSuKho_MaSanPham ON dbo.LichSuKho(MaSanPham);
GO

/* Tong tien phieu va tong nhap/xuat tren man hinh lich su duoc tinh tu cac dong LichSuKho. */
CREATE OR ALTER VIEW dbo.vw_LichSuNhapXuat AS
SELECT
    p.MaPhieuKho, p.SoPhieu, p.LoaiPhieu, p.ThoiGianUnixMs,
    p.MaDoiTac, p.TenDoiTacLuu, p.DienThoaiLuu, p.GhiChu,
    l.MaLichSu, l.MaSanPham, l.MaHangLuu, l.TenSanPhamLuu, l.DonViTinhLuu,
    l.SoLuong, l.DonGia, l.ThanhTien,
    SUM(l.ThanhTien) OVER (PARTITION BY p.MaPhieuKho) AS TongTienPhieu,
    COUNT_BIG(l.MaLichSu) OVER (PARTITION BY p.MaPhieuKho) AS SoDongHang
FROM dbo.PhieuKho AS p
LEFT JOIN dbo.LichSuKho AS l ON l.MaPhieuKho = p.MaPhieuKho;
GO

/* De tao ERD trong SSMS: Database Diagrams > New Database Diagram, chon 8 bang. */
