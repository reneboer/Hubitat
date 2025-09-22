# Driver for Aeotec Smart Switch 7 EU version

Supports secure and unsecure inclusion.

To manually include press button twice slowly. First press stops smart start, second starts inclusion mode.

Be careful with the Device Reset function. The Factory reset option will also remove the device from your Z-Wave network and needs to be re-included.

# Driver for Aeotec Home Energy Meter 8 3 clamp version

Supports secure and unsecure inclusion.

To manually include press the button once.

The HEM 8 supports reporting for both consumption and production. If you require the production reports set the "Include production end points" to Yes and three additional child devices will be created for produciton reporting. A number of addional measurements like kVarh, kVar, kVah and Power Factor are possible. You can select "Include Special Reports" to pull those values when using the Refresh command. For the other timed reports you confire the reported values using the parameters 101 - 106 using the table below.

    0:       'None',
    1:       'kWh Total', 
    2:       'W Total',
    4:       'V Total',
    8:       'A Total', 
    16:      'kVarh Total', 
    32:      'kVar Total', 
    64:      'kVah Total', 
    128:     'powerFactor Total', 
    256:       'kWh Clamp 3', 
    512:       'W Clamp 3',
    1024:      'V Clamp 3',
    2048:      'A Clamp 3',
    4096:      'kVarh Clamp 3',
    8192:      'kVar Clamp 3',
    16384:     'kVah Clamp 3',
    32768:     'PF Clamp 3',
    65536:     'kWh Clamp 2',
    131072:    'W Clamp 2',
    262144:    'V Clamp 2',
    524288:    'A Clamp 2',
    1048576:   'kVarh Clamp 2',
    2097152:   'kVar Clamp 2',
    4194304:   'kVah Clamp 2',
    8388608:   'PF Clamp 2',
    16777216:  'kWh Clamp 1',
    33554432:  'W Clamp 1',
    67108864:  'V Clamp 1',
    134217728: 'A Clamp 1'
    268435456: 'kVarh Clamp 1',
    536870912: 'kVar Clamp 1',
    1073741824:'kVah Clamp 1',
    2147483648:'PF Clamp 1'

More details are on the Aeotec support pages: https://aeotec.freshdesk.com/support/solutions/folders/6000243211
