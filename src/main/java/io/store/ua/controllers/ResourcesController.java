package io.store.ua.controllers;

import io.store.ua.enums.*;
import io.store.ua.utility.UserSecurityStrategyService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;

@RestController
@RequiredArgsConstructor
public class ResourcesController {
    @GetMapping("/vars/userRoles")
    public ResponseEntity<?> userRoles() {
        return ResponseEntity.ok(Arrays.stream(UserRole.values()).toList());
    }

    @GetMapping("/vars/userStatuses")
    public ResponseEntity<?> userStatuses() {
        return ResponseEntity.ok(Arrays.stream(UserStatus.values()).toList());
    }

    @GetMapping("/vars/securityType")
    public ResponseEntity<?> securityType() {
        return ResponseEntity.ok(UserSecurityStrategyService.USER_AUTHENTICATION_TYPE);
    }

    @GetMapping("/vars/cardTypes")
    public ResponseEntity<?> cardTypes() {
        return ResponseEntity.ok(Arrays.stream(CardType.values()).toList());
    }

    @GetMapping("/vars/currencies")
    public ResponseEntity<?> currencies() {
        return ResponseEntity.ok(Arrays.stream(Currency.values()).toList());
    }

    @GetMapping("/vars/paymentProviders")
    public ResponseEntity<?> paymentProviders() {
        return ResponseEntity.ok(Arrays.stream(PaymentProvider.values()).toList());
    }

    @GetMapping("/vars/shipmentDirections")
    public ResponseEntity<?> shipmentDirections() {
        return ResponseEntity.ok(Arrays.stream(ShipmentDirection.values()).toList());
    }

    @GetMapping("/vars/shipmentStatuses")
    public ResponseEntity<?> shipmentStatuses() {
        return ResponseEntity.ok(Arrays.stream(ShipmentStatus.values()).toList());
    }

    @GetMapping("/vars/stockItemStatuses")
    public ResponseEntity<?> stockItemStatuses() {
        return ResponseEntity.ok(Arrays.stream(StockItemStatus.values()).toList());
    }

    @GetMapping("/vars/transactionFlowTypes")
    public ResponseEntity<?> transactionFlowTypes() {
        return ResponseEntity.ok(Arrays.stream(TransactionFlowType.values()).toList());
    }

    @GetMapping("/vars/transactionPurposes")
    public ResponseEntity<?> transactionPurposes() {
        return ResponseEntity.ok(Arrays.stream(TransactionPurpose.values()).toList());
    }

    @GetMapping("/vars/transactionStatuses")
    public ResponseEntity<?> transactionStatuses() {
        return ResponseEntity.ok(Arrays.stream(TransactionStatus.values()).toList());
    }
}
