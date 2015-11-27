package de.rwth.idsg.steve.repository;

import de.rwth.idsg.steve.SteveException;
import de.rwth.idsg.steve.ocpp.OcppProtocol;
import de.rwth.idsg.steve.ocpp.OcppTransport;
import de.rwth.idsg.steve.repository.dto.ChargePoint;
import de.rwth.idsg.steve.repository.dto.ChargePointSelect;
import de.rwth.idsg.steve.repository.dto.ConnectorStatus;
import de.rwth.idsg.steve.utils.DateTimeUtils;
import de.rwth.idsg.steve.web.dto.Address;
import de.rwth.idsg.steve.web.dto.ChargeBoxForm;
import de.rwth.idsg.steve.web.dto.ChargePointQueryForm;
import jooq.steve.db.tables.records.AddressRecord;
import jooq.steve.db.tables.records.ChargeBoxRecord;
import lombok.extern.slf4j.Slf4j;
import org.joda.time.DateTime;
import org.jooq.Configuration;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Record1;
import org.jooq.Record4;
import org.jooq.Result;
import org.jooq.SelectConditionStep;
import org.jooq.SelectQuery;
import org.jooq.TableLike;
import org.jooq.exception.DataAccessException;
import org.jooq.impl.DSL;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;

import java.util.List;

import static de.rwth.idsg.steve.utils.CustomDSL.date;
import static de.rwth.idsg.steve.utils.CustomDSL.includes;
import static jooq.steve.db.tables.Address.ADDRESS;
import static jooq.steve.db.tables.ChargeBox.CHARGE_BOX;
import static jooq.steve.db.tables.Connector.CONNECTOR;
import static jooq.steve.db.tables.ConnectorStatus.CONNECTOR_STATUS;

/**
 * @author Sevket Goekay <goekay@dbis.rwth-aachen.de>
 * @since 14.08.2014
 */
@Slf4j
@Repository
public class ChargePointRepositoryImpl implements ChargePointRepository {

    @Autowired
    @Qualifier("jooqConfig")
    private Configuration config;

    @Autowired private AddressRepository addressRepository;

    @Override
    public boolean isRegistered(String chargeBoxId) {
        Record1<Integer> r = DSL.using(config)
                                .selectOne()
                                .from(CHARGE_BOX)
                                .where(CHARGE_BOX.CHARGE_BOX_ID.eq(chargeBoxId))
                                .fetchOne();

        return (r != null) && (r.value1() == 1);
    }

    @Override
    public List<ChargePointSelect> getChargePointSelect(OcppProtocol protocol) {
        final OcppTransport transport = protocol.getTransport();

        return DSL.using(config)
                  .select(CHARGE_BOX.CHARGE_BOX_ID, CHARGE_BOX.ENDPOINT_ADDRESS)
                  .from(CHARGE_BOX)
                  .where(CHARGE_BOX.OCPP_PROTOCOL.equal(protocol.getCompositeValue()))
                  .and(CHARGE_BOX.ENDPOINT_ADDRESS.isNotNull())
                  .fetch()
                  .map(r -> new ChargePointSelect(transport, r.value1(), r.value2()));
    }

    @Override
    public List<String> getChargeBoxIds() {
        return DSL.using(config)
                  .select(CHARGE_BOX.CHARGE_BOX_ID)
                  .from(CHARGE_BOX)
                  .fetch(CHARGE_BOX.CHARGE_BOX_ID);
    }

    @Override
    public List<ChargePoint.Overview> getOverview(ChargePointQueryForm form) {
        return getOverviewInternal(form)
                  .map(r -> ChargePoint.Overview.builder()
                                                .chargeBoxId(r.value1())
                                                .description(r.value2())
                                                .ocppProtocol(r.value3())
                                                .lastHeartbeatTimestamp(DateTimeUtils.humanize(r.value4()))
                                                .build()
                  );
    }

