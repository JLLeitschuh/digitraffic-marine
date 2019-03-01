package fi.livi.digitraffic.meri.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

import fi.livi.digitraffic.meri.config.MqttConfig;
import fi.livi.digitraffic.meri.domain.ais.VesselMetadata;
import fi.livi.digitraffic.meri.model.ais.VesselLocationFeature;

@ConditionalOnProperty("ais.mqtt.enabled")
@Component
public class VesselMqttSender {
    private static final Logger LOG = LoggerFactory.getLogger(VesselMqttSender.class);

    @Lazy // this will not be available if mqtt is not enabled
    private final MqttConfig.VesselGateway vesselGateway;
    private final ObjectMapper objectMapper;

    private static final String VESSELS_METADATA_TOPIC = "vessels/%d/metadata";
    private static final String VESSELS_LOCATIONS_TOPIC = "vessels/%d/locations";
    private static final String VESSEL_STATUS_TOPIC  ="vessels/status";

    @Autowired
    public VesselMqttSender(final MqttConfig.VesselGateway vesselGateway, final ObjectMapper objectMapper) {
        this.vesselGateway = vesselGateway;
        this.objectMapper = objectMapper;
    }

    public boolean sendMetadataMessage(final VesselMetadata vesselMetadata) {
        try {
            final String metadataAsString = objectMapper.writeValueAsString(vesselMetadata);

            sendMessage(metadataAsString, String.format(VESSELS_METADATA_TOPIC, vesselMetadata.getMmsi()));

            return true;
        } catch (final Exception e) {
            LOG.error("error sending metadata", e);
        }

        return false;
    }

    public boolean sendLocationMessage(final VesselLocationFeature vesselLocation) {
        try {
            final String locationAsString = objectMapper.writeValueAsString(vesselLocation);

            sendMessage(locationAsString, String.format(VESSELS_LOCATIONS_TOPIC, vesselLocation.mmsi));

            return true;
        } catch (final Exception e) {
            LOG.error("error sending location", e);
        }

        return false;
    }

    public boolean sendStatusMessage(final Object status) {
        try {
            final String statusAsString = objectMapper.writeValueAsString(status);

            sendMessage(statusAsString, VESSEL_STATUS_TOPIC);

            return true;
        } catch (final Exception e) {
            LOG.error("error sending status", e);
        }

        return false;
    }

    // This must be synchronized, because Paho does not support concurrency!
    private synchronized void sendMessage(final String payLoad, final String topic) {
        vesselGateway.sendToMqtt(topic, payLoad);
    }
}
