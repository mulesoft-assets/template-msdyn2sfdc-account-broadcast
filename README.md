
# Anypoint Template: MS Dynamics to Salesforce account Broadcast

# License Agreement
This template is subject to the conditions of the 
<a href="https://s3.amazonaws.com/templates-examples/AnypointTemplateLicense.pdf">MuleSoft License Agreement</a>.
Review the terms of the license before downloading and using this template. You can use this template for free 
with the Mule Enterprise Edition, CloudHub, or as a trial in Anypoint Studio.

# Use Case
This template sets an online migration of accounts from MS Dynamics to Salesforce.
Every time there is a new account or a change in an already existing one, the integration polls for changes from the MS Dynamics source instance and it is responsible for updating the account in Salesforce target instance.

Requirements have been set not only to be used as examples, but also to establish a starting point to adapt your integration to your requirements.

As implemented, this template leverages the batch module.
The batch job is divided in *Process* and *On Complete* stages.
The integration is triggered by a Scheduler defined in the flow that is going to trigger the application, querying newest MS Dynamics updates/creations matching a filter criteria and executing the batch job.
During the *Process* stage, each MS Dynamics account is filtered depending on if it has an existing matching account in Salesforce.
The last step of the *Process* stage groups the accounts and creates or updates them in Salesforce.

Finally during the *On Complete* stage, the template logs output statistics data to the console.

# Considerations

To make this template run, there are certain preconditions that must be considered. All of them deal with the preparations in both source and destination systems, that must be made in order for all to run smoothly. Failing to do so could lead to unexpected behavior of the template.

The fusiness logic supports custom mapping for the Industry attribute of accounts:

1. Agriculture and Non-petrol Natural Resource Extraction
2. Consulting
3. Doctor's Offices and Clinics
4. Entertainment Retail 
5. Financial
 
**Note:** You need to install the Java Cryptography Extensions to be able to connect to MS Dynamics. Choose a relevant version according to your Java installation.



## Salesforce Considerations

Here's what you need to know about Salesforce to get this template to work.

### FAQ

- Where can I check that the field configuration for my Salesforce instance is the right one? See: <a href="https://help.salesforce.com/HTViewHelpDoc?id=checking_field_accessibility_for_a_particular_field.htm&language=en_US">Salesforce: Checking Field Accessibility for a Particular Field</a>
- Can I modify the Field Access Settings? How? See: <a href="https://help.salesforce.com/HTViewHelpDoc?id=modifying_field_access_settings.htm&language=en_US">Salesforce: Modifying Field Access Settings</a>


### As a Data Destination

There are no considerations with using Salesforce as a data destination.





## Microsoft Dynamics CRM Considerations

### As a Data Source

There are no considerations with using Microsoft Dynamics CRM as a data origin.




# Run it!
Simple steps to get MS Dynamics to Salesforce account Broadcast running.


## Running On Premises
In this section we help you run your template on your computer.


### Where to Download Anypoint Studio and the Mule Runtime
If you are a newcomer to Mule, here is where to get the tools.

+ [Download Anypoint Studio](https://www.mulesoft.com/platform/studio)
+ [Download Mule runtime](https://www.mulesoft.com/lp/dl/mule-esb-enterprise)


### Importing a Template into Studio
In Studio, click the Exchange X icon in the upper left of the taskbar, log in with your
Anypoint Platform credentials, search for the template, and click **Open**.


### Running on Studio
After you import your template into Anypoint Studio, follow these steps to run it:

+ Locate the properties file `mule.dev.properties`, in src/main/resources.
+ Complete all the properties required as per the examples in the "Properties to Configure" section.
+ Right click the template project folder.
+ Hover your mouse over `Run as`
+ Click `Mule Application (configure)`
+ Inside the dialog, select Environment and set the variable `mule.env` to the value `dev`
+ Click `Run`


### Running on Mule Standalone
Complete all properties in one of the property files, for example in mule.prod.properties and run your app with the corresponding environment variable. To follow the example, this is `mule.env=prod`. 


## Running on CloudHub
While creating your application on CloudHub (or you can do it later as a next step), go to Runtime Manager > Manage Application > Properties to set the environment variables listed in "Properties to Configure" as well as the **mule.env**.


### Deploying your Anypoint Template on CloudHub
Studio provides an easy way to deploy your template directly to CloudHub, for the specific steps to do so check this


## Properties to Configure
To use this template, configure properties (credentials, configurations, etc.) in the properties file or in CloudHub from Runtime Manager > Manage Application > Properties. The sections that follow list example values.
### Application Configuration
**Batch Aggregator Configuration**
+ page.size `200`

**Scheduler Configuration**
+ scheduler.frequency `10000`
+ scheduler.start.delay `100`

**Watermarking default last query timestamp for example 2018-12-13T03:00:59Z**
+ watermark.default.expression `2018-04-01T19:40:27Z`

**MS Dynamics Connector Configuration**
+ msdyn.user `msdyn_user`
+ msdyn.password `msdyn_password`
+ msdyn.url `https://{your MS Dynamics URL}`
+ msdyn.retries `5`

**Salesforce Connector Configuration**
+ sfdc.username `joan.baez@orgb`
+ sfdc.password `JoanBaez456`
+ sfdc.securityToken `ces56arl7apQs56XTddf34X`

# API Calls
Salesforce imposes limits on the number of API calls that can be made. Therefore calculating this amount may be an important factor to consider. The template calls to the API can be calculated using the formula:

***1 + X + X / ${page.size}***

***X*** is the number of accounts to be synchronized on each run. 

Divide by ***${page.size}*** because by default, accounts are gathered in groups of ${page.size} for each upsert API call in the commit step.	

For instance if 10 records are fetched from origin instance, then 12 API calls are made (1 + 10 + 1).


# Customize It!
This brief guide intends to give a high level idea of how this template is built and how you can change it according to your needs.
As Mule applications are based on XML files, this page describes the XML files used with this template.

More files are available such as test classes and Mule application files, but to keep it simple, we focus on these XML files:

* config.xml
* businessLogic.xml
* endpoints.xml
* errorHandling.xml


## config.xml
Configuration for connectors and configuration properties are set in this file. Even change the configuration here, all parameters that can be modified are in properties file, which is the recommended place to make your changes. However if you want to do core changes to the logic, you need to modify this file.

In the Studio visual editor, the properties are on the *Global Element* tab.


## businessLogic.xml
This file holds the functional aspect of the template, directed by a flow responsible of conducting the business logic.

The functional aspect of this template is implemented in this XML, directed by a flow that polls for MS Dynamics creations and updates.
Several message processors constitute four high level actions that fully implement the logic of this template:

1. The template queries for all the existing accounts from MS Dynamics modified after the watermark.
2. During the *Process* stage, each account is filtered depending on if it has an existing matching account in Salesforce.
3. The last step of the *Process* stage, groups the accounts and creates or updates them in Salesforce.
4. During the *On Complete* stage, the template logs output statistics data into the console.



## endpoints.xml
This file is formed by a flow containing the Scheduler that periodically queries MS Dynamics for updated or created accounts that meet the defined criteria in the query. Then it executes the batch job process that the query results follow.



## errorHandling.xml
This is the right place to handle how your integration reacts depending on the different exceptions. 
This file provides error handling that is referenced by the main flow in the business logic.




