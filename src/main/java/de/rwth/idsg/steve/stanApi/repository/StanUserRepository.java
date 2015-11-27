package de.rwth.idsg.steve.stanApi.repository;

import de.rwth.idsg.steve.SteveException;
import de.rwth.idsg.steve.stanApi.dto.TransactionDTO;
import de.rwth.idsg.steve.stanApi.dto.UserDTO;
import lombok.extern.slf4j.Slf4j;
import org.jooq.Configuration;
import org.jooq.Record2;
import org.jooq.exception.DataAccessException;
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

    public void addUser(UserDTO userDTO) {
        try {
            int count = DSL.using(config)
                           .insertInto(USER,
                                   USER.ID_TAG,
                                   USER.PARENT_ID_TAG,
                                   USER.EXPIRY_DATE,
                                   USER.NOTE,
                                   USER.IN_TRANSACTION,
                                   USER.BLOCKED)
                           .values(userDTO.getIdTag(), null, null, userDTO.getNote(), false, false)
                           .onDuplicateKeyIgnore() // Important detail
                           .execute();

            if (count == 0) {
                throw new SteveException("A user with idTag '%s' already exists.", userDTO.getIdTag());
            }
        } catch (DataAccessException e) {
            throw new SteveException("Execution of addUser for idTag '%s' FAILED.", userDTO.getIdTag(), e);
        }
    }

    public void deleteUser(String idTag) {
        try {
            DSL.using(config)
               .delete(USER)
               .where(USER.ID_TAG.equal(idTag))
               .execute();
        } catch (DataAccessException e) {
            throw new SteveException("Execution of deleteUser for idTag '%s' FAILED.", idTag, e);
        }
    }


    public List<UserDTO> findUsers() {
        List<UserDTO> users = DSL.using(config)
                                 .select(USER.ID_TAG, USER.NOTE)
                                 .from(USER)
                                 .fetch()
                                 .map(r -> UserDTO.builder().idTag(r.value1()).note(r.value2()).build());

        return users;
    }

    public UserDTO findUser(String idTag) {

        Record2<String, String> userRecord = DSL.using(config)
                                                .select(USER.ID_TAG, USER.NOTE)
                                                .from(USER)
                                                .where(USER.ID_TAG.eq(idTag))
                                                .fetchOne();

        List<TransactionDTO> transactions = DSL.using(config)
                                               .select(TRANSACTION.TRANSACTION_PK, TRANSACTION.START_TIMESTAMP, TRANSACTION.STOP_TIMESTAMP,
                                                       TRANSACTION.START_VALUE, TRANSACTION.STOP_VALUE, CONNECTOR.CHARGE_BOX_ID, CONNECTOR.CONNECTOR_ID)
                                               .from(TRANSACTION)
                                               .join(CONNECTOR)
                                               .on(CONNECTOR.CONNECTOR_PK.eq(TRANSACTION.CONNECTOR_PK))
                                               .where(TRANSACTION.ID_TAG.eq(idTag))
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

        if (stopValue == null || startValue == null) {
            return null;
        }

        Double start = Double.parseDouble(startValue);
        Double stop = Double.parseDouble(stopValue);

        Double result = stop - start;

        return result > 0 ? result : 0;
    }

}