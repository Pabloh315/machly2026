package com.machly.app.domain;

import static org.junit.Assert.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.Test;

public class BookingCalculatorTest {
 @Test public void oneDayReservationHasOneInclusiveDay(){ assertEquals(1, BookingCalculator.inclusiveDays(LocalDate.of(2026,1,10),LocalDate.of(2026,1,10))); }
 @Test public void multipleDaysAreInclusive(){ assertEquals(3, BookingCalculator.inclusiveDays(LocalDate.of(2026,1,10),LocalDate.of(2026,1,12))); }
 @Test(expected=IllegalArgumentException.class) public void invertedDatesAreRejected(){ BookingCalculator.inclusiveDays(LocalDate.of(2026,1,12),LocalDate.of(2026,1,10)); }
 @Test public void quoteCalculatesSubtotalFeeAndTotal(){ BookingCalculator.Quote q=BookingCalculator.quote(LocalDate.of(2026,1,10),LocalDate.of(2026,1,12),new BigDecimal("100.00")); assertEquals(3,q.days); assertEquals(new BigDecimal("300.00"),q.subtotal); assertEquals(new BigDecimal("24.00"),q.serviceFee); assertEquals(new BigDecimal("324.00"),q.total); }
 @Test public void quoteRoundsMonetaryValues(){ BookingCalculator.Quote q=BookingCalculator.quote(LocalDate.of(2026,1,10),LocalDate.of(2026,1,10),new BigDecimal("10.155")); assertEquals(new BigDecimal("10.16"),q.subtotal); assertEquals(new BigDecimal("0.81"),q.serviceFee); assertEquals(new BigDecimal("10.97"),q.total); }
}
