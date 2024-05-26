package com.contact_reconciliation.service.impl;

import com.contact_reconciliation.dto.ContactResponseDto;
import com.contact_reconciliation.service.ContactService;
import org.springframework.stereotype.Service;

@Service
public class ContactServiceImpl implements ContactService {
    @Override
    public ContactResponseDto getLinkedContacts(String email, String phoneNumber) {
        return null;
    }
}
