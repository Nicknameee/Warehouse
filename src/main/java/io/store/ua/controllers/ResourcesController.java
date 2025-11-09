package io.store.ua.controllers;

import io.store.ua.enums.*;
import io.store.ua.utility.UserSecurityStrategyService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class ResourcesController {
    @GetMapping("/vars/userRoles")
    public ResponseEntity<?> userRoles() {
        return ResponseEntity.ok(UserRole.values());
    }

    @GetMapping("/vars/userStatuses")
    public ResponseEntity<?> userStatuses() {
        return ResponseEntity.ok(UserStatus.values());
    }

    @GetMapping("/vars/securityType")
    public ResponseEntity<?> securityType() {
        return ResponseEntity.ok(UserSecurityStrategyService.USER_AUTHENTICATION_TYPE);
    }

    @GetMapping("/vars/cardTypes")
    public ResponseEntity<?> cardTypes() {
        return ResponseEntity.ok(CardType.values());
    }

    @GetMapping("/vars/currencies")
    public ResponseEntity<?> currencies() {
        return ResponseEntity.ok(Currency.values());
    }

    @GetMapping("/vars/paymentProviders")
    public ResponseEntity<?> paymentProviders() {
        return ResponseEntity.ok(PaymentProvider.values());
    }

    @GetMapping("/vars/shipmentDirections")
    public ResponseEntity<?> shipmentDirections() {
        return ResponseEntity.ok(ShipmentDirection.values());
    }

    @GetMapping("/vars/shipmentStatuses")
    public ResponseEntity<?> shipmentStatuses() {
        return ResponseEntity.ok(ShipmentStatus.values());
    }

    @GetMapping("/vars/stockItemStatuses")
    public ResponseEntity<?> stockItemStatuses() {
        return ResponseEntity.ok(StockItemStatus.values());
    }

    @GetMapping("/vars/transactionFlowTypes")
    public ResponseEntity<?> transactionFlowTypes() {
        return ResponseEntity.ok(TransactionFlowType.values());
    }

    @GetMapping("/vars/transactionPurposes")
    public ResponseEntity<?> transactionPurposes() {
        return ResponseEntity.ok(TransactionPurpose.values());
    }

    @GetMapping("/vars/transactionStatuses")
    public ResponseEntity<?> transactionStatuses() {
        return ResponseEntity.ok(TransactionStatus.values());
    }
}
