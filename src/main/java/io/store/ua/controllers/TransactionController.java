package io.store.ua.controllers;

import io.store.ua.models.dto.TransactionDTO;
import io.store.ua.service.TransactionService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigInteger;
import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/v1/transactions")
@RequiredArgsConstructor
public class TransactionController {
    private final TransactionService transactionService;

    @GetMapping("/findAll")
    public ResponseEntity<?> findAll(@RequestParam(name = "pageSize") int pageSize,
                                     @RequestParam(name = "page") int page) {
        return ResponseEntity.ok(transactionService.findAll(pageSize, page));
    }

    @GetMapping("/findBy")
    public ResponseEntity<?> findBy(@RequestParam(name = "transactionId", required = false) String transactionId,
                                    @RequestParam(name = "reference", required = false) String reference,
                                    @RequestParam(name = "currency", required = false) String currency,
                                    @RequestParam(name = "minAmount", required = false) BigInteger minAmount,
                                    @RequestParam(name = "maxAmount", required = false) BigInteger maxAmount,
                                    @RequestParam(name = "createdFrom", required = false)
                                    @DateTimeFormat(pattern = "dd-MM-yyyy'At'HH:mm:ss") LocalDateTime createdFrom,
                                    @RequestParam(name = "createdTo", required = false)
                                    @DateTimeFormat(pattern = "dd-MM-yyyy'At'HH:mm:ss") LocalDateTime createdTo,
                                    @RequestParam(name = "paidFrom", required = false)
                                    @DateTimeFormat(pattern = "dd-MM-yyyy'At'HH:mm:ss") LocalDateTime paidFrom,
                                    @RequestParam(name = "paidTo", required = false)
                                    @DateTimeFormat(pattern = "dd-MM-yyyy'At'HH:mm:ss") LocalDateTime paidTo,
                                    @RequestParam(name = "flowType", required = false) String flowType,
                                    @RequestParam(name = "purpose", required = false) String purpose,
                                    @RequestParam(name = "status", required = false) String status,
                                    @RequestParam(name = "beneficiaryId", required = false) Long beneficiaryId,
                                    @RequestParam(name = "paymentProvider", required = false) String paymentProvider,
                                    @RequestParam(name = "pageSize") int pageSize,
                                    @RequestParam(name = "page") int page) {
        return ResponseEntity.ok(transactionService.findBy(transactionId,
                reference,
                currency,
                minAmount,
                maxAmount,
                createdFrom,
                createdTo,
                paidFrom,
                paidTo,
                flowType,
                purpose,
                status,
                beneficiaryId,
                paymentProvider,
                pageSize,
                page));
    }

    @PostMapping("/incoming/initiate")
    public ResponseEntity<?> initiateIncoming(@RequestBody TransactionDTO transactionDTO,
                                              @RequestParam(name = "autoSettle", required = false) Boolean autoSettle) {
        return ResponseEntity.ok(transactionService.initiateIncomingPayment(transactionDTO, autoSettle));
    }

    @PostMapping("/outgoing/initiate")
    public ResponseEntity<?> initiateOutgoing(@RequestBody TransactionDTO transactionDTO,
                                              @RequestParam(name = "autoSettle", required = false) Boolean autoSettle) {
        return ResponseEntity.ok(transactionService.initiateOutcomingPayment(transactionDTO, autoSettle));
    }

    @PostMapping("/settle")
    public ResponseEntity<?> settle(@RequestBody TransactionDTO transactionDTO) {
        return ResponseEntity.ok(transactionService.settlePayment(transactionDTO));
    }

    @PostMapping("/cancel")
    public ResponseEntity<?> cancel(@RequestBody TransactionDTO transactionDTO) {
        return ResponseEntity.ok(transactionService.cancelPayment(transactionDTO));
    }
}
