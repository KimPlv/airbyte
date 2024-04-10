/*
 * Copyright (c) 2023 Airbyte, Inc., all rights reserved.
 */

package io.airbyte.integrations.source.mysql.cdc;

import io.airbyte.cdk.db.jdbc.DateTimeConverter;
import io.airbyte.cdk.db.jdbc.JdbcDatabase;
import io.airbyte.cdk.integrations.debezium.internals.DebeziumConverterUtils;
import io.debezium.spi.converter.CustomConverter;
import io.debezium.spi.converter.RelationalColumn;
import io.debezium.time.Conversions;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.Locale;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import org.apache.kafka.connect.data.SchemaBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This is a custom debezium converter used in MySQL to handle the DATETIME data type. We need a
 * custom converter cause by default debezium returns the DATETIME values as numbers. We need to
 * convert it to proper format. Ref :
 * https://debezium.io/documentation/reference/2.1/development/converters.html This is built from
 * reference with {@link io.debezium.connector.mysql.converters.TinyIntOneToBooleanConverter} If you
 * rename this class then remember to rename the datetime.type property value in
 * {@link MySqlCdcProperties#commonProperties(JdbcDatabase)} (If you don't rename, a test would
 * still fail but it might be tricky to figure out where to change the property name)
 */
public class MySQLDateTimeConverter implements CustomConverter<SchemaBuilder, RelationalColumn> {

  private static final Logger LOGGER = LoggerFactory.getLogger(MySQLDateTimeConverter.class);

  private final String[] DATE_TYPES = {"DATE", "DATETIME", "TIME", "TIMESTAMP"};

  @Override
  public void configure(final Properties props) {}

  @Override
  public void converterFor(final RelationalColumn field, final ConverterRegistration<SchemaBuilder> registration) {
    if (Arrays.stream(DATE_TYPES).anyMatch(s -> s.equalsIgnoreCase(field.typeName()))) {
      registerDate(field, registration);
    }
  }

  private int getTimePrecision(final RelationalColumn field) {
    return field.length().orElse(-1);
  }

  // Ref :
  // https://debezium.io/documentation/reference/2.1/connectors/mysql.html#mysql-temporal-types
  private void registerDate(final RelationalColumn field, final ConverterRegistration<SchemaBuilder> registration) {
    final var fieldType = field.typeName();
    registration.register(SchemaBuilder.string().optional(), x -> {
      if (x == null) {
        LOGGER.info("In fieldType : {} ", fieldType.toUpperCase(Locale.ROOT));
        LOGGER.info("Field value is null");
        return DebeziumConverterUtils.convertDefaultValue(field);
      }

      switch (fieldType.toUpperCase(Locale.ROOT)) {
        case "DATETIME":
          printDebugLogs(fieldType.toUpperCase(Locale.ROOT), x);
          if (x instanceof final Long l) {
            LOGGER.info("1");
            if (getTimePrecision(field) <= 3) {
              LOGGER.info("2");
              return DateTimeConverter.convertToTimestamp(Conversions.toInstantFromMillis(l));
            }
            if (getTimePrecision(field) <= 6) {
              LOGGER.info("3");
              return DateTimeConverter.convertToTimestamp(Conversions.toInstantFromMicros(l));
            }
          }
          LOGGER.info("4");
          return DateTimeConverter.convertToTimestamp(x);
        case "DATE":
          printDebugLogs(fieldType.toUpperCase(Locale.ROOT), x);
          if (x instanceof final Integer i) {
            return DateTimeConverter.convertToDate(LocalDate.ofEpochDay(i));
          }
          return DateTimeConverter.convertToDate(x);
        case "TIME":
          printDebugLogs(fieldType.toUpperCase(Locale.ROOT), x);
          if (x instanceof Long) {
            long l = Math.multiplyExact((Long) x, TimeUnit.MICROSECONDS.toNanos(1));
            return DateTimeConverter.convertToTime(LocalTime.ofNanoOfDay(l));
          }
          return DateTimeConverter.convertToTime(x);
        case "TIMESTAMP":
          printDebugLogs(fieldType.toUpperCase(Locale.ROOT), x);
          return DateTimeConverter.convertToTimestampWithTimezone(x);
        case "INT":
          printDebugLogs(fieldType.toUpperCase(Locale.ROOT), x);
          LOGGER.info(x.getClass().getName());
          return x.toString();
        default:
          throw new IllegalArgumentException("Unknown field type  " + fieldType.toUpperCase(Locale.ROOT));
      }
    });
  }

  private void printDebugLogs(String fieldType, Object x) {
    LOGGER.info("In fieldType : {} ", fieldType);
    LOGGER.info("Value to convert : " + x.toString());
  }
}
