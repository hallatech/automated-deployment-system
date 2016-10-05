#!/bin/sh

#Run the .profile so that environment variables are correctly set

# Finally just run the .bash_profile
. ~/.bash_profile

if [ ${#JBOSS_HOME} -eq 0 ]
then 
	echo "JBOSS_HOME is not set, will set JBOSS_HOME"
	echo "export JBOSS_HOME=#spindrift.script.postDeployment.DeployHome#/Jboss/jboss-as" >> ~/.bash_profile
else 
	echo "JBOSS_HOME is set"
fi

if [ ${#JAVA_HOME} -eq 0 ]
then
        echo "export JAVA_HOME=/usr/local/java/jdk1.6.0_25" >> ~/.bash_profile
        echo "export PATH=\$JAVA_HOME/bin:\$PATH" >> ~/.bash_profile
        cp #spindrift.script.postDeployment.DeployHome#/JDK/jdk-6u25-linux-x64.bin /usr/local/java
        cp #spindrift.script.postDeployment.DeployHome#/scripts/sh/answer.txt /usr/local/java
        chmod 750 #spindrift.script.postDeployment.DeployHome#/scripts/sh/installJDK.sh
        #spindrift.script.postDeployment.DeployHome#/scripts/sh/installJDK.sh
        
else
        echo "JDK already installed.."
fi


#Copy the jboss start stop script
cp #spindrift.script.postDeployment.DeployHome#/scripts/sh/jboss /etc/init.d/jboss
if [ -e ~/bin ]
then
    rm ~/bin/jboss
else
    mkdir ~/bin
fi
cp #spindrift.script.postDeployment.DeployHome#/scripts/sh/jboss $HOME/bin
chmod 750 ~/bin/jboss

# Copy less_jboss.sh
rm ~/bin/less_jboss.sh
touch ~/bin/less_jboss.sh
chmod +x ~/bin/less_jboss.sh
echo "#!/bin/sh" >> ~/bin/less_jboss.sh
echo "if [ \$# -ne 1 ]" >> ~/bin/less_jboss.sh
echo "then" >> ~/bin/less_jboss.sh
echo "echo \"Usage: less_jboss.sh  slot1|slot2|slot3|slot4\"" >> ~/bin/less_jboss.sh
echo "exit 0" >> ~/bin/less_jboss.sh
echo "fi" >> ~/bin/less_jboss.sh
echo "echo \"less #spindrift.script.postDeployment.DeployHome#/Jboss/jboss-as/server/\$1/log/server.log\"" >> ~/bin/less_jboss.sh
echo "less #spindrift.script.postDeployment.DeployHome#/Jboss/jboss-as/server/\$1/log/server.log" >> ~/bin/less_jboss.sh

# Copy tail_jboss.sh
rm ~/bin/tail_jboss.sh
touch ~/bin/tail_jboss.sh
chmod +x ~/bin/tail_jboss.sh
echo "#!/bin/sh" >> ~/bin/tail_jboss.sh
echo "if [ \$# -ne 1 ]" >> ~/bin/tail_jboss.sh
echo "then" >> ~/bin/tail_jboss.sh
echo "echo \"Usage: tail_jboss.sh slot1|slot2|slot3|slot4\"" >> ~/bin/tail_jboss.sh
echo "exit 0" >> ~/bin/tail_jboss.sh
echo "fi" >> ~/bin/tail_jboss.sh
echo "echo \"tail -200f #spindrift.script.postDeployment.DeployHome#/Jboss/jboss-as/server/\$1/log/server.log\"" >> ~/bin/tail_jboss.sh
echo "tail -200f #spindrift.script.postDeployment.DeployHome#/Jboss/jboss-as/server/\$1/log/server.log" >> ~/bin/tail_jboss.sh



#Add house keeping activity to crontab

cronentry=$(crontab -l | wc -l)

if [ $cronentry -ne 0 ]
then
        echo "Cron tab already exists $cronentry"
else
        echo "Cron tab doesn't exist $cronentry"
        echo "Installing prune jboss logs into crontab"
        crontab #spindrift.script.postDeployment.DeployHome#/scripts/sh/pruneLogCron.txt
fi

