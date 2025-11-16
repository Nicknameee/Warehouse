package io.store.ua.controllers;

import io.store.ua.enums.*;
import io.store.ua.utility.UserSecurityStrategyService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;

@RestController
@RequiredArgsConstructor
public class ResourcesController {
    @GetMapping("/vars/userRoles")
    public ResponseEntity<List<UserRole>> userRoles() {
        return ResponseEntity.ok(Arrays.stream(UserRole.values()).toList());
    }

    @GetMapping("/vars/userStatuses")
    public ResponseEntity<List<UserStatus>> userStatuses() {
        return ResponseEntity.ok(Arrays.stream(UserStatus.values()).toList());
    }

    @GetMapping("/vars/securityType")
    public ResponseEntity<String> securityType() {
        return ResponseEntity.ok(UserSecurityStrategyService.USER_AUTHENTICATION_TYPE);
    }

    @GetMapping("/vars/cardTypes")
    public ResponseEntity<List<CardType>> cardTypes() {
        return ResponseEntity.ok(Arrays.stream(CardType.values()).toList());
    }

    @GetMapping("/vars/currencies")
    public ResponseEntity<List<Currency>> currencies() {
        return ResponseEntity.ok(Arrays.stream(Currency.values()).toList());
    }

    @GetMapping("/vars/paymentProviders")
    public ResponseEntity<List<PaymentProvider>> paymentProviders() {
        return ResponseEntity.ok(Arrays.stream(PaymentProvider.values()).toList());
    }

    @GetMapping("/vars/shipmentDirections")
    public ResponseEntity<List<ShipmentDirection>> shipmentDirections() {
        return ResponseEntity.ok(Arrays.stream(ShipmentDirection.values()).toList());
    }

    @GetMapping("/vars/shipmentStatuses")
    public ResponseEntity<List<ShipmentStatus>> shipmentStatuses() {
        return ResponseEntity.ok(Arrays.stream(ShipmentStatus.values()).toList());
    }

    @GetMapping("/vars/stockItemStatuses")
    public ResponseEntity<List<StockItemStatus>> stockItemStatuses() {
        return ResponseEntity.ok(Arrays.stream(StockItemStatus.values()).toList());
    }

    @GetMapping("/vars/transactionFlowTypes")
    public ResponseEntity<List<TransactionFlowType>> transactionFlowTypes() {
        return ResponseEntity.ok(Arrays.stream(TransactionFlowType.values()).toList());
    }

    @GetMapping("/vars/transactionPurposes")
    public ResponseEntity<List<TransactionPurpose>> transactionPurposes() {
        return ResponseEntity.ok(Arrays.stream(TransactionPurpose.values()).toList());
    }

    @GetMapping("/vars/transactionStatuses")
    public ResponseEntity<List<TransactionStatus>> transactionStatuses() {
        return ResponseEntity.ok(Arrays.stream(TransactionStatus.values()).toList());
    }
}
