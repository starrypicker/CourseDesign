-- ============================================
-- 体育用品批发销售信息系统 数据库脚本
-- ============================================

CREATE DATABASE IF NOT EXISTS sports_sales DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE sports_sales;

-- 生产厂家信息表
DROP TABLE IF EXISTS manufacturer;
CREATE TABLE manufacturer (
    manufacturer_code VARCHAR(20) NOT NULL COMMENT '厂家代码',
    manufacturer_name VARCHAR(100) NOT NULL COMMENT '厂家名称',
    contact_person VARCHAR(50) DEFAULT NULL COMMENT '联系人',
    phone VARCHAR(20) DEFAULT NULL COMMENT '联系电话',
    address VARCHAR(200) DEFAULT NULL COMMENT '地址',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (manufacturer_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='生产厂家信息表';

-- 库存商品信息表
DROP TABLE IF EXISTS product;
CREATE TABLE product (
    product_code VARCHAR(20) NOT NULL COMMENT '商品分类编码',
    manufacturer_code VARCHAR(20) NOT NULL COMMENT '生产厂家编码',
    product_name VARCHAR(100) NOT NULL COMMENT '商品名称',
    product_desc VARCHAR(500) DEFAULT NULL COMMENT '商品说明',
    unit_price DECIMAL(10,2) NOT NULL COMMENT '单价',
    stock_quantity INT NOT NULL DEFAULT 0 COMMENT '库存数量',
    min_stock INT NOT NULL DEFAULT 10 COMMENT '最低库存预警线',
    weight DECIMAL(10,2) DEFAULT 0.00 COMMENT '单位重量(kg)',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态:1-上架,0-下架',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (product_code),
    KEY idx_manufacturer (manufacturer_code),
    CONSTRAINT fk_product_manufacturer FOREIGN KEY (manufacturer_code) REFERENCES manufacturer(manufacturer_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='库存商品信息表';

-- 顾客信息表
DROP TABLE IF EXISTS customer;
CREATE TABLE customer (
    customer_code VARCHAR(20) NOT NULL COMMENT '顾客代码',
    customer_name VARCHAR(100) NOT NULL COMMENT '顾客名称',
    contact_name VARCHAR(50) DEFAULT NULL COMMENT '联系人姓名',
    address VARCHAR(200) DEFAULT NULL COMMENT '地址',
    postal_code VARCHAR(10) DEFAULT NULL COMMENT '邮编',
    phone VARCHAR(200) DEFAULT NULL COMMENT '联系电话',
    email VARCHAR(200) DEFAULT NULL COMMENT '电子邮箱',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态:1-正常,0-禁用',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (customer_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='顾客信息表';

-- 订单信息表
DROP TABLE IF EXISTS orders;
CREATE TABLE orders (
    order_id BIGINT NOT NULL AUTO_INCREMENT COMMENT '订单号',
    order_date DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '下单日期',
    customer_code VARCHAR(20) NOT NULL COMMENT '顾客代码',
    shipping_requirement VARCHAR(200) DEFAULT NULL COMMENT '运输要求',
    can_supply TINYINT DEFAULT NULL COMMENT '能否供货标志:1-能,0-否',
    shipping_date DATETIME DEFAULT NULL COMMENT '运输日期',
    total_weight DECIMAL(10,2) DEFAULT 0.00 COMMENT '货物总重量(kg)',
    shipping_fee DECIMAL(10,2) DEFAULT 0.00 COMMENT '运费',
    total_amount DECIMAL(12,2) DEFAULT 0.00 COMMENT '订单总金额',
    payment_status TINYINT NOT NULL DEFAULT 0 COMMENT '付款状态:0-未付款,1-已付款,2-已退款',
    shipping_status TINYINT NOT NULL DEFAULT 0 COMMENT '发货状态:0-未发货,1-已发货,2-已签收',
    order_status TINYINT NOT NULL DEFAULT 0 COMMENT '订单状态:0-待确认,1-已确认,2-已完成,3-已取消',
    recipient_name VARCHAR(50) DEFAULT NULL COMMENT '收件人',
    recipient_address VARCHAR(200) DEFAULT NULL COMMENT '收件地址',
    recipient_phone VARCHAR(200) DEFAULT NULL COMMENT '收件人电话',
    payment_method VARCHAR(20) DEFAULT NULL COMMENT '支付方式',
    payment_time DATETIME DEFAULT NULL COMMENT '支付时间',
    remark VARCHAR(500) DEFAULT NULL COMMENT '备注',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (order_id),
    KEY idx_customer (customer_code),
    KEY idx_order_date (order_date),
    KEY idx_order_status (order_status),
    CONSTRAINT fk_order_customer FOREIGN KEY (customer_code) REFERENCES customer(customer_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单信息表';

-- 订单细则表
DROP TABLE IF EXISTS order_item;
CREATE TABLE order_item (
    item_id BIGINT NOT NULL AUTO_INCREMENT COMMENT '细则编号',
    order_id BIGINT NOT NULL COMMENT '订单号',
    product_code VARCHAR(20) NOT NULL COMMENT '产品分类编号',
    manufacturer_code VARCHAR(20) NOT NULL COMMENT '生产厂',
    quantity INT NOT NULL DEFAULT 0 COMMENT '数量',
    unit_price DECIMAL(10,2) NOT NULL COMMENT '单价',
    total_amount DECIMAL(12,2) NOT NULL COMMENT '总金额',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (item_id),
    KEY idx_order (order_id),
    KEY idx_product (product_code),
    CONSTRAINT fk_item_order FOREIGN KEY (order_id) REFERENCES orders(order_id) ON DELETE CASCADE,
    CONSTRAINT fk_item_product FOREIGN KEY (product_code) REFERENCES product(product_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单细则表';

-- 进货记录表
DROP TABLE IF EXISTS purchase_record;
CREATE TABLE purchase_record (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '进货记录ID',
    product_code VARCHAR(20) NOT NULL COMMENT '商品编码',
    manufacturer_code VARCHAR(20) NOT NULL COMMENT '厂家编码',
    quantity INT NOT NULL COMMENT '进货数量',
    unit_price DECIMAL(10,2) NOT NULL COMMENT '进货单价',
    total_amount DECIMAL(12,2) NOT NULL COMMENT '进货总金额',
    status TINYINT NOT NULL DEFAULT 0 COMMENT '状态:0-待入库,1-已入库,2-已取消',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_product (product_code),
    CONSTRAINT fk_purchase_product FOREIGN KEY (product_code) REFERENCES product(product_code),
    CONSTRAINT fk_purchase_manufacturer FOREIGN KEY (manufacturer_code) REFERENCES manufacturer(manufacturer_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='进货记录表';

-- 插入测试数据
INSERT INTO manufacturer (manufacturer_code, manufacturer_name, contact_person, phone, address) VALUES
('M001', '李宁体育', '张经理', '010-88886666', '北京市通州区'),
('M002', '安踏体育', '李经理', '0595-85678888', '福建省晋江市'),
('M003', '耐克中国', '王经理', '021-62888888', '上海市杨浦区'),
('M004', '阿迪达斯', '赵经理', '021-60666666', '上海市浦东新区');

INSERT INTO product (product_code, manufacturer_code, product_name, product_desc, unit_price, stock_quantity, min_stock, weight) VALUES
('P001', 'M001', '李宁跑步鞋', '李宁超轻跑步鞋，透气网面', 399.00, 100, 20, 0.35),
('P002', 'M001', '李宁篮球', '李宁CBA官方比赛用球', 189.00, 50, 10, 0.62),
('P003', 'M002', '安踏运动T恤', '安踏速干运动T恤', 149.00, 200, 30, 0.20),
('P004', 'M002', '安踏运动裤', '安踏弹力运动长裤', 199.00, 80, 15, 0.35),
('P005', 'M003', '耐克足球鞋', '耐克刺客系列足球鞋', 899.00, 60, 10, 0.30),
('P006', 'M003', '耐克运动背包', '耐克大容量运动背包', 349.00, 40, 10, 0.80),
('P007', 'M004', '阿迪达斯羽毛球拍', '阿迪达斯碳素羽毛球拍', 599.00, 30, 5, 0.09),
('P008', 'M004', '阿迪达斯瑜伽垫', '阿迪达斯TPE环保瑜伽垫', 259.00, 120, 20, 1.50);

INSERT INTO customer (customer_code, customer_name, contact_name, address, postal_code, phone, email) VALUES
('C001', '阳光体育用品店', '刘经理', '广州市天河区体育西路100号', '510620', '020-38889999', 'sun@sports.com'),
('C002', '飞跃运动商城', '陈经理', '深圳市南山区科技园路50号', '518057', '0755-26886666', 'fly@sports.com'),
('C003', '健身体育批发', '周经理', '成都市武侯区人民南路200号', '610041', '028-85557777', 'fit@sports.com');
