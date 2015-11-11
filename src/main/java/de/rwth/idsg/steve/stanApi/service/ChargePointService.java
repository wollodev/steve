package de.rwth.idsg.steve.stanApi.service;

import de.rwth.idsg.steve.ocpp.OcppTransport;
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
import java.util.NoSuchElementException;

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

    public void unlockConnector(String chargeBoxId, int connectorId) throws NoSuchElementException {
        ChargePoint chargePoint = chargePointRepository.getDetails(chargeBoxId);

        String ocppProtocol = chargePoint.getOcppProtocol();

        List<ChargePointSelect> chargePointSelects = getChargePointSelects(chargePoint);

        UnlockConnectorParams unlockConnectorParams = new UnlockConnectorParams();
        unlockConnectorParams.setConnectorId(connectorId);
        unlockConnectorParams.setChargePointSelectList(chargePointSelects);

        if (isOcpp12(chargePoint)) {
            chargePointService12Client.unlockConnector(unlockConnectorParams);
        } else if (isOcpp15(chargePoint)) {
            chargePointService15Client.unlockConnector(unlockConnectorParams);
        } else {
            throw new NoSuchElementException("No OCPP Implementation found.");
        }
    }

    public void startTransaction(String chargeBoxId, int connectorId, String userIdTag) throws NoSuchElementException {

        ChargePoint chargePoint = chargePointRepository.getDetails(chargeBoxId);

        RemoteStartTransactionParams remoteStartTransactionParams = new RemoteStartTransactionParams();
        remoteStartTransactionParams.setConnectorId(connectorId);
        remoteStartTransactionParams.setIdTag(userIdTag);

        if (isOcpp12(chargePoint)) {
            chargePointService12Client.remoteStartTransaction(remoteStartTransactionParams);
        } else if (isOcpp15(chargePoint)) {
            chargePointService15Client.remoteStartTransaction(remoteStartTransactionParams);
        } else {
            throw new NoSuchElementException("No OCPP Implementation found.");
        }
    }

    public void stopTransaction(String chargeBoxId, int transactionId) throws NoSuchElementException {

        ChargePoint chargePoint = chargePointRepository.getDetails(chargeBoxId);

        RemoteStopTransactionParams remoteStopTransactionParams = new RemoteStopTransactionParams();
        remoteStopTransactionParams.setChargePointSelectList(getChargePointSelects(chargePoint));
        remoteStopTransactionParams.setTransactionId(transactionId);

        if (isOcpp12(chargePoint)) {
            chargePointService12Client.remoteStopTransaction(remoteStopTransactionParams);
        } else if (isOcpp15(chargePoint)) {
            chargePointService15Client.remoteStopTransaction(remoteStopTransactionParams);
        } else {
            throw new NoSuchElementException("No OCPP Implementation found.");
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

    private boolean isOcpp12(ChargePoint chargePoint) {
        return chargePoint.getOcppProtocol().contains("1.2");
    }

    private boolean isOcpp15(ChargePoint chargePoint) {
        return chargePoint.getOcppProtocol().contains("1.5");
    }

    private OcppTransport getTransportMode(ChargePoint chargePoint) throws NoSuchElementException {
        String ocppProtocol = chargePoint.getOcppProtocol();

        if (ocppProtocol.contains("S")) {
            return OcppTransport.SOAP;
        } else if (ocppProtocol.contains("P")) {
            return OcppTransport.JSON;
        }

        throw new NoSuchElementException("no ocpp transport type found.");
    }
}
