TRUNCATE TABLE
    stock_item_history,
    transactions,
    shipments,
    beneficiaries,
    stock_items,
    storage_sections,
    stock_item_groups,
    product_tags,
    product_photos,
    products,
    warehouses,
    tags
    RESTART IDENTITY CASCADE;
-- ==========================================
-- 1. INDEPENDENT ENTITIES (Tags, Groups, Beneficiaries)
-- ==========================================

-- Tags
INSERT INTO tags (name, is_active)
VALUES ('Niche Fragrances', true),
       ('Apple Ecosystem', true),
       ('High Performance', true);

-- Stock Item Groups
INSERT INTO stock_item_groups (code, name, is_active)
VALUES ('GRP-FRAG-01', 'Luxury Perfumes', true),
       ('GRP-TECH-01', 'Laptops & Tablets', true);

-- Beneficiaries (Real-life example: A supplier and a logistics partner)
INSERT INTO beneficiaries (iban, swift, name, card, is_active)
VALUES ('UA123052990000026003333333333', 'PBNKUA2X', 'Tech Distro LLC', '4441114444444441', true),
       ('UA983220010000026009999999999', 'SAVEUA2X', 'Global Logistics UA', '5168757500000001', true);

-- Products (High-end items fitting your interests)
INSERT INTO products (code, title, description, price, currency, weight, length, width, height)
VALUES ('PRD-AVENTUS', 'Creed Aventus 100ml', 'The legendary chypre fruity fragrance for men.', 1650000, 'UAH', 450, 10,
        10, 15),
       ('PRD-MAC-M3', 'MacBook Pro 16 M3', 'Space Black, 16GB Unified Memory, 512GB SSD.', 11599900, 'UAH', 2100, 35,
        24, 2);


-- ==========================================
-- 2. DEPENDENT ENTITIES (Warehouses, Mapping)
-- ==========================================

-- Warehouses (Kyiv and Lviv hubs)
-- Requires at least one User to exist. We fetch the first one found.
INSERT INTO warehouses (code, name, manager_id, is_active, phones, address, working_hours)
VALUES ('WHS-KIEV-01',
        'Kyiv Main Hub',
        (SELECT id FROM users ORDER BY id LIMIT 1),
        true,
        ARRAY ['+380441234567', '+380670000001'],
           -- JSON matching Address.java
        '{
          "building": "101",
          "street": "Velyka Vasylkivska",
          "city": "Kyiv",
          "state": "Kyiv City",
          "country": "UA",
          "postalCode": "01004",
          "latitude": 50.4265,
          "longitude": 30.5201
        }'::json,
           -- JSON matching WorkingHours.java
        '{
          "timezone": "Europe/Kiev",
          "days": [
            {
              "day": "MONDAY",
              "open": [
                {
                  "from": "09:00",
                  "to": "18:00"
                }
              ]
            },
            {
              "day": "TUESDAY",
              "open": [
                {
                  "from": "09:00",
                  "to": "18:00"
                }
              ]
            }
          ]
        }'::json),
       ('WHS-LVIV-01',
        'Lviv West Branch',
        (SELECT id FROM users ORDER BY id LIMIT 1),
        true,
        ARRAY ['+380321234567'],
        '{
          "building": "10",
          "street": "Horodotska",
          "city": "Lviv",
          "state": "Lviv Oblast",
          "country": "UA",
          "postalCode": "79000",
          "latitude": 49.8397,
          "longitude": 24.0297
        }'::json,
        '{
          "timezone": "Europe/Kiev",
          "days": [
            {
              "day": "MONDAY",
              "open": [
                {
                  "from": "10:00",
                  "to": "19:00"
                }
              ]
            }
          ]
        }'::json);

-- Storage Sections (Aisles within warehouses)
INSERT INTO storage_sections (warehouse_id, code, is_active)
VALUES ((SELECT id FROM warehouses WHERE code = 'WHS-KIEV-01'), 'SEC-FRAG-A', true),
       ((SELECT id FROM warehouses WHERE code = 'WHS-KIEV-01'), 'SEC-TECH-B', true),
       ((SELECT id FROM warehouses WHERE code = 'WHS-LVIV-01'), 'SEC-GEN-A', true);

