package de.rwth.idsg.steve.repository;

import de.rwth.idsg.steve.SteveException;
import de.rwth.idsg.steve.repository.dto.OcppTag;
import de.rwth.idsg.steve.utils.CustomDSL;
import de.rwth.idsg.steve.web.dto.OcppTagForm;
import de.rwth.idsg.steve.web.dto.OcppTagQueryForm;
import jooq.steve.db.tables.records.OcppTagRecord;
import lombok.extern.slf4j.Slf4j;
import org.joda.time.DateTime;
import org.jooq.Configuration;
import org.jooq.Record5;
import org.jooq.RecordMapper;
import org.jooq.Result;
import org.jooq.SelectQuery;
import org.jooq.TableField;
import org.jooq.exception.DataAccessException;
import org.jooq.impl.DSL;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;

import java.util.List;

import static de.rwth.idsg.steve.utils.DateTimeUtils.humanize;
import static de.rwth.idsg.steve.utils.DateTimeUtils.toDateTime;
import static jooq.steve.db.tables.OcppTag.OCPP_TAG;

/**
 * @author Sevket Goekay <goekay@dbis.rwth-aachen.de>
 * @since 14.08.2014
 */
@Slf4j
@Repository
public class OcppTagRepositoryImpl implements OcppTagRepository {

    @Autowired
    @Qualifier("jooqConfig")
    private Configuration config;

    @Override
    @SuppressWarnings("unchecked")
    public List<OcppTag> getTags(OcppTagQueryForm form) {
        SelectQuery selectQuery = DSL.using(config).selectQuery();
        selectQuery.addFrom(OCPP_TAG);
        selectQuery.addSelect(
                OCPP_TAG.ID_TAG,
                OCPP_TAG.PARENT_ID_TAG,
                OCPP_TAG.EXPIRY_DATE,
                OCPP_TAG.IN_TRANSACTION,
                OCPP_TAG.BLOCKED
        );

        if (form.isIdTagSet()) {
            selectQuery.addConditions(OCPP_TAG.ID_TAG.eq(form.getIdTag()));
        }

        if (form.isParentIdTagSet()) {
            selectQuery.addConditions(OCPP_TAG.PARENT_ID_TAG.eq(form.getParentIdTag()));
        }

        switch (form.getExpired()) {
            case ALL:
                break;

            case TRUE:
                selectQuery.addConditions(OCPP_TAG.EXPIRY_DATE.lessOrEqual(CustomDSL.utcTimestamp()));
                break;

            case FALSE:
                selectQuery.addConditions(
                        OCPP_TAG.EXPIRY_DATE.isNull().or(OCPP_TAG.EXPIRY_DATE.greaterThan(CustomDSL.utcTimestamp()))
                );
                break;

            default:
                throw new SteveException("Unknown enum type");
        }

        processBooleanType(selectQuery, OCPP_TAG.IN_TRANSACTION, form.getInTransaction());
        processBooleanType(selectQuery, OCPP_TAG.BLOCKED, form.getBlocked());

        return selectQuery.fetch().map(new UserMapper());
    }

    @Override
    public Result<OcppTagRecord> getRecords() {
        return DSL.using(config)
                  .selectFrom(OCPP_TAG)
                  .fetch();
    }

    @Override
    public Result<OcppTagRecord> getRecords(List<String> idTagList) {
        return DSL.using(config)
                  .selectFrom(OCPP_TAG)
                  .where(OCPP_TAG.ID_TAG.in(idTagList))
                  .fetch();
    }

    @Override
    public OcppTagRecord getRecord(String idTag) {
        return DSL.using(config)
                  .selectFrom(OCPP_TAG)
                  .where(OCPP_TAG.ID_TAG.equal(idTag))
                  .fetchOne();
    }

    @Override
    public List<String> getIdTags() {
        return DSL.using(config)
                .select(OCPP_TAG.ID_TAG)
                .from(OCPP_TAG)
                .fetch(OCPP_TAG.ID_TAG);
    }

