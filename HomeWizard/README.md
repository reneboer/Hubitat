# HomeWizard Watermeter driver

This driver will poll the HomeWizard water meter HWE-WTR for current water usage in liters per minute and thetotal water use in m3 since the device was installed on your water meter.

You will have to install the water meter using USB power and enable the remote API in the HomeWizard energy app. 

Install this driver and create a virtual device using it. Plug in the fixed IP address of your water meter and set the polling interval. During polling you will see an occasional timeout warning (408), but a next poll will be succesfull again.
