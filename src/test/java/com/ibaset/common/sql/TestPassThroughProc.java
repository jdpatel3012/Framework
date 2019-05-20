/**
 * Proprietary and Confidential
 * Copyright 1995-2017 iBASEt, Inc.
 * Unpublished-rights reserved under the Copyright Laws of the United States
 * US Government Procurements:
 * Commercial Software licensed with Restricted Rights.
 * Use, reproduction, or disclosure is subject to restrictions set forth in
 * license agreement and purchase contract.
 * iBASEt, Inc. 27442 Portola Parkway, Suite 300, Foothill Ranch, CA 92610
 *
 * Solumina software may be subject to United States Dept of Commerce Export Controls.
 * Contact iBASEt for specific Expert Control Classification information.
 */
package com.ibaset.common.sql;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.doReturn;

import java.util.HashMap;

import org.junit.Before;
import org.junit.Test;
import org.springframework.dao.DuplicateKeyException;

import com.ibaset.common.BinaryParameter;
import com.ibaset.solumina.sfcore.application.IMessage;

public class TestPassThroughProc {

    private PassThroughProc passThroughProc;
    private IMessage message;
    final String procedureName = "PROC1";
    final HashMap<String, BinaryParameter> binaryParameters = new HashMap<>();
    final String duplicateKeyMessage = "ORA-00001";
    final String exceptionMessage = "TestException";

    @Before
    public void setUp() throws Exception {
        passThroughProc = spy(PassThroughProc.class);
        message = mock(IMessage.class);
        passThroughProc.setMessage(message);
    }

    @Test
    public void testExecute_DuplicateKeyException() throws Exception {
        doThrow(new DuplicateKeyException(exceptionMessage)).when(passThroughProc).doExecute(procedureName,
                binaryParameters);

        passThroughProc.execute(procedureName, binaryParameters);

        verify(message, times(1)).raiseError(duplicateKeyMessage);
    }

    @Test(expected = RuntimeException.class)
    public void testExecute_NullPointerException() throws Exception {
        doThrow(new NullPointerException(exceptionMessage)).when(passThroughProc).doExecute(procedureName,
                binaryParameters);

        passThroughProc.execute(procedureName, binaryParameters);

        verify(message, times(0)).raiseError(duplicateKeyMessage);
    }

    @Test
    public void testExecute_NoException() throws Exception {
        doReturn(new HashMap<>()).when(passThroughProc).doExecute(procedureName, binaryParameters);

        passThroughProc.execute(procedureName, binaryParameters);

        verify(message, times(0)).raiseError(duplicateKeyMessage);
    }

}
