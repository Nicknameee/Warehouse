package io.store.ua.controllers;

import io.store.ua.entity.Beneficiary;
import io.store.ua.models.dto.BeneficiaryDTO;
import io.store.ua.service.BeneficiaryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/beneficiaries")
@RequiredArgsConstructor
public class BeneficiaryController {
    private final BeneficiaryService beneficiaryService;

    @GetMapping("/findBy")
    public ResponseEntity<List<Beneficiary>> findBy(@RequestParam(value = "IBANPrefix", required = false) String IBANPrefix,
                                                    @RequestParam(value = "SWIFTPrefix", required = false) String SWIFTPrefix,
                                                    @RequestParam(value = "cardPrefix", required = false) String cardPrefix,
                                                    @RequestParam(value = "namePart", required = false) String namePart,
                                                    @RequestParam(value = "isActive", required = false) Boolean isActive,
                                                    @RequestParam("pageSize") int pageSize,
                                                    @RequestParam("page") int page) {
        return ResponseEntity.ok(beneficiaryService.findBy(IBANPrefix, SWIFTPrefix, cardPrefix, namePart, isActive, pageSize, page));
    }

    @PostMapping
    public ResponseEntity<Beneficiary> save(@RequestBody BeneficiaryDTO beneficiaryDTO) {
        return ResponseEntity.ok(beneficiaryService.save(beneficiaryDTO));
    }

    @PutMapping
    public ResponseEntity<Beneficiary> update(@RequestBody BeneficiaryDTO beneficiaryDTO) {
        return ResponseEntity.ok(beneficiaryService.update(beneficiaryDTO));
    }

    @PutMapping("/changeState")
    public ResponseEntity<List<Beneficiary>> changeState(@RequestParam("beneficiaryID") List<Long> beneficiaryIDs,
                                                         @RequestParam("isActive") Boolean isActive) {
        return ResponseEntity.ok(beneficiaryService.changeState(beneficiaryIDs, isActive));
    }
}
