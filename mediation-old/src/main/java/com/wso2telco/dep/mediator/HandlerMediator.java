/*
 *
 *  Copyright (c) 2014, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 * /
 */
package com.wso2telco.dep.mediator;

import com.wso2telco.core.dbutils.exception.BusinessException;
import com.wso2telco.dep.mediator.internal.Base64Coder;
import com.wso2telco.dep.mediator.util.MediationHelper;
import com.wso2telco.dep.oneapivalidation.exceptions.CustomException;

import org.apache.axis2.AxisFault;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.MessageContext;
import org.apache.synapse.core.axis2.Axis2MessageContext;
import org.apache.synapse.mediators.AbstractMediator;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Date;
import java.util.Map;



// TODO: Auto-generated Javadoc

/**
 * The Class HandlerMediator.
 */
public class HandlerMediator extends AbstractMediator {

    /** The log. */
    @SuppressWarnings("unused")

    private static Log log = LogFactory.getLog(HandlerMediator.class);

    /** The executor class. */
    private String executorClass;

    /** The nb publisher. */
    private NorthboundPublisher nbPublisher;


    /* (non-Javadoc)
     * @see org.apache.synapse.Mediator#mediate(org.apache.synapse.MessageContext)
     */
    public boolean mediate(MessageContext context) {
    	String jsonBody = null;
        try {
            Class clazz = Class.forName(executorClass);
            RequestExecutor reqHandler = (RequestExecutor) clazz.newInstance();

            reqHandler.setApplicationid(storeApplication(context));

            reqHandler.initialize(context);
            reqHandler.validateRequest(reqHandler.getHttpMethod(), reqHandler.getSubResourcePath(),
                    reqHandler.getJsonBody(), context);
            jsonBody = reqHandler.getJsonBody().toString();
            reqHandler.execute(context);

        } catch (CustomException ax) {
            handleCustomException(ax.getErrcode(),ax.getErrvar(), ax, context, jsonBody);

        } catch (Exception axisFault) {
            handleException("Unexpected error ", axisFault, context);
            return false;
        }
        return true;
    }

    /**
     * Handle custom exception.
     *
     * @param errcode the errcode
     * @param errvar the errvar
     * @param ax the ax
     * @param context the context
     * @param jsonBody the json body
     */
    public void handleCustomException(String errcode, String[] errvar, CustomException ax, MessageContext context, String jsonBody) {
        context.setProperty("ERROR_CODE",errcode);
        context.setProperty("errvar",errvar[0]);

        //NB Data Publish
        if(nbPublisher == null){
            nbPublisher = new NorthboundPublisher();
        }
        nbPublisher.publishNBErrorResponseData(ax, jsonBody, context);

        handleException(ax.getErrmsg(), ax,context);
    }

    /**
     * Store application.
     *
     * @param context the context
     * @return the string
     * @throws AxisFault the axis fault
     */
    private String storeApplication(MessageContext context) throws BusinessException {

       return MediationHelper.getInstance().getApplicationId(context);
    }

    /**
     * Gets the executor class.
     *
     * @return the executor class
     */
    public String getExecutorClass() {
        return executorClass;
    }

    /**
     * Sets the executor class.
     *
     * @param executorClass the new executor class
     */
    public void setExecutorClass(String executorClass) {
        this.executorClass = executorClass;
    }
}
