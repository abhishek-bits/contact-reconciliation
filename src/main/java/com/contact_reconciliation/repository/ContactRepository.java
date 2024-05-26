package com.contact_reconciliation.repository;


import com.contact_reconciliation.entity.Contact;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;

@Repository
public interface ContactRepository extends JpaRepository<Contact, Long> {
    Collection<Contact> findByEmailLike(String email);
    Collection<Contact> findByPhoneNumberLike(String phoneNumber);
    Collection<Contact> findByEmailLikeOrPhoneNumberLike(String email, String phoneNumber);
}