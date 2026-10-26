package cl.duoc.api;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClient;

/** Edición y baja de productos (PUT/DELETE del catálogo). SecurityConfig ya exige rol Admin en /api/catalog/** salvo GET. */
@RestController
public class AdminCatalogController {

    private final RestClient catalog;

    public AdminCatalogController(@Qualifier("catalogServiceClient") RestClient catalog) {
        this.catalog = catalog;
    }

    @PutMapping(value = "/api/catalog/products/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> updateProduct(@PathVariable Long id, @RequestBody String body) {
        return catalog.put()
                .uri("/api/catalog/products/" + id)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .toEntity(String.class);
    }

    @DeleteMapping("/api/catalog/products/{id}")
    public ResponseEntity<Void> deleteProduct(@PathVariable Long id) {
        catalog.delete().uri("/api/catalog/products/" + id).retrieve().toBodilessEntity();
        return ResponseEntity.noContent().build();
    }
}
