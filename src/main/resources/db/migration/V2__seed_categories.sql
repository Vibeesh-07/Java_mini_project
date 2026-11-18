-- V2__seed_categories.sql: Standard item categories for campus lost & found

INSERT INTO categories (name, description, icon) VALUES
('Electronics', 'Laptops, phones, chargers, calculators, headphones, USB drives', 'laptop'),
('Books & Stationery', 'Textbooks, notebooks, binders, pencil cases, calculators', 'book-open'),
('Wallets & Bags', 'Wallets, purses, backpacks, gym bags, clutches', 'briefcase'),
('Identity & Cards', 'Student IDs, driver licenses, keyscards, credit/debit cards', 'credit-card'),
('Keys & Access Cards', 'Dorm keys, car keys, bike lock keys, keychains, fobs', 'key'),
('Clothing & Accessories', 'Jackets, hoodies, caps, sunglasses, watches, jewelry', 'shirt'),
('Bottles & Tumblers', 'Water bottles, thermoses, travel mugs, lunchboxes', 'coffee'),
('Sports & Recreation', 'Sports equipment, gym accessories, musical instruments', 'activity'),
('Other Belongings', 'Miscellaneous personal belongings not listed above', 'package')
ON DUPLICATE KEY UPDATE name=name;