-- Link Products to Tags
INSERT INTO product_tags (product_id, tag_id)
VALUES ((SELECT id FROM products WHERE code = 'PRD-AVENTUS'), (SELECT id FROM tags WHERE name = 'Niche Fragrances')),
       ((SELECT id FROM products WHERE code = 'PRD-MAC-M3'), (SELECT id FROM tags WHERE name = 'Apple Ecosystem')),
       ((SELECT id FROM products WHERE code = 'PRD-MAC-M3'), (SELECT id FROM tags WHERE name = 'High Performance'));


-- ==========================================
-- 3. INVENTORY & LOGISTICS (Stock, Shipments)
-- ==========================================

-- Stock Items (Specific batches of products in warehouses)
INSERT INTO stock_items (batch_version, code, product_id, group_id, warehouse_id, storage_section_id,
                         expiry_date, available_quantity, status, is_active)
VALUES (20250101,
        'ITM-CRD-001',
        (SELECT id FROM products WHERE code = 'PRD-AVENTUS'),
        (SELECT id FROM stock_item_groups WHERE code = 'GRP-FRAG-01'),
        (SELECT id FROM warehouses WHERE code = 'WHS-KIEV-01'),
        (SELECT id FROM storage_sections WHERE code = 'SEC-FRAG-A'),
        '2030-12-31',
        50,
        'AVAILABLE', -- Matches StockItemStatus.java
        true),
       (20250102,
        'ITM-MAC-001',
        (SELECT id FROM products WHERE code = 'PRD-MAC-M3'),
        (SELECT id FROM stock_item_groups WHERE code = 'GRP-TECH-01'),
        (SELECT id FROM warehouses WHERE code = 'WHS-KIEV-01'),
        (SELECT id FROM storage_sections WHERE code = 'SEC-TECH-B'),
        NULL,
        10,
        'AVAILABLE',
        true);

-- Shipments (Incoming supply from external)
INSERT INTO shipments (code, warehouse_id_recipient, warehouse_id_sender, address,
                       stock_item_id, stock_item_quantity, initiator_id,
                       status, shipment_direction)
VALUES ('SHP-IN-001',
        (SELECT id FROM warehouses WHERE code = 'WHS-KIEV-01'),
        NULL, -- Incoming from external source
        NULL,
        (SELECT id FROM stock_items WHERE code = 'ITM-MAC-001'),
        5,
        (SELECT id FROM users ORDER BY id LIMIT 1),
        'DELIVERED', -- Matches ShipmentStatus.java
        'INCOMING' -- Matches ShipmentDirection.java
       );


-- ==========================================
-- 4. FINANCE (Transactions)
-- ==========================================

-- Transaction (Paying the supplier for the MacBooks)
INSERT INTO transactions (transaction_id, reference, flow_type, purpose, status,
                          amount, currency, beneficiary_id, payment_provider, external_references)
VALUES ('TRX-SUP-001',
        'REF-INV-999',
        'DEBIT', -- Matches TransactionFlowType.java
        'STOCK_INBOUND_COST', -- Matches TransactionPurpose.java
        'SETTLED', -- Matches TransactionStatus.java
        57999500, -- 5 MacBooks * Price (approx)
        'UAH', -- Matches Currency.java
        (SELECT id FROM beneficiaries WHERE name = 'Tech Distro LLC'),
        'LIQ_PAY', -- Matches PaymentProvider.java
           -- JSON matching ExternalReferences.java (flexible map structure)
        '{
          "transactionId": "liqpay_order_12345",
          "status": "success",
          "merchantId": "i987654321",
          "merchantName": "Tech Distro LLC",
          "authenticationCode": "3DS-OK",
          "country": "UA",
          "currency": "UAH",
          "type": "purchase",
          "reference": "REF-INV-999"
        }'::json);