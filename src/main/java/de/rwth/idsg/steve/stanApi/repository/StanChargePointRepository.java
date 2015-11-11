package de.rwth.idsg.steve.stanApi.repository;

import de.rwth.idsg.steve.stanApi.dto.ChargePointDTO;
import de.rwth.idsg.steve.stanApi.dto.ConnectionStatus;
import de.rwth.idsg.steve.stanApi.dto.ConnectorDTO;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.joda.time.DateTime;
import org.jooq.Configuration;
import org.jooq.Field;
import org.jooq.TableLike;
import org.jooq.impl.DSL;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static jooq.steve.db.tables.Chargebox.CHARGEBOX;
import static jooq.steve.db.tables.Connector.CONNECTOR;
import static jooq.steve.db.tables.ConnectorStatus.CONNECTOR_STATUS;
import static jooq.steve.db.tables.Settings.SETTINGS;
import static jooq.steve.db.tables.Transaction.TRANSACTION;

/**
 * Created by Wolfgang Kluth on 04/11/15.
 */

@Slf4j
@Repository
public class StanChargePointRepository {

    @Autowired
    @Qualifier("jooqConfig")
    private Configuration config;


    @Getter
    @AllArgsConstructor
    private class DummyConnector {
        private Integer connectorId;
        private String chargeBoxId;
        private String status;
        private String errorCode;
        private Integer transactionId;
        private String transactionUserIdTag;
        private DateTime transactionStartTimestamp;
    }

    public List<ChargePointDTO> getChargeBoxes() {

        int heartbeatInterval = DSL.using(config)
                                   .select(SETTINGS.HEARTBEATINTERVALINSECONDS)
                                   .from(SETTINGS)
                                   .fetchOne()
                                   .value1();

        List<ChargePointDTO> chargePointDTOs = DSL.using(config)
                                                  .select(CHARGEBOX.CHARGEBOXID, CHARGEBOX.LASTHEARTBEATTIMESTAMP)
                                                  .from(CHARGEBOX)
                                                  .fetch()
                                                  .map(r -> createChargePointDTO(r.value1(), r.value2(), heartbeatInterval));

        Field<Integer> t1Pk = CONNECTOR_STATUS.CONNECTOR_PK.as("t1_pk");
        Field<DateTime> t1Max = DSL.max(CONNECTOR_STATUS.STATUSTIMESTAMP).as("t1_max");
        TableLike<?> t1 = DSL.select(t1Pk, t1Max, CONNECTOR_STATUS.ERRORCODE, CONNECTOR_STATUS.STATUS)
                             .from(CONNECTOR_STATUS)
                             .groupBy(CONNECTOR_STATUS.CONNECTOR_PK)
                             .asTable("t1");

        Field<Integer> transCpk = TRANSACTION.CONNECTOR_PK.as("trans_cpk");
        Field<DateTime> transStartMax = DSL.max(TRANSACTION.STARTTIMESTAMP).as("trans_start_max");
        Field<Integer> transPk= TRANSACTION.TRANSACTION_PK.as("trans_pk");
        TableLike<?> trans = DSL.select(transCpk, transStartMax, TRANSACTION.IDTAG, transPk)
                                .from(TRANSACTION)
                                .where(TRANSACTION.STOPTIMESTAMP.isNull())
                                .groupBy(TRANSACTION.CONNECTOR_PK)
                                .asTable("trans");

        List<DummyConnector> connectors = DSL.using(config)
                                             .select(CONNECTOR.CONNECTORID, CONNECTOR.CHARGEBOXID,
                                                     t1.field(CONNECTOR_STATUS.STATUS), t1.field(CONNECTOR_STATUS.ERRORCODE),
                                                     trans.field(transPk),
                                                     trans.field(TRANSACTION.IDTAG), trans.field(transStartMax))
                                             .from(CONNECTOR)
                                             .leftOuterJoin(t1).on(t1Pk.eq(CONNECTOR.CONNECTOR_PK))
                                             .leftOuterJoin(trans).on(transCpk.eq(CONNECTOR.CONNECTOR_PK))
                                             .fetch()
                                             .map(r -> new DummyConnector(r.value1(), r.value2(), r.value3(),
                                                     r.value4(), r.value5(), r.value6(), r.value7()));


        List<DummyConnector> chargePointStatus = DSL.using(config)
                                                    .select(CONNECTOR.CONNECTORID, CONNECTOR.CHARGEBOXID,
                                                            CONNECTOR_STATUS.STATUS, CONNECTOR_STATUS.ERRORCODE)
                                                    .from(CONNECTOR_STATUS)
                                                    .join(CONNECTOR)
                                                    .onKey()
                                                    .join(t1)
                                                    .on(CONNECTOR_STATUS.CONNECTOR_PK.equal(t1.field(t1Pk)))
                                                    .and(CONNECTOR_STATUS.STATUSTIMESTAMP.equal(t1.field(t1Max)))
                                                    .and(CONNECTOR.CONNECTORID.equal(0)) // filter only chargePoint status
                                                    .orderBy(CONNECTOR_STATUS.STATUSTIMESTAMP.desc())
                                                    .fetch()
                                                    .map(r -> new DummyConnector(r.value1(), r.value2(), r.value3(),
                                                            r.value4(), 0, null, null));

        Map<String, List<ConnectorDTO>> connectorsMap = toMap(connectors);
        Map<String, List<ConnectorDTO>> chargePointStatusMap = toMap(chargePointStatus);

        chargePointDTOs.forEach(cp -> completeChargePointDTO(
                cp,
                chargePointStatusMap.get(cp.getCharBoxId()),
                connectorsMap.get(cp.getCharBoxId())));

        return chargePointDTOs;
    }

    private ChargePointDTO createChargePointDTO(String chargePointId, DateTime lastHeartbeat, int heartbeatInterval) {
        DateTime minHeartbeatDistance = DateTime.now().minusSeconds(heartbeatInterval);

        ConnectionStatus connectionStatus;

        if (lastHeartbeat.isAfter(minHeartbeatDistance)) {
            connectionStatus = ConnectionStatus.CONNECTED;
        } else {
            connectionStatus = ConnectionStatus.DISCONNECTED;
        }

        return ChargePointDTO.builder()
                             .charBoxId(chargePointId)
                             .connectionStatus(connectionStatus)
                             .build();
    }

    private void completeChargePointDTO(ChargePointDTO chargePointDTO,
                                        List<ConnectorDTO> chargePointConnectorList,
                                        List<ConnectorDTO> connectors) {

        chargePointDTO.setConnectors(connectors);

        if (chargePointConnectorList != null && !chargePointConnectorList.isEmpty()) {
            ConnectorDTO chargePointConnector = chargePointConnectorList.get(0);

            chargePointDTO.setStatus(chargePointConnector.getStatus());
            chargePointDTO.setErrorCode(chargePointConnector.getErrorCode());
        }
    }

    private Map<String, List<ConnectorDTO>> toMap(List<DummyConnector> list) {
        return list.stream()
                   .collect(Collectors.groupingBy(DummyConnector::getChargeBoxId))
                   .entrySet()
                   .parallelStream()
                   .collect(Collectors.toMap(Map.Entry::getKey,
                           e -> e.getValue()
                                 .parallelStream()
                                 .map(dc -> new ConnectorDTO(
                                         dc.getConnectorId(),
                                         dc.getStatus(),
                                         dc.getErrorCode(),
                                         dc.getTransactionId(),
                                         dc.transactionUserIdTag,
                                         dc.transactionStartTimestamp))
                                 .collect(Collectors.toList())));
    }

}
