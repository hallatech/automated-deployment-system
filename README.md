==================================================================
Spindrift Software Factory Automatated Build System  setup scripts
==================================================================

1. Description
2. Requirements
3. Usage
4. Contact and Information
5. Legal Stuff/Disclaimer

===============
1. DESCRIPTION
===============

install.sh and createApplication.sh issued to setup neccessary directories required for 
building applications with the Spindrift Software Factory Automatated Build System 

===============
2. REQUIREMENTS
=============== 

application user i.e. jboss | weblogic should be created and scripts to be excecuted 
from their home directories

===============
3. USAGE
===============

install.sh - creates workspace, Output, Input, Release and SpindriftAD-TRUNK folders under ~/ 
# chmod 750 install.sh
#./install.sh

createApplication.sh - creates place holder for ear files and example configurations for the
desired applcations
# chmod 750 createApplication.sh
#./createApplication.sh "Application Name" 

Where "Application Name" could be "LiveStoreFront", "ContentAdmin" or "all" for all applications

==========================
4. CONTACT AND INFORMATION
==========================


=========================
5. LEGAL STUFF/DISCLAIMER 
=========================


