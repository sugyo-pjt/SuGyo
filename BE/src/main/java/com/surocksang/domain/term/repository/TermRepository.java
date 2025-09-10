package com.surocksang.domain.term.repository;

import com.surocksang.domain.term.domain.Term;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TermRepository extends JpaRepository<Term, Long> {

}
