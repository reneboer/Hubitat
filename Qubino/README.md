# Qubino drivers

Qubino DIN Dimmer driver: I started with an existing Qubino ZMNHSDx DIN Dimmer driver. The build in driver will show in Alexa as a temp sensor even if you do not have a temp sensor connected. This driver will have it show properly as a dimmer on Alexa. The device must be included without security or it will cause issues, also with the build in driver. Use a second controller to do this (zwave stick and SiLabs Windows Zwave Controller software).

Qubino Smart Plug 16A: Added the Qubino ZMNHYDx Smart Plug 16A driver as the current Hubtat does not seem to have a driver for a switch with power measurements. This driver reports Watts, Kwh, Voltage and Amperage. The device can be included secure, but best be included without security if not realy needed. This can be done on the Hub directly as this is a zwave-700 device. When you enable the "Create HTML Tile" preference an extra attribue htmlTile will be created that will report the current power, energie, amparage and voltage. You can use this in a dashboard. If not used, disable it to reduce the number of events.

Both drivers will poll the current parameters when installed or when Refresh is performed. You may need to hard refresh your browser to see the correct parameter values, especially for enumerated (drop down) values. After meking changed to the paramaters, click Save preferences and then run the Configure command.

