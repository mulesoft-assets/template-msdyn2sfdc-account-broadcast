/**
 * Mule Anypoint Template
 *
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 */

package org.mule.templates.integration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mule.MessageExchangePattern;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.processor.chain.SubflowInterceptingChainLifecycleWrapper;
import org.mule.streaming.ConsumerIterator;
import org.mule.transport.NullPayload;
import org.mule.util.UUID;

import com.mulesoft.module.batch.BatchTestHelper;

/**
 * The objective of this class is to validate the correct behavior of the flows
 * for this Anypoint Tempalte that make calls to external systems.
 * 
 */
public class BusinessLogicIT extends AbstractTemplateTestCase {

	private static final String KEY_NAME = "name";
	private static final String KEY_WEBSITE = "websiteurl";
	private static final String KEY_PHONE = "telephone1";
	private static final String KEY_NUMBER_OF_EMPLOYEES = "numberofemployees";
	private static final String KEY_ID = "accountid";
	private static final String SFDC_KEY_WEBSITE = "Site";
	private static final String SFDC_KEY_PHONE = "Phone";
	private BatchTestHelper helper;
	private SubflowInterceptingChainLifecycleWrapper retrieveAccountFromSalesforceFlow;
	private List<Map<String, Object>> createdAccountsInDynamics = new ArrayList<Map<String, Object>>();
	private List<String> sfdcIdList = new ArrayList<String>();
	
	@Test
	public void testMainFlow() throws Exception {		
		helper = new BatchTestHelper(muleContext);
		// Run poll and wait for it to run
		runSchedulersOnce(POLL_FLOW_NAME);
		waitForPollToRun();
			
		// Wait for the batch job executed by the poll flow to finish
		helper.awaitJobTermination(TIMEOUT_SEC * 1000, 500);
		helper.assertJobWasSuccessful();
	
		Thread.sleep(3000);
		Map<String, Object> payload0 = invokeRetrieveFlow(retrieveAccountFromSalesforceFlow, createdAccountsInDynamics.get(0));
		assertNotNull("The account 0 should have been sync but is null", payload0);
		assertEquals("The account 0 should have been sync (Website)", createdAccountsInDynamics.get(0).get(KEY_WEBSITE), payload0.get(SFDC_KEY_WEBSITE));
		assertEquals("The account 0 should have been sync (Phone)", createdAccountsInDynamics.get(0).get(KEY_PHONE), payload0.get(SFDC_KEY_PHONE));

		Map<String, Object>  payload1 = invokeRetrieveFlow(retrieveAccountFromSalesforceFlow, createdAccountsInDynamics.get(1));
		assertNotNull("The account 1 should have been sync but is null", payload1);
		assertEquals("The account 1 should have been sync (Website)", createdAccountsInDynamics.get(1).get(KEY_WEBSITE), payload1.get(SFDC_KEY_WEBSITE));
		assertEquals("The account 1 should have been sync (Phone)", createdAccountsInDynamics.get(1).get(KEY_PHONE), payload1.get(SFDC_KEY_PHONE));
				
	}
	
	@Before
	public void setUp() throws Exception {
		stopFlowSchedulers(POLL_FLOW_NAME);
		registerListeners();
		
	
		// Flow to retrieve accounts from target system after sync in g
		retrieveAccountFromSalesforceFlow = getSubFlow("retrieveAccountFromSalesforceFlow");
		retrieveAccountFromSalesforceFlow.initialise();
		createTestDataInSandBox();

	}

	private void createTestDataInSandBox() throws MuleException, Exception {
		// Create object in target system to be updated
		
		String uniqueSuffix = "_" + TEMPLATE_NAME + "_" + UUID.getUUID();
		
		Map<String, Object> salesforceAccount3 = new HashMap<String, Object>();
		salesforceAccount3.put(KEY_NAME, "Name_3_SFDC" + uniqueSuffix);
		salesforceAccount3.put(SFDC_KEY_WEBSITE, "http://example.com");
		salesforceAccount3.put(SFDC_KEY_PHONE, "112");
		List<Map<String, Object>> createdAccountInSalesforce = new ArrayList<Map<String, Object>>();
		createdAccountInSalesforce.add(salesforceAccount3);
	
		SubflowInterceptingChainLifecycleWrapper createAccountInSalesforceFlow = getSubFlow("createAccountsInSalesforceFlow");
		createAccountInSalesforceFlow.initialise();
		createAccountInSalesforceFlow.process(getTestEvent(createdAccountInSalesforce, MessageExchangePattern.REQUEST_RESPONSE));
	
		Thread.sleep(1001); // this is here to prevent equal LastModifiedDate
		
		// Create accounts in source system to be or not to be synced
	
		// This account should be synced
		Map<String, Object> account0 = new HashMap<String, Object>();
		account0.put(KEY_NAME, "Name_0_SIEB" + uniqueSuffix);
		account0.put(KEY_WEBSITE, "http://acme.org");
		account0.put(KEY_PHONE, "123");
		account0.put(KEY_NUMBER_OF_EMPLOYEES, 6000);		
		createdAccountsInDynamics.add(account0);
				
		// This account should be synced (update)
		Map<String, Object> account1 = new HashMap<String, Object>();
		account1.put(KEY_NAME,  salesforceAccount3.get(KEY_NAME));
		account1.put(KEY_WEBSITE, "http://example.edu");
		account1.put(KEY_PHONE, "911");
		account1.put(KEY_NUMBER_OF_EMPLOYEES, 7100);
		createdAccountsInDynamics.add(account1);

		SubflowInterceptingChainLifecycleWrapper createAccountFlow = getSubFlow("createAccountsInDynamicsFlow");
		createAccountFlow.initialise();
		MuleEvent event = createAccountFlow.process(getTestEvent(createdAccountsInDynamics, MessageExchangePattern.REQUEST_RESPONSE));
		List<?> results = (List<?>) event.getMessage().getPayload();
		
		logger.info("Results after adding: " + createdAccountsInDynamics.toString());
	}

	@After
	public void tearDown() throws MuleException, Exception {
		// clean MS Dynamics sandbox
		SubflowInterceptingChainLifecycleWrapper flow = getSubFlow("retrieveAccountFromDynamicsFlow");
		flow.initialise();
		List<String> ids = new ArrayList<String>();
		
		for (Map<String, Object> account : createdAccountsInDynamics){
			MuleEvent res = flow.process(getTestEvent(account, MessageExchangePattern.REQUEST_RESPONSE));
			ConsumerIterator<Object> iterator= (ConsumerIterator<Object>) res.getMessage().getPayload();
			while (iterator.hasNext()){				
				ids.add(((Map<String, String>)iterator.next()).get(KEY_ID));
			} 
		}
		
		flow = getSubFlow("deleteAccountsFromDynamicsFlow");
		flow.initialise();		
		flow.process(getTestEvent(ids, MessageExchangePattern.REQUEST_RESPONSE));
		
		// clean Salesforce sandbox
		flow = getSubFlow("deleteAccountsFromSalesforceFlow");
		flow.initialise();
		flow.process(getTestEvent(sfdcIdList , MessageExchangePattern.REQUEST_RESPONSE));
	}	
	
	@SuppressWarnings("unchecked")
	protected Map<String, Object> invokeRetrieveFlow(SubflowInterceptingChainLifecycleWrapper flow, Map<String, Object> payload) throws Exception {
		MuleEvent event = flow.process(getTestEvent(payload, MessageExchangePattern.REQUEST_RESPONSE));
		Object resultPayload = event.getMessage().getPayload();
		return resultPayload instanceof NullPayload ? null : (Map<String, Object>) resultPayload;
	}

}
