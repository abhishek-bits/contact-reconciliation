package com.contact_reconciliation.controller;

import com.contact_reconciliation.dto.ApiResponse;
import com.contact_reconciliation.dto.ContactRequestDto;
import com.contact_reconciliation.dto.ContactResponseDto;
import com.contact_reconciliation.service.ContactService;
import io.micrometer.common.util.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/identity")
public class ContactController {

    @Autowired
    private ContactService contactService;

    @PostMapping
    ResponseEntity<ApiResponse> getContactInfo(@RequestBody ContactRequestDto contactRequestDto) {
        if(StringUtils.isEmpty(contactRequestDto.getPhoneNumber()) && StringUtils.isEmpty(contactRequestDto.getEmail())) {
            return ResponseEntity.badRequest().build();
        }
        ContactResponseDto contactResponseDto = contactService.getLinkedContacts(contactRequestDto.getEmail(), contactRequestDto.getPhoneNumber());
        return ResponseEntity.ok(ApiResponse.builder().contact(contactResponseDto).build());
    }
}
