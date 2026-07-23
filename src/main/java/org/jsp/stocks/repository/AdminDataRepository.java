package org.jsp.stocks.repository;

import org.jsp.stocks.dto.AdminData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AdminDataRepository extends JpaRepository<AdminData, Integer> {
}