package de.rwth.idsg.steve.stanApi.service;

import de.rwth.idsg.steve.ocpp.OcppProtocol;
import de.rwth.idsg.steve.ocpp.OcppTransport;
import de.rwth.idsg.steve.ocpp.OcppVersion;
import de.rwth.idsg.steve.repository.ChargePointRepository;
import de.rwth.idsg.steve.repository.dto.ChargePoint;
import de.rwth.idsg.steve.repository.dto.ChargePointSelect;
import de.rwth.idsg.steve.service.ChargePointService12_Client;
import de.rwth.idsg.steve.service.ChargePointService15_Client;
import de.rwth.idsg.steve.web.dto.common.RemoteStartTransactionParams;
import de.rwth.idsg.steve.web.dto.common.RemoteStopTransactionParams;
import de.rwth.idsg.steve.web.dto.common.UnlockConnectorParams;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

/**
 * Created by Wolfgang Kluth on 04/11/15.
 */

@Service
public class ChargePointService {

    @Autowired
    private ChargePointRepository chargePointRepository;

    @Autowired
    private ChargePointService12_Client chargePointService12Client;

    @Autowired
    private ChargePointService15_Client chargePointService15Client;

    private static int CHARGE_POINT_CONNECTOR_ID = 0;

    public void unlockConnector(String chargeBoxId, int connectorId) {
        ChargePoint chargePoint = chargePointRepository.getDetails(chargeBoxId);

        String ocppProtocol = chargePoint.getOcppProtocol();

        List<ChargePointSelect> chargePointSelects = getChargePointSelects(chargePoint);

        UnlockConnectorParams unlockConnectorParams = new UnlockConnectorParams();
        unlockConnectorParams.setConnectorId(connectorId);
        unlockConnectorParams.setChargePointSelectList(chargePointSelects);

        switch (getVersion(chargePoint)) {
            case V_12:
                chargePointService12Client.unlockConnector(unlockConnectorParams);
                break;

            case V_15:
                chargePointService15Client.unlockConnector(unlockConnectorParams);
                break;
        }
    }

    public void startTransaction(String chargeBoxId, int connectorId, String userIdTag) {

        ChargePoint chargePoint = chargePointRepository.getDetails(chargeBoxId);

        RemoteStartTransactionParams remoteStartTransactionParams = new RemoteStartTransactionParams();
        remoteStartTransactionParams.setConnectorId(connectorId);
        remoteStartTransactionParams.setIdTag(userIdTag);

        switch (getVersion(chargePoint)) {
            case V_12:
                chargePointService12Client.remoteStartTransaction(remoteStartTransactionParams);
                break;

            case V_15:
                chargePointService15Client.remoteStartTransaction(remoteStartTransactionParams);
                break;
        }
    }

    public void stopTransaction(String chargeBoxId, int transactionId) {

        ChargePoint chargePoint = chargePointRepository.getDetails(chargeBoxId);

        RemoteStopTransactionParams remoteStopTransactionParams = new RemoteStopTransactionParams();
        remoteStopTransactionParams.setChargePointSelectList(getChargePointSelects(chargePoint));
        remoteStopTransactionParams.setTransactionId(transactionId);

        switch (getVersion(chargePoint)) {
            case V_12:
                chargePointService12Client.remoteStopTransaction(remoteStopTransactionParams);
                break;

            case V_15:
                chargePointService15Client.remoteStopTransaction(remoteStopTransactionParams);
                break;
        }
    }

    //////////////////////////////////////////////////////////////////////////////
    ////////////////////////////     Helper Methods     //////////////////////////
    //////////////////////////////////////////////////////////////////////////////

    private List<ChargePointSelect> getChargePointSelects(ChargePoint chargePoint) {
        OcppTransport ocppTransport = getTransportMode(chargePoint);

        ChargePointSelect chargePointSelect = new ChargePointSelect(ocppTransport,
                chargePoint.getChargeBoxId(),
                chargePoint.getEndpointAddress());
        return Arrays.asList(chargePointSelect);
    }

    private OcppVersion getVersion(ChargePoint chargePoint) {
        OcppProtocol ocppProtocol = OcppProtocol.fromCompositeValue(chargePoint.getOcppProtocol());
        return ocppProtocol.getVersion();
    }

    private OcppTransport getTransportMode(ChargePoint chargePoint) {
        OcppProtocol ocppProtocol = OcppProtocol.fromCompositeValue(chargePoint.getOcppProtocol());
        return ocppProtocol.getTransport();
    }
}
