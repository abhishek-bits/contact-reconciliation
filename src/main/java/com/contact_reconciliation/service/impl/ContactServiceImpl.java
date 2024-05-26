package com.contact_reconciliation.service.impl;

import com.contact_reconciliation.dto.ContactResponseDto;
import com.contact_reconciliation.entity.Contact;
import com.contact_reconciliation.enums.LinkPrecedence;
import com.contact_reconciliation.repository.ContactRepository;
import com.contact_reconciliation.service.ContactService;
import io.micrometer.common.util.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Service
public class ContactServiceImpl implements ContactService {

    @Autowired
    private ContactRepository contactRepository;

    @Override
    public ContactResponseDto getLinkedContacts(String email, String phoneNumber) {
        if(StringUtils.isEmpty(email)) {

            Optional<Contact> contactOptional = contactRepository.findTopByPhoneNumberOrderByIdAsc(phoneNumber);

            if(contactOptional.isEmpty()) {

                // This is the first time this entry has arrived.
                Contact contact = contactRepository.save(
                        Contact.builder()
                                .phoneNumber(phoneNumber)
                                .linkPrecedence(LinkPrecedence.PRIMARY)
                                .createdAt(LocalDateTime.now())
                                .updatedAt(LocalDateTime.now())
                                .build());

                return getContactResponseDto(contact, null);

            } else {
                return getContactResponseDto(LinkPrecedence.PRIMARY.equals(contactOptional.get().getLinkPrecedence())
                                ? contactOptional.get()
                                : contactRepository.findById(contactOptional.get().getLinkedId()).get(),
                        null);
            }

        } else if(StringUtils.isEmpty(phoneNumber)) {

            Optional<Contact> contactOptional = contactRepository.findTopByEmailOrderByIdAsc(email);

            if(contactOptional.isEmpty()) {

                // This is the first time this entry has arrived
                Contact contact = contactRepository.save(
                        Contact.builder()
                                .email(email)
                                .linkPrecedence(LinkPrecedence.PRIMARY)
                                .createdAt(LocalDateTime.now())
                                .updatedAt(LocalDateTime.now())
                                .build());

                return getContactResponseDto(contact, null);

            } else {

                return getContactResponseDto(
                        LinkPrecedence.PRIMARY.equals(contactOptional.get().getLinkPrecedence())
                                ? contactOptional.get()
                                : contactRepository.findById(contactOptional.get().getLinkedId()).get(),
                        null);
            }

        } else {

            List<Contact> contactsWithEmail = contactRepository.findAllByEmail(email);
            List<Contact> contactsWithPhoneNumber = contactRepository.findAllByPhoneNumber(phoneNumber);

            if(CollectionUtils.isEmpty(contactsWithEmail) || CollectionUtils.isEmpty(contactsWithPhoneNumber)) {

                // This is the first time this entry has arrived
                Contact contact = contactRepository.save(
                        Contact.builder()
                                .email(email)
                                .phoneNumber(phoneNumber)
                                .linkPrecedence(LinkPrecedence.PRIMARY)
                                .createdAt(LocalDateTime.now())
                                .updatedAt(LocalDateTime.now())
                                .build());

                if(CollectionUtils.isEmpty(contactsWithEmail) && CollectionUtils.isEmpty(contactsWithPhoneNumber)) {
                    return getContactResponseDto(contact, null);
                }

                // We can now add this contact to either contactsWithEmail or contactsWithPhoneNumber
                // If those arrays are not empty then this contact will be updated appropriately.
                if(CollectionUtils.isEmpty(contactsWithEmail)) {
                    contactsWithPhoneNumber.add(contact);
                } else {
                    contactsWithEmail.add(contact);
                }
            }

            filterDuplicateContacts(contactsWithEmail, contactsWithPhoneNumber);

            // Find out the new primary contact for this case
            // This should be the contact with the lowest value of ID
            Contact primaryContact = getPrimaryContact(contactsWithEmail, contactsWithPhoneNumber);

            // Now, for all the other entries in the DB
            // Make all of them secondary and update linkedId to id of primaryContact.

            List<Contact> secondaryContacts = getSecondaryContacts(primaryContact, contactsWithEmail, contactsWithPhoneNumber);

            contactRepository.saveAll(secondaryContacts);

            return getContactResponseDto(primaryContact, secondaryContacts);
        }
    }

    private List<Contact> getSecondaryContacts(Contact primaryContact, List<Contact> contactsWithEmail, List<Contact> contactsWithPhoneNumber) {

        // There exists a set of contacts those were previously part of different equivalence classes.
        // They will now have to be integrated into the same equivalence class.

        Set<Long> otherPrimaryContactIds = new HashSet<>();

        contactsWithEmail.forEach(contact -> {
            if(null != contact.getLinkedId() && contact.getLinkedId() != primaryContact.getId()) {
                otherPrimaryContactIds.add(contact.getLinkedId());
            }
        });
        contactsWithPhoneNumber.forEach(contact -> {
            if(null != contact.getLinkedId() && contact.getLinkedId() != primaryContact.getId()) {
                otherPrimaryContactIds.add(contact.getLinkedId());
            }
        });

        List<Contact> thisSecondaryContacts = contactRepository.findAllByLinkedId(primaryContact.getId());
        List<Contact> otherPrimaryContacts = contactRepository.findAllByIdIn(otherPrimaryContactIds);
        List<Contact> otherSecondaryContacts = contactRepository.findAllByLinkedIdIn(otherPrimaryContactIds);

        Map<Long, Contact> contactIdToContactMap = getContactIdToContactMap(
                thisSecondaryContacts, otherPrimaryContacts, otherSecondaryContacts, contactsWithEmail, contactsWithPhoneNumber);

        List<Contact> secondaryContacts = new ArrayList<>();

        for(Map.Entry<Long, Contact> entry : contactIdToContactMap.entrySet()) {
            if(entry.getKey() == primaryContact.getId()) {
                continue;
            }
            Contact contact = entry.getValue();
            contact.setLinkPrecedence(LinkPrecedence.SECONDARY);
            contact.setLinkedId(primaryContact.getId());
            contact.setUpdatedAt(LocalDateTime.now());
            secondaryContacts.add(contact);
        }

        secondaryContacts.sort(Comparator.comparing(Contact::getId));

        return secondaryContacts;
    }

