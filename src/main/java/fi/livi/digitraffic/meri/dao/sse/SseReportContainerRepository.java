package fi.livi.digitraffic.meri.dao.sse;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import fi.livi.digitraffic.meri.domain.sse.tlsc.SseReportContainer;

@Repository
public interface SseReportContainerRepository extends JpaRepository<SseReportContainer, Long> {



}