    @SuppressWarnings("unchecked")
    private Result<Record4<String, String, String, DateTime>> getOverviewInternal(ChargePointQueryForm form) {
        SelectQuery selectQuery = DSL.using(config).selectQuery();
        selectQuery.addFrom(CHARGE_BOX);
        selectQuery.addSelect(
                CHARGE_BOX.CHARGE_BOX_ID,
                CHARGE_BOX.DESCRIPTION,
                CHARGE_BOX.OCPP_PROTOCOL,
                CHARGE_BOX.LAST_HEARTBEAT_TIMESTAMP
        );

        if (form.isSetOcppVersion()) {

            // http://dev.mysql.com/doc/refman/5.7/en/pattern-matching.html
            selectQuery.addConditions(CHARGE_BOX.OCPP_PROTOCOL.like(form.getOcppVersion().getValue() + "_"));
        }

        if (form.isSetDescription()) {
            selectQuery.addConditions(includes(CHARGE_BOX.DESCRIPTION, form.getDescription()));
        }

        switch (form.getHeartbeatPeriod()) {
            case ALL:
                break;

            case TODAY:
                selectQuery.addConditions(
                        date(CHARGE_BOX.LAST_HEARTBEAT_TIMESTAMP).eq(date(DateTime.now()))
                );
                break;

            case YESTERDAY:
                selectQuery.addConditions(
                        date(CHARGE_BOX.LAST_HEARTBEAT_TIMESTAMP).eq(date(DateTime.now().minusDays(1)))
                );
                break;

            case EARLIER:
                selectQuery.addConditions(
                        date(CHARGE_BOX.LAST_HEARTBEAT_TIMESTAMP).lessThan(date(DateTime.now().minusDays(1)))
                );
                break;

            default:
                throw new SteveException("Unknown enum type");
        }

        // Default order
        selectQuery.addOrderBy(CHARGE_BOX.CHARGE_BOX_PK.asc());

        return selectQuery.fetch();
    }

    @Override
    public ChargePoint.Details getDetails(String chargeBoxId) {
        DSLContext ctx = DSL.using(config);

        ChargeBoxRecord cbr = ctx.selectFrom(CHARGE_BOX)
                                 .where(CHARGE_BOX.CHARGE_BOX_ID.equal(chargeBoxId))
                                 .fetchOne();

        if (cbr == null) {
            throw new SteveException("There is no charge point with chargeBoxId '%s'", chargeBoxId);
        }

        cbr.detach();

        AddressRecord ar = null;
        if (cbr.getAddressPk() != null) {
            ar = ctx.selectFrom(ADDRESS)
                    .where(ADDRESS.ADDRESS_PK.equal(cbr.getAddressPk()))
                    .fetchOne();
        }

        if (ar != null) {
            ar.detach();
        }

        return new ChargePoint.Details(cbr, ar);
    }

    @Override
    public List<ConnectorStatus> getChargePointConnectorStatus() {
        // Prepare for the inner select of the second join
        Field<Integer> t1Pk = CONNECTOR_STATUS.CONNECTOR_PK.as("t1_pk");
        Field<DateTime> t1Max = DSL.max(CONNECTOR_STATUS.STATUS_TIMESTAMP).as("t1_max");
        TableLike<?> t1 = DSL.select(t1Pk, t1Max)
                             .from(CONNECTOR_STATUS)
                             .groupBy(CONNECTOR_STATUS.CONNECTOR_PK)
                             .asTable("t1");

        return DSL.using(config)
                  .select(CONNECTOR.CHARGE_BOX_ID, CONNECTOR.CONNECTOR_ID,
                          CONNECTOR_STATUS.STATUS_TIMESTAMP, CONNECTOR_STATUS.STATUS, CONNECTOR_STATUS.ERROR_CODE)
                  .from(CONNECTOR_STATUS)
                  .join(CONNECTOR)
                  .onKey()
                  .join(t1)
                  .on(CONNECTOR_STATUS.CONNECTOR_PK.equal(t1.field(t1Pk)))
                  .and(CONNECTOR_STATUS.STATUS_TIMESTAMP.equal(t1.field(t1Max)))
                  .orderBy(CONNECTOR_STATUS.STATUS_TIMESTAMP.desc())
                  .fetch()
                  .map(r -> ConnectorStatus.builder()
                                           .chargeBoxId(r.value1())
                                           .connectorId(r.value2())
                                           .timeStamp(DateTimeUtils.humanize(r.value3()))
                                           .status(r.value4())
                                           .errorCode(r.value5())
                                           .build()
                  );
    }

