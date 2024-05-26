package com.contact_reconciliation.repository;

import com.contact_reconciliation.entity.Contact;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface ContactRepository extends JpaRepository<Contact, Long> {
    Optional<Contact> findTopByEmailOrderByIdAsc(String email);

    Optional<Contact> findTopByPhoneNumberOrderByIdAsc(String phoneNumber);

    List<Contact> findAllByLinkedId(Long linkedId);

    List<Contact> findAllByEmail(String email);

    List<Contact> findAllByPhoneNumber(String phoneNumber);

    List<Contact> findAllByIdIn(Collection<Long> ids);

    List<Contact> findAllByLinkedIdIn(Collection<Long> ids);
}
