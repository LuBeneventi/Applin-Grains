-- ==============================================================================
-- SEED DE DATOS DE PRUEBA: TIENDA DE CAFÉ DE ESPECIALIDAD (PEDIDOS360)
-- Base de datos: pedidos360_catalog
-- Tabla: products
-- Moneda: CLP (Pesos Chilenos)
-- ==============================================================================

-- Conectarse a la base de datos de catálogo si se ejecuta desde psql:
-- \c pedidos360_catalog;

INSERT INTO products (name, description, price, stock, image_url) VALUES
('Espresso Doble (60ml)', 'Extracción intensa con granos de origen colombiano, crema dorada y notas a chocolate amargo.', 2500.00, 100, 'https://images.unsplash.com/photo-1510591509098-f4fdc6d0ff04?w=500'),
('Café Latte (300ml)', 'Doble espresso con leche vaporizada y suave microespuma de leche entera o vegetal.', 3400.00, 80, 'https://images.unsplash.com/photo-1561047029-3000c68339ca?w=500'),
('Capuccino Italiano (200ml)', 'Proporción perfecta de espresso, leche vaporizada y densa espuma, con toque de cacao en polvo.', 3200.00, 90, 'https://images.unsplash.com/photo-1572442388796-11668a67e53d?w=500'),
('Flat White (180ml)', 'Ristretto doble con microespuma sedosa, sabor concentrado a café con textura aterciopelada.', 3500.00, 70, 'https://images.unsplash.com/photo-1577968897966-3d4325b36b61?w=500'),
('Cold Brew Nitro (350ml)', 'Macerado en frío por 18 horas e infusionado con nitrógeno para una textura cremosa y refrescante.', 3900.00, 50, 'https://images.unsplash.com/photo-1517701550927-30cf4ba1dba5?w=500'),
('Café en Grano Geisha Panamá (250g)', 'Café de especialidad de altura (1.800 msnm), proceso lavado, notas florales de jazmín y bergamota.', 18990.00, 30, 'https://images.unsplash.com/photo-1587734195503-904fca47e0e9?w=500'),
('Café en Grano Etiopía Yirgacheffe (250g)', 'Tueste medio-claro, proceso natural, cuerpo sedoso con notas a arándanos frescos y miel silvestre.', 14500.00, 40, 'https://images.unsplash.com/photo-1559056199-641a0ac8b55e?w=500'),
('Café en Grano Colombia Huila (250g)', 'Tueste medio, proceso lavado, balance clásico con notas a caramelo, avellana tostada y manzana roja.', 11990.00, 60, 'https://images.unsplash.com/photo-1611854779393-1b2da9d400fe?w=500'),
('Cafetera Prensa Francesa Timemore 450ml', 'Vidrio borosilicato resistente al calor y filtro de acero inoxidable de malla fina para extracción limpia.', 22990.00, 15, 'https://images.unsplash.com/photo-1544787219-7f47ccb76574?w=500'),
('Cafetera V60 de Cerámica Blanca Hario 02', 'Método cónico de goteo manual para resaltar la claridad y notas florales del café de especialidad.', 24990.00, 20, 'https://images.unsplash.com/photo-1514432324607-a09d9b4aefdd?w=500'),
('Croissant Artesanal de Mantequilla', 'Hojaldre francés tradicional elaborado con mantequilla europea, crujiente y horneado diariamente.', 2800.00, 45, 'https://images.unsplash.com/photo-1555507036-ab1f4038808a?w=500'),
('Cookie Chunky con Chocolate Belga 70%', 'Galleta estilo neoyorquina suave por dentro, con generosos trozos de chocolate bitter belga y flor de sal.', 2400.00, 50, 'https://images.unsplash.com/photo-1499636136210-6f4ee915583e?w=500')
ON CONFLICT DO NOTHING;
