package com.machly.app.domain;

import static org.junit.Assert.*;
import org.junit.Test;

public class SessionStateTest {
 @Test public void validTokenIsAccepted(){ assertTrue(SessionState.isValid("token",2000,1000)); }
 @Test public void expiredTokenIsRejected(){ assertFalse(SessionState.isValid("token",1000,1000)); }
 @Test public void missingTokenIsRejected(){ assertFalse(SessionState.isValid(null,2000,1000)); }
 @Test public void blankTokenIsRejected(){ assertFalse(SessionState.isValid(" ",2000,1000)); }
}
