package com.contact_reconciliation.dto;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Collection;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContactResponseDto implements Serializable {
    private Long primaryContactId;
    private Collection<String> emails;
    private Collection<String> phoneNumbers;
    private Collection<Long> secondaryContactIds;
}
