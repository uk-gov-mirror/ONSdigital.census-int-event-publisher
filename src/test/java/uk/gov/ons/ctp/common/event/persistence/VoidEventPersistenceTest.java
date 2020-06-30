package uk.gov.ons.ctp.common.event.persistence;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Test;

public class VoidEventPersistenceTest {

  @Test
  public void testPersistenceSupported() {
    EventPersistence persistence = new VoidEventPersistence();

    assertFalse(persistence.isFirestorePersistenceSupported());
  }

  @Test
  public void testPersistEvent() {
    EventPersistence persistence = new VoidEventPersistence();

    try {
      persistence.persistEvent(null, null, null);
      fail();
    } catch (Exception e) {
      assertTrue(e.getMessage(), e.getMessage().matches("Application not configured .*"));
    }
  }
}
