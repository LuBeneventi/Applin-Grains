package com.pedidos360.catalog.config;

import com.pedidos360.catalog.entity.Product;
import com.pedidos360.catalog.repository.ProductRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;
import java.util.List;

/**
 * Inicializador automático de datos de prueba para la tienda de café Pedidos360.
 * Si el catálogo se encuentra vacío al arrancar, inserta el menú base de cafetería de especialidad.
 */
@Configuration
public class DataInitializer {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    @Bean
    public CommandLineRunner initCoffeeCatalog(ProductRepository productRepository) {
        return args -> {
            if (productRepository.count() == 0) {
                log.info("Catálogo vacío detectado. Insertando menú de prueba de cafetería de especialidad...");

                List<Product> coffeeMenu = List.of(
                    Product.builder()
                        .name("Espresso Doble (60ml)")
                        .description("Extracción intensa con granos de origen colombiano, crema dorada y notas a chocolate amargo.")
                        .price(new BigDecimal("2500"))
                        .stock(100)
                        .imageUrl("https://images.unsplash.com/photo-1510591509098-f4fdc6d0ff04?w=500")
                        .build(),

                    Product.builder()
                        .name("Café Latte (300ml)")
                        .description("Doble espresso con leche vaporizada y suave microespuma de leche entera o vegetal.")
                        .price(new BigDecimal("3400"))
                        .stock(80)
                        .imageUrl("https://images.unsplash.com/photo-1561047029-3000c68339ca?w=500")
                        .build(),

                    Product.builder()
                        .name("Capuccino Italiano (200ml)")
                        .description("Proporción perfecta de espresso, leche vaporizada y densa espuma, con toque de cacao en polvo.")
                        .price(new BigDecimal("3200"))
                        .stock(90)
                        .imageUrl("https://images.unsplash.com/photo-1572442388796-11668a67e53d?w=500")
                        .build(),

                    Product.builder()
                        .name("Flat White (180ml)")
                        .description("Ristretto doble con microespuma sedosa, sabor concentrado a café con textura aterciopelada.")
                        .price(new BigDecimal("3500"))
                        .stock(70)
                        .imageUrl("https://images.unsplash.com/photo-1577968897966-3d4325b36b61?w=500")
                        .build(),

                    Product.builder()
                        .name("Cold Brew Nitro (350ml)")
                        .description("Macerado en frío por 18 horas e infusionado con nitrógeno para una textura cremosa y refrescante.")
                        .price(new BigDecimal("3900"))
                        .stock(50)
                        .imageUrl("https://images.unsplash.com/photo-1517701550927-30cf4ba1dba5?w=500")
                        .build(),

                    Product.builder()
                        .name("Café en Grano Geisha Panamá (250g)")
                        .description("Café de especialidad de altura (1.800 msnm), proceso lavado, notas florales de jazmín y bergamota.")
                        .price(new BigDecimal("18990"))
                        .stock(30)
                        .imageUrl("https://images.unsplash.com/photo-1587734195503-904fca47e0e9?w=500")
                        .build(),

                    Product.builder()
                        .name("Café en Grano Etiopía Yirgacheffe (250g)")
                        .description("Tueste medio-claro, proceso natural, cuerpo sedoso con notas a arándanos frescos y miel silvestre.")
                        .price(new BigDecimal("14500"))
                        .stock(40)
                        .imageUrl("https://images.unsplash.com/photo-1559056199-641a0ac8b55e?w=500")
                        .build(),

                    Product.builder()
                        .name("Café en Grano Colombia Huila (250g)")
                        .description("Tueste medio, proceso lavado, balance clásico con notas a caramelo, avellana tostada y manzana roja.")
                        .price(new BigDecimal("11990"))
                        .stock(60)
                        .imageUrl("https://images.unsplash.com/photo-1611854779393-1b2da9d400fe?w=500")
                        .build(),

                    Product.builder()
                        .name("Cafetera Prensa Francesa Timemore 450ml")
                        .description("Vidrio borosilicato resistente al calor y filtro de acero inoxidable de malla fina para extracción limpia.")
                        .price(new BigDecimal("22990"))
                        .stock(15)
                        .imageUrl("https://images.unsplash.com/photo-1544787219-7f47ccb76574?w=500")
                        .build(),

                    Product.builder()
                        .name("Cafetera V60 de Cerámica Blanca Hario 02")
                        .description("Método cónico de goteo manual para resaltar la claridad y notas florales del café de especialidad.")
                        .price(new BigDecimal("24990"))
                        .stock(20)
                        .imageUrl("https://images.unsplash.com/photo-1514432324607-a09d9b4aefdd?w=500")
                        .build(),

                    Product.builder()
                        .name("Croissant Artesanal de Mantequilla")
                        .description("Hojaldre francés tradicional elaborado con mantequilla europea, crujiente y horneado diariamente.")
                        .price(new BigDecimal("2800"))
                        .stock(45)
                        .imageUrl("https://images.unsplash.com/photo-1555507036-ab1f4038808a?w=500")
                        .build(),

                    Product.builder()
                        .name("Cookie Chunky con Chocolate Belga 70%")
                        .description("Galleta estilo neoyorquina suave por dentro, con generosos trozos de chocolate bitter belga y flor de sal.")
                        .price(new BigDecimal("2400"))
                        .stock(50)
                        .imageUrl("https://images.unsplash.com/photo-1499636136210-6f4ee915583e?w=500")
                        .build()
                );

                productRepository.saveAll(coffeeMenu);
                log.info("¡Menú de café cargado exitosamente! Total productos iniciales: {}", coffeeMenu.size());
            } else {
                log.info("El catálogo ya contiene {} productos. Se omite carga inicial.", productRepository.count());
            }
        };
    }
}