    @Override
    public List<Integer> getConnectorIds(String chargeBoxId) {
        return DSL.using(config)
                  .select(CONNECTOR.CONNECTOR_ID)
                  .from(CONNECTOR)
                  .where(CONNECTOR.CHARGE_BOX_ID.equal(chargeBoxId))
                  .fetch(CONNECTOR.CONNECTOR_ID);
    }

    @Override
    public void addChargePoint(ChargeBoxForm form) {
        DSL.using(config).transaction(configuration -> {
            DSLContext ctx = DSL.using(configuration);
            try {
                Integer addressId = addressRepository.insert(ctx, form.getAddress());
                addChargePointInternal(ctx, form, addressId);

            } catch (DataAccessException e) {
                throw new SteveException("The charge point with chargeBoxId '%s' could NOT be added.",
                        form.getChargeBoxId(), e);
            }
        });
    }

    @Override
    public void updateChargePoint(ChargeBoxForm form) {
        DSL.using(config).transaction(configuration -> {
            DSLContext ctx = DSL.using(configuration);
            try {
                Address address = form.getAddress();
                Integer addressId = addressRepository.updateOrInsert(ctx, address);
                updateChargePointInternal(ctx, form, addressId);

            } catch (DataAccessException e) {
                throw new SteveException("The charge point with chargeBoxId '%s' could NOT be added.",
                        form.getChargeBoxId(), e);
            }
        });
    }

    @Override
    public void deleteChargePoint(String chargeBoxId) {
        DSL.using(config).transaction(configuration -> {
            DSLContext ctx = DSL.using(configuration);
            try {
                addressRepository.delete(ctx, selectAddressId(chargeBoxId));
                deleteChargePointInternal(ctx, chargeBoxId);

            } catch (DataAccessException e) {
                throw new SteveException("The charge point with chargeBoxId '%s' could NOT be deleted.",
                        chargeBoxId, e);
            }
        });
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private SelectConditionStep<Record1<Integer>> selectAddressId(String chargeBoxId) {
        return DSL.select(CHARGE_BOX.ADDRESS_PK)
                  .from(CHARGE_BOX)
                  .where(CHARGE_BOX.CHARGE_BOX_ID.eq(chargeBoxId));
    }

    private void addChargePointInternal(DSLContext ctx, ChargeBoxForm form, Integer addressPk) {
        int count = ctx.insertInto(CHARGE_BOX)
                       .set(CHARGE_BOX.CHARGE_BOX_ID, form.getChargeBoxId())
                       .set(CHARGE_BOX.DESCRIPTION, form.getDescription())
                       .set(CHARGE_BOX.LOCATION_LATITUDE, form.getLocationLatitude())
                       .set(CHARGE_BOX.LOCATION_LONGITUDE, form.getLocationLongitude())
                       .set(CHARGE_BOX.NOTE, form.getNote())
                       .set(CHARGE_BOX.ADDRESS_PK, addressPk)
                       .onDuplicateKeyIgnore() // Important detail
                       .execute();

        if (count == 0) {
            throw new SteveException("A charge point with chargeBoxId '%s' already exists.", form.getChargeBoxId());
        }
    }

    private void updateChargePointInternal(DSLContext ctx, ChargeBoxForm form, Integer addressPk) {
        ctx.update(CHARGE_BOX)
           .set(CHARGE_BOX.DESCRIPTION, form.getDescription())
           .set(CHARGE_BOX.LOCATION_LATITUDE, form.getLocationLatitude())
           .set(CHARGE_BOX.LOCATION_LONGITUDE, form.getLocationLongitude())
           .set(CHARGE_BOX.NOTE, form.getNote())
           .set(CHARGE_BOX.ADDRESS_PK, addressPk)
           .where(CHARGE_BOX.CHARGE_BOX_ID.equal(form.getChargeBoxId()))
           .execute();
    }

    private void deleteChargePointInternal(DSLContext ctx, String chargeBoxId) {
        ctx.delete(CHARGE_BOX)
           .where(CHARGE_BOX.CHARGE_BOX_ID.equal(chargeBoxId))
           .execute();
    }
}
