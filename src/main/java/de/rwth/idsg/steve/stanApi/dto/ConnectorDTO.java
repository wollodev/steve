package de.rwth.idsg.steve.stanApi.dto;

import lombok.Getter;
import org.joda.time.DateTime;

/**
 * Created by Wolfgang Kluth on 04/11/15.
 */

@Getter
public class ConnectorDTO {
    private Integer connectorId;
    private String status;
    private String errorCode;

    private TransactionDTO openTransaction;

    public ConnectorDTO(Integer connectorId, String status, String errorCode, Integer transactionId, String userIdTag, DateTime startTimestamp) {
        this.connectorId = connectorId;
        this.status = status;
        this.errorCode = errorCode;

        if (userIdTag != null) {
            this.openTransaction = new TransactionDTO(transactionId, userIdTag, startTimestamp);
        }
    }

}
