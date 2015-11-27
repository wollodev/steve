package de.rwth.idsg.steve.stanApi.dto;

import lombok.Data;
import org.joda.time.DateTime;

/**
 * Created by Wolfgang Kluth on 10/11/15.
 */

@Data
public class TransactionDTO {

    private Integer transactionId;
    private String userIdTag;
    private DateTime startTimestamp;
    private DateTime stopTimestamp;
    private Double consumption;
    private String chargePoint;
    private Integer connectorId;

    public TransactionDTO(Integer transactionId, String userIdTag, DateTime startTimestamp) {
        this.transactionId = transactionId;
        this.userIdTag = userIdTag;
        this.startTimestamp = startTimestamp;
    }

    public TransactionDTO(Integer transactionId, DateTime startTimestamp, DateTime stopTimestamp, Double consumption,
                          String chargePoint,
                          Integer connectorId) {
        this.transactionId = transactionId;
        this.startTimestamp = startTimestamp;
        this.stopTimestamp = stopTimestamp;
        this.consumption = consumption;
        this.chargePoint = chargePoint;
        this.connectorId = connectorId;
    }
}
