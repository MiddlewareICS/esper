/*
 ***************************************************************************************
 *  Copyright (C) 2006 EsperTech, Inc. All rights reserved.                            *
 *  http://www.espertech.com/esper                                                     *
 *  http://www.espertech.com                                                           *
 *  ---------------------------------------------------------------------------------- *
 *  The software in this package is published under the terms of the GPL license       *
 *  a copy of which has been included with this distribution in the license.txt file.  *
 ***************************************************************************************
 */
package com.espertech.esper.supportregression.subscriber;

import com.espertech.esper.client.EPStatement;

public class SupportSubscriberMultirowUnderlyingNamedMethodWStmt extends SupportSubscriberMultirowUnderlyingBase {
    public SupportSubscriberMultirowUnderlyingNamedMethodWStmt() {
        super(true);
    }

    public void someNewDataMayHaveArrived(EPStatement stmt, Object[] newData, Object[] oldData) {
        addIndication(stmt, newData, oldData);
    }
}
