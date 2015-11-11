package de.rwth.idsg.steve.stanApi.repository;

import de.rwth.idsg.steve.stanApi.dto.TransactionDTO;
import de.rwth.idsg.steve.stanApi.dto.UserDTO;
import lombok.extern.slf4j.Slf4j;
import org.jooq.Configuration;
import org.jooq.Record2;
import org.jooq.impl.DSL;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;

import java.util.List;

import static jooq.steve.db.tables.Connector.CONNECTOR;
import static jooq.steve.db.tables.Transaction.TRANSACTION;
import static jooq.steve.db.tables.User.USER;

/**
 * Created by Wolfgang Kluth on 11/11/15.
 */

@Slf4j
@Repository
public class StanUserRepository {

    @Autowired
    @Qualifier("jooqConfig")
    private Configuration config;

    public List<UserDTO> findUsers() {
        List<UserDTO> users = DSL.using(config)
                                 .select(USER.IDTAG, USER.NOTE)
                                 .from(USER)
                                 .fetch()
                                 .map(r -> UserDTO.builder().idTag(r.value1()).note(r.value2()).build());

        return users;
    }

    public UserDTO findUser(String idTag) {

        Record2<String, String> userRecord = DSL.using(config)
                                                .select(USER.IDTAG, USER.NOTE)
                                                .from(USER)
                                                .where(USER.IDTAG.eq(idTag))
                                                .fetchOne();

        List<TransactionDTO> transactions = DSL.using(config)
                                               .select(TRANSACTION.TRANSACTION_PK, TRANSACTION.STARTTIMESTAMP, TRANSACTION.STOPTIMESTAMP,
                                                       TRANSACTION.STARTVALUE, TRANSACTION.STOPVALUE, CONNECTOR.CHARGEBOXID, CONNECTOR.CONNECTORID)
                                               .from(TRANSACTION)
                                               .join(CONNECTOR)
                                               .on(CONNECTOR.CONNECTOR_PK.eq(TRANSACTION.CONNECTOR_PK))
                                               .where(TRANSACTION.IDTAG.eq(idTag))
                                               .fetch()
                                               .map(r -> new TransactionDTO(r.value1(),
                                                       r.value2(),
                                                       r.value3(),
                                                       calculateConsumption(r.value4(), r.value5()),
                                                       r.value6(),
                                                       r.value7()));

        return UserDTO.builder()
                      .idTag(userRecord.value1())
                      .note(userRecord.value2())
                      .transactions(transactions)
                      .build();

    }

    private Double calculateConsumption(String startValue, String stopValue) {

        if (stopValue == null) {
            return null;
        }

        Double start = Double.parseDouble(startValue);
        Double stop = Double.parseDouble(stopValue);

        Double result = stop - start;

        return result > 0 ? result : 0;
    }

}