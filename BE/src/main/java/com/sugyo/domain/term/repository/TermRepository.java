package com.sugyo.domain.term.repository;

import com.sugyo.domain.term.domain.Term;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TermRepository extends JpaRepository<Term, Long> {

}
