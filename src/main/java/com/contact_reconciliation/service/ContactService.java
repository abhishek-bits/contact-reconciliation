package com.contact_reconciliation.service;

import com.contact_reconciliation.dto.ContactResponseDto;
import org.springframework.stereotype.Service;

@Service
public interface ContactService {
    public ContactResponseDto getLinkedContacts(String email, String phoneNumber);
}
