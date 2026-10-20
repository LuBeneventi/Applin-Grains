package com.pedidos360.catalog.service;

import com.pedidos360.catalog.dto.ProductCreateDto;
import com.pedidos360.catalog.dto.ProductResponseDto;
import com.pedidos360.catalog.dto.ProductUpdateDto;

import java.util.List;

public interface ProductService {

    ProductResponseDto createProduct(ProductCreateDto dto);

    List<ProductResponseDto> getAllProducts();

    ProductResponseDto updateProduct(Long id, ProductUpdateDto dto);

    ProductResponseDto getProductById(Long id);

    ProductResponseDto reduceStock(Long id, Integer quantity);

    void deleteProduct(Long id);
}
