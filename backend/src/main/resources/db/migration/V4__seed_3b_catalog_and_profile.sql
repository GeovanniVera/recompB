-- 3B catalog (user_id NULL = global) + initial profile for the seed user.
-- Every insert is guarded so re-applying this migration on a partially seeded
-- database cannot duplicate rows.

-- ---------- food items ----------
INSERT INTO food_items (user_id, name, state_tag, kcal_per_100g, protein_per_100g, carbs_per_100g, fat_per_100g, quick_grams_json, created_at)
SELECT NULL, 'Pechuga/Fajitas Pollo 3B', 'Cocido', 165.00, 31.00, 0.00, 3.60, '[100,150,200]', UTC_TIMESTAMP(6)
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM food_items WHERE user_id IS NULL AND name = 'Pechuga/Fajitas Pollo 3B' AND state_tag = 'Cocido');

INSERT INTO food_items (user_id, name, state_tag, kcal_per_100g, protein_per_100g, carbs_per_100g, fat_per_100g, quick_grams_json, created_at)
SELECT NULL, 'Pechuga/Fajitas Pollo 3B', 'Crudo', 120.00, 22.50, 0.00, 2.60, '[100,150,200]', UTC_TIMESTAMP(6)
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM food_items WHERE user_id IS NULL AND name = 'Pechuga/Fajitas Pollo 3B' AND state_tag = 'Crudo');

INSERT INTO food_items (user_id, name, state_tag, kcal_per_100g, protein_per_100g, carbs_per_100g, fat_per_100g, quick_grams_json, created_at)
SELECT NULL, 'Arroz Blanco Cocido', 'Cocido', 130.00, 2.70, 28.00, 0.30, '[100,150,200,250]', UTC_TIMESTAMP(6)
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM food_items WHERE user_id IS NULL AND name = 'Arroz Blanco Cocido' AND state_tag = 'Cocido');

INSERT INTO food_items (user_id, name, state_tag, kcal_per_100g, protein_per_100g, carbs_per_100g, fat_per_100g, quick_grams_json, created_at)
SELECT NULL, 'Verduras Congeladas 3B', 'Cocido', 38.00, 2.50, 7.20, 0.40, '[100,150,200]', UTC_TIMESTAMP(6)
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM food_items WHERE user_id IS NULL AND name = 'Verduras Congeladas 3B' AND state_tag = 'Cocido');

INSERT INTO food_items (user_id, name, state_tag, kcal_per_100g, protein_per_100g, carbs_per_100g, fat_per_100g, quick_grams_json, created_at)
SELECT NULL, 'Atún en Agua', 'Drenado', 108.00, 24.00, 0.00, 1.00, '[100,120]', UTC_TIMESTAMP(6)
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM food_items WHERE user_id IS NULL AND name = 'Atún en Agua' AND state_tag = 'Drenado');

INSERT INTO food_items (user_id, name, state_tag, kcal_per_100g, protein_per_100g, carbs_per_100g, fat_per_100g, quick_grams_json, created_at)
SELECT NULL, 'Huevo Entero', 'Pieza', 144.00, 12.60, 0.80, 9.60, '[1,2,3]', UTC_TIMESTAMP(6)
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM food_items WHERE user_id IS NULL AND name = 'Huevo Entero' AND state_tag = 'Pieza');

INSERT INTO food_items (user_id, name, state_tag, kcal_per_100g, protein_per_100g, carbs_per_100g, fat_per_100g, quick_grams_json, created_at)
SELECT NULL, 'Tortilla de Maíz', 'Pieza', 213.00, 4.70, 45.00, 2.00, '[1,2,3]', UTC_TIMESTAMP(6)
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM food_items WHERE user_id IS NULL AND name = 'Tortilla de Maíz' AND state_tag = 'Pieza');

INSERT INTO food_items (user_id, name, state_tag, kcal_per_100g, protein_per_100g, carbs_per_100g, fat_per_100g, quick_grams_json, created_at)
SELECT NULL, 'Avena en Hojuelas 3B', 'Crudo', 379.00, 13.20, 67.70, 6.50, '[40,60,80]', UTC_TIMESTAMP(6)
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM food_items WHERE user_id IS NULL AND name = 'Avena en Hojuelas 3B' AND state_tag = 'Crudo');

INSERT INTO food_items (user_id, name, state_tag, kcal_per_100g, protein_per_100g, carbs_per_100g, fat_per_100g, quick_grams_json, created_at)
SELECT NULL, 'Frijoles Cocidos', 'Cocido', 127.00, 8.70, 22.80, 0.50, '[100,150,200]', UTC_TIMESTAMP(6)
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM food_items WHERE user_id IS NULL AND name = 'Frijoles Cocidos' AND state_tag = 'Cocido');

INSERT INTO food_items (user_id, name, state_tag, kcal_per_100g, protein_per_100g, carbs_per_100g, fat_per_100g, quick_grams_json, created_at)
SELECT NULL, 'Crema de Cacahuate 3B', 'Directo', 588.00, 25.00, 20.00, 50.00, '[15,30]', UTC_TIMESTAMP(6)
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM food_items WHERE user_id IS NULL AND name = 'Crema de Cacahuate 3B' AND state_tag = 'Directo');

-- ---------- quick plates (user_id NULL = base plate) ----------
-- Plato Express 3B: 150 g pollo cocido + 200 g arroz + 150 g verduras.
INSERT INTO quick_plates (user_id, title, subtitle, kcal, protein, carbs, fat, created_at)
SELECT NULL, 'Plato Express 3B', '150 g pollo cocido + 200 g arroz + 150 g verduras', 565.00, 55.65, 66.80, 6.60, UTC_TIMESTAMP(6)
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM quick_plates WHERE user_id IS NULL AND title = 'Plato Express 3B');

-- Desayuno Base: 60 g avena + 2 huevos + 1 tortilla de maíz.
INSERT INTO quick_plates (user_id, title, subtitle, kcal, protein, carbs, fat, created_at)
SELECT NULL, 'Desayuno Base', '60 g avena + 2 huevos + 1 tortilla de maíz', 728.00, 37.82, 87.22, 25.10, UTC_TIMESTAMP(6)
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM quick_plates WHERE user_id IS NULL AND title = 'Desayuno Base');

-- Cena Rápida Atún: 120 g atún drenado + 1 tortilla + 100 g verduras.
INSERT INTO quick_plates (user_id, title, subtitle, kcal, protein, carbs, fat, created_at)
SELECT NULL, 'Cena Rápida Atún', '120 g atún drenado + 1 tortilla + 100 g verduras', 381.00, 36.00, 52.20, 3.60, UTC_TIMESTAMP(6)
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM quick_plates WHERE user_id IS NULL AND title = 'Cena Rápida Atún');

-- ---------- initial profile for the seed user ----------
INSERT INTO user_profiles (user_id, sex, age, height_cm, current_weight_kg, gym_days_per_week,
                           activity_factor, deficit_pct, protein_per_kg, fat_per_kg,
                           created_at, updated_at)
SELECT u.id, 'MALE', 25, 170.00, 70.00, 4, 1.48, 0.12, 2.00, 0.80, UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)
FROM app_users u
WHERE u.email = 'geovanni.vera23@gmail.com'
  AND NOT EXISTS (SELECT 1 FROM user_profiles p WHERE p.user_id = u.id);