    private Map<Long, Contact> getContactIdToContactMap(
            List<Contact> thisSecondaryContacts,
            List<Contact> otherPrimaryContacts,
            List<Contact> otherSecondaryContacts,
            List<Contact> contactsWithEmail,
            List<Contact> contactsWithPhoneNumber) {
        Map<Long,Contact> map = new HashMap<>();
        thisSecondaryContacts.forEach(contact -> {
            map.putIfAbsent(contact.getId(), contact);
        });
        otherPrimaryContacts.forEach(contact -> {
            map.putIfAbsent(contact.getId(), contact);
        });
        otherSecondaryContacts.forEach(contact -> {
            map.putIfAbsent(contact.getId(), contact);
        });
        contactsWithEmail.forEach(contact -> {
            map.putIfAbsent(contact.getId(), contact);
        });
        contactsWithPhoneNumber.forEach(contact -> {
            map.putIfAbsent(contact.getId(), contact);
        });
        return map;
    }

    private void filterDuplicateContacts(List<Contact> contactsWithEmail, List<Contact> contactsWithPhoneNumber) {
        Set<Long> contactIds = new HashSet<>();
        contactsWithEmail.forEach(contact -> contactIds.add(contact.getId()));
        for(int i = 0; i < contactsWithPhoneNumber.size(); i++) {
            if(contactIds.contains(contactsWithPhoneNumber.get(i).getId())) {
                contactsWithPhoneNumber.remove(i);
                break;
            }
        }
    }

    private Contact getPrimaryContact(List<Contact> contactsWithEmail, List<Contact> contactsWithPhoneNumber) {
        Contact primaryContact;
        if(CollectionUtils.isEmpty(contactsWithEmail)) {
            primaryContact = contactsWithPhoneNumber.get(0);
            if(!LinkPrecedence.PRIMARY.equals(primaryContact.getLinkPrecedence())) {
                primaryContact = contactRepository.findById(primaryContact.getLinkedId()).get();
            }
        } else if(CollectionUtils.isEmpty(contactsWithPhoneNumber)) {
            primaryContact = contactsWithEmail.get(0);
            if(!LinkPrecedence.PRIMARY.equals(primaryContact.getLinkPrecedence())) {
                primaryContact = contactRepository.findById(primaryContact.getLinkedId()).get();
            }
        } else {
            // TODO: Improve this to single DB call.
            primaryContact = contactsWithEmail.get(0);
            if(null != contactsWithEmail.get(0).getLinkedId()) {
                primaryContact = contactRepository.findById(contactsWithEmail.get(0).getLinkedId()).get();
            }
            if(contactsWithPhoneNumber.get(0).getId() < primaryContact.getId()) {
                primaryContact = contactsWithPhoneNumber.get(0);
                if(null != contactsWithPhoneNumber.get(0).getLinkedId()) {
                    primaryContact = contactRepository.findById(contactsWithPhoneNumber.get(0).getLinkedId()).get();
                }
            }
        }
        return primaryContact;
    }

    private ContactResponseDto getContactResponseDto(Contact primaryContact, List<Contact> secondaryContacts) {

        if(CollectionUtils.isEmpty(secondaryContacts)) {
            secondaryContacts = contactRepository.findAllByLinkedId(primaryContact.getId());
        }

        Set<String> emailSet = new HashSet<>();
        Set<String> phoneNumberSet = new HashSet<>();
        Set<Long> secondaryContactIdSet = new HashSet<>();

        secondaryContacts.forEach(secondaryContact -> {
            if(!(StringUtils.isEmpty(secondaryContact.getEmail())
                    || secondaryContact.getEmail().equals(primaryContact.getEmail()))) {
                emailSet.add(secondaryContact.getEmail());
            }
            if(!(StringUtils.isEmpty(secondaryContact.getPhoneNumber())
                    || secondaryContact.getPhoneNumber().equals(primaryContact.getPhoneNumber()))) {
                phoneNumberSet.add(secondaryContact.getPhoneNumber());
            }
            secondaryContactIdSet.add(secondaryContact.getId());
        });

        List<String> emails = new ArrayList<>(emailSet);
        List<String> phoneNumbers = new ArrayList<>(phoneNumberSet);

        // Primary contact details should come first
        emails.add(0, primaryContact.getEmail());
        phoneNumbers.add(0, primaryContact.getPhoneNumber());

        return ContactResponseDto.builder()
                .primaryContactId(primaryContact.getId())
                .emails(emails)
                .phoneNumbers(phoneNumbers)
                .secondaryContactIds(secondaryContactIdSet)
                .build();
    }
}
