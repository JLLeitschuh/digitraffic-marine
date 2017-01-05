package fi.livi.digitraffic.meri.service.portnet.vesseldetails;

import static fi.livi.digitraffic.meri.dao.UpdatedTimestampRepository.UpdatedName.VESSEL_DETAILS;

import java.sql.Date;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import fi.livi.digitraffic.meri.dao.UpdatedTimestampRepository;
import fi.livi.digitraffic.meri.dao.portnet.VesselDetailsRepository;
import fi.livi.digitraffic.meri.domain.portnet.VesselDetails.VesselDetails;
import fi.livi.digitraffic.meri.portnet.vesseldetails.xsd.VesselList;

@Service
public class VesselDetailsUpdater {

    private final VesselDetailsRepository vesselDetailsRepository;

    private final VesselDetailsClient vesselDetailsClient;

    private final UpdatedTimestampRepository updatedTimestampRepository;

    private final static Logger log = LoggerFactory.getLogger(VesselDetailsUpdater.class);

    @Autowired
    public VesselDetailsUpdater(final VesselDetailsRepository vesselDetailsRepository,
                                final VesselDetailsClient vesselDetailsClient,
                                final UpdatedTimestampRepository updatedTimestampRepository) {
        this.vesselDetailsRepository = vesselDetailsRepository;
        this.vesselDetailsClient = vesselDetailsClient;
        this.updatedTimestampRepository = updatedTimestampRepository;
    }

    @Transactional
    public void update() {
        Instant lastUpdated = updatedTimestampRepository.getLastUpdated(VESSEL_DETAILS.toString());

        updateVesselDetails(lastUpdated);
    }

    @Transactional
    protected void updateVesselDetails(Instant lastUpdated) {

        final Instant now = Instant.now();
        final Instant from = lastUpdated == null ? now.minus(1, ChronoUnit.DAYS) : lastUpdated;

        VesselList vesselList = vesselDetailsClient.getVesselList(from);

        if (isListOk(vesselList)) {

            updatedTimestampRepository.setUpdated(VESSEL_DETAILS.name(), Date.from(now), getClass().getSimpleName());

            final List<VesselDetails> added = new ArrayList<>();
            final List<VesselDetails> updated = new ArrayList<>();
            final StopWatch watch = new StopWatch();

            watch.start();
            vesselList.getVesselDetails().forEach(vesselDetails -> update(vesselDetails, added, updated));
            vesselDetailsRepository.save(added);
            watch.stop();

            log.info("Added {} vessel detail, updated {}, took {} ms.", added.size(), updated.size(), watch.getTime());
        }
    }

    private void update(fi.livi.digitraffic.meri.portnet.vesseldetails.xsd.VesselDetails vd, List<VesselDetails> added, List<VesselDetails> updated) {
        final VesselDetails old = vesselDetailsRepository.findOne(vd.getIdentificationData().getVesselId().longValue());

        if(old == null) {
            final VesselDetails vesselDetails = new VesselDetails();
            vesselDetails.setAll(vd);
            added.add(vesselDetails);
        } else {
            old.setAll(vd);
            updated.add(old);
        }
    }

    private static boolean isListOk(final VesselList list) {
        final String status = getStatusFromResponse(list);

        switch(status) {
        case "OK":
            log.info("fetched {} vessel details", CollectionUtils.size(list.getVesselDetails()));
            break;
        case "NOT_FOUND":
            log.info("No vessel details from server");
            break;
        default:
            log.error("error with status " + status);
            return false;
        }

        return true;
    }

    private static String getStatusFromResponse(final VesselList list) {
        if(list == null) {
            return "ERROR";
        }

        return list.getHeader() == null ? "NOT_FOUND" : list.getHeader().getResponseType().getStatus();
    }
}
