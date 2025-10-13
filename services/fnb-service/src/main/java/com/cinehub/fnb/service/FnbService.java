package com.cinehub.fnb.service;

import com.cinehub.fnb.dto.request.FnbItemDto;
import com.cinehub.fnb.dto.request.FnbItemRequest;
import com.cinehub.fnb.dto.response.FnbCalculationResponse;
import com.cinehub.fnb.dto.response.FnbItemResponse;
import com.cinehub.fnb.entity.FnbItem;
import com.cinehub.fnb.repository.FnbItemRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
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

    // ---------------------------------------------------------------------
    // 1. LOGIC TÍNH TOÁN (CHO BOOKING SERVICE)
    // ---------------------------------------------------------------------

    /**
     * Tính tổng giá F&B dựa trên danh sách các mục và số lượng được chọn.
     * API này được Booking Service gọi đồng bộ.
     */
    public FnbCalculationResponse calculateTotalPrice(List<FnbItemDto> selectedFnbItems) {

        // Lấy tất cả các FnbItemId duy nhất
        Set<UUID> fnbIds = selectedFnbItems.stream()
                .map(FnbItemDto::getFnbItemId)
                .collect(Collectors.toSet());

        // Tra cứu tất cả các mục F&B trong một lần gọi DB
        List<FnbItem> fnbEntities = fnbItemRepository.findAllByIdIn(fnbIds.stream().toList());

        // Map ID sang Entity để dễ dàng tra cứu giá
        Map<UUID, FnbItem> fnbMap = fnbEntities.stream()
                .collect(Collectors.toMap(FnbItem::getId, item -> item));

        BigDecimal total = BigDecimal.ZERO;

        for (FnbItemDto itemDto : selectedFnbItems) {
            FnbItem fnbItem = fnbMap.get(itemDto.getFnbItemId());

            if (fnbItem == null) {
                log.warn("❌ F&B Item ID {} not found. Skipping calculation for this item.", itemDto.getFnbItemId());
                // Tùy chọn: Ném ngoại lệ thay vì bỏ qua
                continue;
            }

            // Tính tổng: đơn giá * số lượng
            BigDecimal itemTotal = fnbItem.getUnitPrice()
                    .multiply(new BigDecimal(itemDto.getQuantity()));
            total = total.add(itemTotal);
        }

        log.info("🍔 Total F&B price calculated: {}", total);
        return FnbCalculationResponse.builder()
                .totalFnbPrice(total)
                .build();
    }

    // ---------------------------------------------------------------------
    // 2. CRUD CHO ADMIN/STAFF
    // ---------------------------------------------------------------------

    /**
     * Lấy tất cả các mục F&B đang hoạt động.
     */
    public List<FnbItemResponse> getAllFnbItems() {
        return fnbItemRepository.findAll().stream()
                .map(this::mapToResponse)
                .toList();
    }

    /**
     * Tạo mới một mục F&B.
     */
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