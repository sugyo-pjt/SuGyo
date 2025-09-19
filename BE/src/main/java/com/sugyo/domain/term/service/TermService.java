package com.sugyo.domain.term.service;

import com.sugyo.common.annotation.CacheableWithTTL;
import com.sugyo.domain.term.domain.Term;
import com.sugyo.domain.term.dto.TermResponse;
import com.sugyo.domain.term.dto.TermTitleResponse;
import com.sugyo.domain.term.repository.TermRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Service
public class TermService {
    private final TermRepository termRepository;

    @CacheableWithTTL(cacheName = "terms-summary", ttl = 10, unit = ChronoUnit.MINUTES)
    public Set<TermTitleResponse> getTermSummary() {
        return getAllTerms().stream()
                .map(TermTitleResponse::from)
                .collect(Collectors.toSet());
    }

    @CacheableWithTTL(cacheName = "term", ttl = 10, unit = ChronoUnit.MINUTES)
    public TermResponse getTermById(long id) {
        return TermResponse.from(termRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("존재하지 않는 약관 id입니다.")));
    }

    public List<Term> getAllTerms() {
        return termRepository.findAll();
    }
}
