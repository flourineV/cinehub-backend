package com.cinehub.pricing.service;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.cinehub.pricing.entity.Combo;
import com.cinehub.pricing.repository.ComboRepository;

@Service
public class ComboService {
    private final ComboRepository comboRepository;

    public ComboService(ComboRepository comboRepository) {
        this.comboRepository = comboRepository;
    }

    public List<Combo> findAll() {
        return comboRepository.findAll();
    }

    public Combo findById(UUID id) {
        return comboRepository.findById(id).orElse(null);
    }

    public Combo save(Combo combo) {
        return comboRepository.save(combo);
    }

    public Combo update(UUID id, Combo combo) {
        Combo existing = comboRepository.findById(id).orElse(null);
        if (existing != null) {
            existing.setComboName(combo.getComboName());
            existing.setPrice(combo.getPrice());
            existing.setDescription(combo.getDescription());
            return comboRepository.save(existing);
        }
        return null;
    }

    public void delete(UUID id) {
        comboRepository.deleteById(id);
    }
}