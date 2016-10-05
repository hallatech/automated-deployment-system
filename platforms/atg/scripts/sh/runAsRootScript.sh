---------------------------------------
#!/bin/sh
#Create an empty jboss service first

touch /etc/rc.d/init.d/jboss
echo "#! /bin/sh" >> /etc/rc.d/init.d/jboss
echo "# chkconfig: 3 95 20" >> /etc/rc.d/init.d/jboss
echo "# description: Start and stops jboss process" >> /etc/rc.d/init.d/jboss
echo "# processname: jboss" >>/etc/rc.d/init.d/jboss

chmod 755 /etc/rc.d/init.d/jboss
chown jboss /etc/rc.d/init.d/jboss
chgrp jboss /etc/rc.d/init.d/jboss
chkconfig --level 3 jboss on

---------------------------------------
