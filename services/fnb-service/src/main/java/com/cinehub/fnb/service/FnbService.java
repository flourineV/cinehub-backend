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
        // -------------------------------------------------------------------

        // Khai báo list mới để lưu chi tiết các mục đã tính toán
        List<CalculatedFnbItemDto> calculatedItems = new ArrayList<>();
        BigDecimal grandTotal = BigDecimal.ZERO; // Đổi tên biến total thành grandTotal để rõ ràng hơn

        for (FnbItemDto itemDto : selectedFnbItems) {
            FnbItem fnbItem = fnbMap.get(itemDto.getFnbItemId());

            if (fnbItem == null) {
                log.warn("❌ F&B Item ID {} not found. Skipping calculation for this item.", itemDto.getFnbItemId());
                // Tùy chọn: Ném ngoại lệ
                continue;
            }

            // 1. Tính toán giá trị: đơn giá * số lượng
            BigDecimal unitPrice = fnbItem.getUnitPrice();
            BigDecimal itemTotal = unitPrice
                    .multiply(new BigDecimal(itemDto.getQuantity()));

            // 2. Cộng vào tổng chung
            grandTotal = grandTotal.add(itemTotal);

            // 3. TẠO DTO CHI TIẾT ĐỂ TRẢ VỀ
            CalculatedFnbItemDto calculatedItem = CalculatedFnbItemDto.builder()
                    .fnbItemId(itemDto.getFnbItemId())
                    .quantity(itemDto.getQuantity())
                    .unitPrice(unitPrice)
                    .totalFnbItemPrice(itemTotal)
                    .build();

            calculatedItems.add(calculatedItem);
        }

        log.info("🍔 Total F&B price calculated: {}", grandTotal);

        // 4. TRẢ VỀ RESPONSE CUỐI CÙNG CÓ CẢ TỔNG VÀ CHI TIẾT
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
                    log.error("❌ F&B Item not found with ID: {}", id);
                    return new IllegalArgumentException("F&B Item not found with ID: " + id);
                });
    }

    @Transactional
    public FnbItemResponse createFnbItem(FnbItemRequest request) {
        // Tùy chọn: Kiểm tra trùng tên (nếu cần xử lý lỗi thân thiện hơn)

        FnbItem newItem = FnbItem.builder()
                .name(request.getName())
                .description(request.getDescription())
                .unitPrice(request.getUnitPrice())
                .build();

        FnbItem savedItem = fnbItemRepository.save(newItem);
        log.info("➕ Created F&B item: {}", savedItem.getName());
        return mapToResponse(savedItem);
    }

    /**
     * Cập nhật một mục F&B hiện có.
     */
    @Transactional
    public FnbItemResponse updateFnbItem(UUID id, FnbItemRequest request) {
        FnbItem existingItem = fnbItemRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("F&B Item not found with ID: " + id));

        existingItem.setName(request.getName());
        existingItem.setDescription(request.getDescription());
        existingItem.setUnitPrice(request.getUnitPrice());

        FnbItem updatedItem = fnbItemRepository.save(existingItem);
        log.info("🔄 Updated F&B item: {}", updatedItem.getName());
        return mapToResponse(updatedItem);
    }

    /**
     * Xóa một mục F&B.
     */
    @Transactional
    public void deleteFnbItem(UUID id) {
        if (!fnbItemRepository.existsById(id)) {
            throw new IllegalArgumentException("F&B Item not found with ID: " + id);
        }
        fnbItemRepository.deleteById(id);
        log.warn("🗑️ Deleted F&B item with ID: {}", id);
    }

    // --- Mapper Helpers ---

    private FnbItemResponse mapToResponse(FnbItem item) {
        return FnbItemResponse.builder()
                .id(item.getId())
                .name(item.getName())
                .description(item.getDescription())
                .unitPrice(item.getUnitPrice())
                .build();
    }
}