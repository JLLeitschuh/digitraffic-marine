package fi.livi.digitraffic.meri.dao.portnet;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import fi.livi.digitraffic.meri.domain.portnet.PortArea;
import fi.livi.digitraffic.meri.domain.portnet.PortAreaKey;

@Repository
public interface PortAreaRepository extends JpaRepository<PortArea, PortAreaKey> {
    List<PortArea> findByPortAreaKeyLocode(final String locode);
}
