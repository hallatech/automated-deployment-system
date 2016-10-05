#!/bin/bash
# Spindrift Auto Deployment Setup Script

# The root folder for Spindrift Automation
/bin/mkdir ~/workspace

# The home folder for Spindrift Automation relative ADROOT
# Code from the Automation source control is located here
/bin/mkdir ~/workspace/SpindriftAD-TRUNK

# The folder where ATG BuildInput files are located relative from ADROOT
/bin/mkdir -p ~/workspace/Input/BuildInputs

# The folder to store ATG License files
/bin/mkdir ~/workspace/Input/ATGLicenses

# The folder where Releases will be generated
/bin/mkdir ~/workspace/Release

# A folder for storing temp working files
/bin/mkdir -p ~/workspace/Output/AllConfig

# Checkout latest source from Spindrift svn
# ssh keys must be setup for authentication
/usr/bin/svn co svn+ssh://rich@svn.spindriftgroup.com/var/svn/Initiatives/AutomatedDeploymentSystem/trunk ~/workspace/SpindriftAD-TRUNK/

# Copy gradle properties template
/bin/cp ~/workspace/SpindriftAD-TRUNK/gradle.properties.tmp ~/workspace/SpindriftAD-TRUNK/gradle.properties


