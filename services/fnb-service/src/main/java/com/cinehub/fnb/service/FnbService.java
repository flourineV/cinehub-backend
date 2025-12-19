package com.cinehub.fnb.service;

import com.cinehub.fnb.dto.request.FnbItemDto;
import com.cinehub.fnb.dto.request.FnbItemRequest;
import com.cinehub.fnb.dto.response.CalculatedFnbItemDto;
import com.cinehub.fnb.dto.response.FnbCalculationResponse;
import com.cinehub.fnb.dto.response.FnbItemResponse;
import com.cinehub.fnb.entity.FnbItem;
import com.cinehub.fnb.repository.FnbItemRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class FnbService {

    private final FnbItemRepository fnbItemRepository;

    public FnbCalculationResponse calculateTotalPrice(List<FnbItemDto> selectedFnbItems) {

        Set<UUID> fnbIds = selectedFnbItems.stream()
                .map(FnbItemDto::getFnbItemId)
                .collect(Collectors.toSet());

        List<FnbItem> fnbEntities = fnbItemRepository.findAllByIdIn(fnbIds.stream().toList());

        Map<UUID, FnbItem> fnbMap = fnbEntities.stream()
                .collect(Collectors.toMap(FnbItem::getId, item -> item));

        List<CalculatedFnbItemDto> calculatedItems = new ArrayList<>();
        BigDecimal grandTotal = BigDecimal.ZERO;

        for (FnbItemDto itemDto : selectedFnbItems) {
            FnbItem fnbItem = fnbMap.get(itemDto.getFnbItemId());

            if (fnbItem == null) {
                log.warn("F&B Item ID {} not found. Skipping calculation for this item.", itemDto.getFnbItemId());
                continue;
            }

            BigDecimal unitPrice = fnbItem.getUnitPrice();
            BigDecimal itemTotal = unitPrice
                    .multiply(new BigDecimal(itemDto.getQuantity()));

            grandTotal = grandTotal.add(itemTotal);

            CalculatedFnbItemDto calculatedItem = CalculatedFnbItemDto.builder()
                    .fnbItemId(itemDto.getFnbItemId())
                    .quantity(itemDto.getQuantity())
                    .unitPrice(unitPrice)
                    .totalFnbItemPrice(itemTotal)
                    .build();

            calculatedItems.add(calculatedItem);
        }

        log.info("Total F&B price calculated: {}", grandTotal);

        return FnbCalculationResponse.builder()
                .totalFnbPrice(grandTotal)
                .calculatedFnbItems(calculatedItems)
                .build();
    }

    public List<FnbItemResponse> getAllFnbItems() {
        return fnbItemRepository.findAll().stream()
                .map(this::mapToResponse)
                .toList();
    }

    public FnbItemResponse getFnbItemById(UUID id) {
        return fnbItemRepository.findById(id)
                .map(this::mapToResponse)
                .orElseThrow(() -> {
                    log.error("F&B Item not found with ID: {}", id);
                    return new IllegalArgumentException("F&B Item not found with ID: " + id);
                });
    }

    @Transactional
    public FnbItemResponse createFnbItem(FnbItemRequest request) {

        if (fnbItemRepository.existsByName(request.getName())) {
            throw new IllegalArgumentException("F&B Item with name '" + request.getName() + "' already exists.");
        }

        FnbItem newItem = FnbItem.builder()
                .name(request.getName())
                .nameEn(request.getNameEn())
                .description(request.getDescription())
                .descriptionEn(request.getDescriptionEn())
                .unitPrice(request.getUnitPrice())
                .imageUrl(request.getImageUrl())
                .build();

        FnbItem savedItem = fnbItemRepository.save(newItem);
        log.info("➕ Created F&B item: {}", savedItem.getName());
        return mapToResponse(savedItem);
    }

    @Transactional
    public FnbItemResponse updateFnbItem(UUID id, FnbItemRequest request) {
        FnbItem existingItem = fnbItemRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("F&B Item not found with ID: " + id));

        existingItem.setName(request.getName());
        existingItem.setNameEn(request.getNameEn());
        existingItem.setDescription(request.getDescription());
        existingItem.setDescriptionEn(request.getDescriptionEn());
        existingItem.setUnitPrice(request.getUnitPrice());
        existingItem.setImageUrl(request.getImageUrl());

        FnbItem updatedItem = fnbItemRepository.save(existingItem);
        log.info("Updated F&B item: {}", updatedItem.getName());
        return mapToResponse(updatedItem);
    }

    @Transactional
    public void deleteFnbItem(UUID id) {
        if (!fnbItemRepository.existsById(id)) {
            throw new IllegalArgumentException("F&B Item not found with ID: " + id);
        }
        fnbItemRepository.deleteById(id);
        log.warn("Deleted F&B item with ID: {}", id);
    }

    private FnbItemResponse mapToResponse(FnbItem item) {
        return FnbItemResponse.builder()
                .id(item.getId())
                .name(item.getName())
                .nameEn(item.getNameEn())
                .description(item.getDescription())
                .descriptionEn(item.getDescriptionEn())
                .unitPrice(item.getUnitPrice())
                .imageUrl(item.getImageUrl())
                .build();
    }
}