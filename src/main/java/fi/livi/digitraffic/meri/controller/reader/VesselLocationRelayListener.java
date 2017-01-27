package fi.livi.digitraffic.meri.controller.reader;

import fi.livi.digitraffic.meri.controller.MessageConverter;
import fi.livi.digitraffic.meri.controller.VesselSender;
import fi.livi.digitraffic.meri.controller.websocket.VesselEndpoint;
import fi.livi.digitraffic.meri.controller.websocket.VesselMMSIEndpoint;
import fi.livi.digitraffic.meri.model.ais.AISMessage;
import fi.livi.digitraffic.meri.model.ais.VesselLocationFeature;
import fi.livi.digitraffic.meri.service.ais.VesselLocationConverter;
import fi.livi.digitraffic.meri.service.ais.VesselMetadataService;

public class VesselLocationRelayListener implements WebsocketListener {
    private final VesselSender vesselSender;
    private final VesselMetadataService vesselMetadataService;

    public VesselLocationRelayListener(final VesselSender vesselSender,
                                       final VesselMetadataService vesselMetadataService) {
        this.vesselSender = vesselSender;
        this.vesselMetadataService = vesselMetadataService;
    }

    @Override
    public void receiveMessage(final String message) {
        final AISMessage ais = MessageConverter.convertLocation(message);

        // Send only allowed mmsis to WebSocket, ship type 30 fishing boat filtered
        if (ais.validate() && isAllowedMmsi(ais.attributes.mmsi)) {
            final VesselLocationFeature feature = VesselLocationConverter.convert(ais);

            vesselSender.sendLocationMessage(feature);
            VesselEndpoint.sendLocationMessage(feature);
            VesselMMSIEndpoint.sendLocationMessage(feature);
        }
    }

    private boolean isAllowedMmsi(int mmsi) {
        return vesselMetadataService.findAllowedMmsis().contains(mmsi);
    }

    @Override
    public void connectionStatus(final ReconnectingHandler.ConnectionStatus status) {
        // no need to do anything
    }
}