    @Override
    public List<String> getActiveIdTags() {
        return DSL.using(config)
                  .select(OCPP_TAG.ID_TAG)
                  .from(OCPP_TAG)
                  .where(OCPP_TAG.IN_TRANSACTION.isFalse())
                    .and(OCPP_TAG.BLOCKED.isFalse())
                    .and(OCPP_TAG.EXPIRY_DATE.isNull().or(OCPP_TAG.EXPIRY_DATE.greaterThan(CustomDSL.utcTimestamp())))
                  .fetch(OCPP_TAG.ID_TAG);
    }

    @Override
    public List<String> getParentIdTags() {
        return DSL.using(config)
                  .selectDistinct(OCPP_TAG.PARENT_ID_TAG)
                  .from(OCPP_TAG)
                  .where(OCPP_TAG.PARENT_ID_TAG.isNotNull())
                  .fetch(OCPP_TAG.PARENT_ID_TAG);
    }

    @Override
    public String getParentIdtag(String idTag) {
        return DSL.using(config)
                  .select(OCPP_TAG.PARENT_ID_TAG)
                  .from(OCPP_TAG)
                  .where(OCPP_TAG.ID_TAG.eq(idTag))
                  .fetchOne()
                  .value1();
    }

    @Override
    public void addOcppTag(OcppTagForm u) {
        try {
            int count = DSL.using(config)
                           .insertInto(OCPP_TAG)
                           .set(OCPP_TAG.ID_TAG, u.getIdTag())
                           .set(OCPP_TAG.PARENT_ID_TAG, u.getParentIdTag())
                           .set(OCPP_TAG.EXPIRY_DATE, toDateTime(u.getExpiration()))
                           .set(OCPP_TAG.NOTE, u.getNote())
                           .set(OCPP_TAG.BLOCKED, false)
                           .set(OCPP_TAG.IN_TRANSACTION, false)
                           .onDuplicateKeyIgnore() // Important detail
                           .execute();

            if (count == 0) {
                throw new SteveException("A user with idTag '%s' already exists.", u.getIdTag());
            }
        } catch (DataAccessException e) {
            throw new SteveException("Execution of addOcppTag for idTag '%s' FAILED.", u.getIdTag(), e);
        }
    }

    @Override
    public void updateOcppTag(OcppTagForm u) {
        try {
            DSL.using(config)
               .update(OCPP_TAG)
               .set(OCPP_TAG.PARENT_ID_TAG, u.getParentIdTag())
               .set(OCPP_TAG.EXPIRY_DATE, toDateTime(u.getExpiration()))
               .set(OCPP_TAG.NOTE, u.getNote())
               .set(OCPP_TAG.BLOCKED, u.getBlocked())
               .where(OCPP_TAG.ID_TAG.equal(u.getIdTag()))
               .execute();
        } catch (DataAccessException e) {
            throw new SteveException("Execution of updateOcppTag for idTag '%s' FAILED.", u.getIdTag(), e);
        }
    }

    @Override
    public void deleteOcppTag(String idTag) {
        try {
            DSL.using(config)
               .delete(OCPP_TAG)
               .where(OCPP_TAG.ID_TAG.equal(idTag))
               .execute();
        } catch (DataAccessException e) {
            throw new SteveException("Execution of deleteOcppTag for idTag '%s' FAILED.", idTag, e);
        }
    }

    private void processBooleanType(SelectQuery selectQuery,
                                    TableField<OcppTagRecord, Boolean> field,
                                    OcppTagQueryForm.BooleanType type) {
        switch (type) {
            case ALL:
                break;

            default:
                selectQuery.addConditions(field.eq(type.getBoolValue()));
        }
    }

    private static class UserMapper
            implements RecordMapper<Record5<String, String, DateTime, Boolean, Boolean>, OcppTag> {
        @Override
        public OcppTag map(Record5<String, String, DateTime, Boolean, Boolean> r) {
            return OcppTag.builder()
                          .idTag(r.value1())
                          .parentIdTag(r.value2())
                          .expiryDate(humanize(r.value3()))
                          .inTransaction(r.value4())
                          .blocked(r.value5())
                          .build();
        }
    }
}
