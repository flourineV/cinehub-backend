package com.cinehub.fnb.repository;

import com.cinehub.fnb.entity.FnbItem;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface FnbItemRepository extends JpaRepository<FnbItem, UUID> {

    List<FnbItem> findAllByIdIn(List<UUID> ids);

    boolean existsByName(String name);
}