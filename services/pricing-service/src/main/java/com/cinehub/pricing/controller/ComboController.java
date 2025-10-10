package com.cinehub.pricing.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.web.bind.annotation.*;

import com.cinehub.pricing.entity.Combo;
import com.cinehub.pricing.service.ComboService;

@RestController
@RequestMapping("/api/pricing/combos")
public class ComboController {
    private final ComboService comboService;

    public ComboController(ComboService comboService) {
        this.comboService = comboService;
    }

    @GetMapping
    public List<Combo> getAll() {
        return comboService.findAll();
    }

    @GetMapping("/{id}")
    public Combo getById(@PathVariable UUID id) {
        return comboService.findById(id);
    }

    @PostMapping
    public Combo create(@RequestBody Combo combo) {
        return comboService.save(combo);
    }

    @PutMapping("/{id}")
    public Combo update(@PathVariable UUID id, @RequestBody Combo combo) {
        return comboService.update(id, combo);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable UUID id) {
        comboService.delete(id);
    }
}
