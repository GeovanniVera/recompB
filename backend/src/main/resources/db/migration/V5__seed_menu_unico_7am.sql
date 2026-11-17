-- Real 3B products and the Gym 7:00 AM single-menu meals.
-- Same idempotency style as V4: every insert is guarded with NOT EXISTS, so
-- re-applying cannot duplicate rows.

-- ---------- food items ----------
INSERT INTO food_items (user_id, name, state_tag, kcal_per_100g, protein_per_100g, carbs_per_100g, fat_per_100g, quick_grams_json, created_at)
SELECT NULL, 'Fajitas de Pollo Campo Grill', 'Cocido', 165.00, 31.00, 0.00, 3.60, '[110,160,220,250]', UTC_TIMESTAMP(6)
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM food_items WHERE user_id IS NULL AND name = 'Fajitas de Pollo Campo Grill' AND state_tag = 'Cocido');

INSERT INTO food_items (user_id, name, state_tag, kcal_per_100g, protein_per_100g, carbs_per_100g, fat_per_100g, quick_grams_json, created_at)
SELECT NULL, 'Chuletas de Cerdo Campo Grill', 'Cocido', 245.00, 25.00, 0.00, 15.50, '[120,150,180,200]', UTC_TIMESTAMP(6)
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM food_items WHERE user_id IS NULL AND name = 'Chuletas de Cerdo Campo Grill' AND state_tag = 'Cocido');

INSERT INTO food_items (user_id, name, state_tag, kcal_per_100g, protein_per_100g, carbs_per_100g, fat_per_100g, quick_grams_json, created_at)
SELECT NULL, 'Atún Tuny en Agua', 'Drenado', 101.00, 24.20, 0.00, 0.50, '[100,130,140,200]', UTC_TIMESTAMP(6)
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM food_items WHERE user_id IS NULL AND name = 'Atún Tuny en Agua' AND state_tag = 'Drenado');

INSERT INTO food_items (user_id, name, state_tag, kcal_per_100g, protein_per_100g, carbs_per_100g, fat_per_100g, quick_grams_json, created_at)
SELECT NULL, 'Yogurt Griego Natural Yoplait', 'Directo', 74.00, 7.00, 7.10, 2.00, '[150,200,250,300]', UTC_TIMESTAMP(6)
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM food_items WHERE user_id IS NULL AND name = 'Yogurt Griego Natural Yoplait' AND state_tag = 'Directo');

INSERT INTO food_items (user_id, name, state_tag, kcal_per_100g, protein_per_100g, carbs_per_100g, fat_per_100g, quick_grams_json, created_at)
SELECT NULL, 'Arroz Súper Extra Campo Santo (Natural)', 'Cocido', 130.00, 2.70, 28.00, 0.30, '[150,200,250,300]', UTC_TIMESTAMP(6)
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM food_items WHERE user_id IS NULL AND name = 'Arroz Súper Extra Campo Santo (Natural)' AND state_tag = 'Cocido');

INSERT INTO food_items (user_id, name, state_tag, kcal_per_100g, protein_per_100g, carbs_per_100g, fat_per_100g, quick_grams_json, created_at)
SELECT NULL, 'Arroz Súper Extra Campo Santo (Tomate)', 'Cocido', 152.00, 3.00, 29.50, 2.30, '[150,200,250,300]', UTC_TIMESTAMP(6)
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM food_items WHERE user_id IS NULL AND name = 'Arroz Súper Extra Campo Santo (Tomate)' AND state_tag = 'Cocido');

INSERT INTO food_items (user_id, name, state_tag, kcal_per_100g, protein_per_100g, carbs_per_100g, fat_per_100g, quick_grams_json, created_at)
SELECT NULL, 'Verduras Congeladas Campo Grill', 'Cocido', 55.00, 2.50, 10.50, 0.40, '[100,150,200]', UTC_TIMESTAMP(6)
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM food_items WHERE user_id IS NULL AND name = 'Verduras Congeladas Campo Grill' AND state_tag = 'Cocido');

INSERT INTO food_items (user_id, name, state_tag, kcal_per_100g, protein_per_100g, carbs_per_100g, fat_per_100g, quick_grams_json, created_at)
SELECT NULL, 'Gelatina D''Gari Regular', 'Directo', 42.00, 1.70, 8.70, 0.00, '[100,150,200]', UTC_TIMESTAMP(6)
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM food_items WHERE user_id IS NULL AND name = 'Gelatina D''Gari Regular' AND state_tag = 'Directo');

INSERT INTO food_items (user_id, name, state_tag, kcal_per_100g, protein_per_100g, carbs_per_100g, fat_per_100g, quick_grams_json, created_at)
SELECT NULL, 'Aceite de Cocina', 'Directo', 884.00, 0.00, 0.00, 100.00, '[5,10,15]', UTC_TIMESTAMP(6)
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM food_items WHERE user_id IS NULL AND name = 'Aceite de Cocina' AND state_tag = 'Directo');

-- ---------- quick plates: Menú Único, gym 7:00 AM ----------
INSERT INTO quick_plates (user_id, title, subtitle, kcal, protein, carbs, fat, created_at)
SELECT NULL, '06:35 Pre-Entreno Gym 7AM', '40g Avena en hojuelas + Café solo', 152.00, 5.30, 27.10, 2.60, UTC_TIMESTAMP(6)
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM quick_plates WHERE user_id IS NULL AND title = '06:35 Pre-Entreno Gym 7AM');

INSERT INTO quick_plates (user_id, title, subtitle, kcal, protein, carbs, fat, created_at)
SELECT NULL, '08:30 Comida 1: Post-Entreno', '250g Yogurt Yoplait + 55g Avena + 150g Gelatina D''Gari', 456.00, 27.40, 68.10, 8.60, UTC_TIMESTAMP(6)
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM quick_plates WHERE user_id IS NULL AND title = '08:30 Comida 1: Post-Entreno');

INSERT INTO quick_plates (user_id, title, subtitle, kcal, protein, carbs, fat, created_at)
SELECT NULL, '14:30 Comida 2: Plato Fuerte Pollo', '220g Fajitas Campo Grill + 300g Arroz Tomate + 150g Verduras + 5g Aceite', 946.00, 81.00, 104.30, 20.90, UTC_TIMESTAMP(6)
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM quick_plates WHERE user_id IS NULL AND title = '14:30 Comida 2: Plato Fuerte Pollo');

INSERT INTO quick_plates (user_id, title, subtitle, kcal, protein, carbs, fat, created_at)
SELECT NULL, '20:30 Comida 3: Cena Recuperadora', '110g Fajitas Campo Grill + 250g Arroz Natural + 150g Verduras + 150g Gelatina', 652.00, 47.20, 98.80, 5.30, UTC_TIMESTAMP(6)
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM quick_plates WHERE user_id IS NULL AND title = '20:30 Comida 3: Cena Recuperadora');
