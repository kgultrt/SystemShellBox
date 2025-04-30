set -e

if [ "$1" = "0" ]; then
   result=$(sh "$PREFIX/local/bin/rish" -c "/system/bin/app_process -Djava.class.path=\"$PKG_PATH\" /system/bin com.rk.shell.Installer" "$2")

   sh "$PREFIX/local/bin/rish" -c "
       mkdir -p /data/local/tmp/ReTerminal/$2
       export LD_LIBRARY_PATH=/data/local/tmp/ReTerminal
       export PROOT_TMP_DIR=/data/local/tmp/ReTerminal/$2
       /data/local/tmp/ReTerminal/proot $result /bin/login -f root
   "

elif [ "$1" = "1" ]; then
    sh "$PREFIX/local/bin/rish"
elif [ "$1" = "2" ]; then
    sh
elif [ "$1" = "3" ]; then
    result=$(su -c "/system/bin/app_process -Djava.class.path=\"$PKG_PATH\" /system/bin com.rk.shell.Installer" "$2")
    su -c "
           mkdir -p /data/local/tmp/ReTerminal/$2
           export LD_LIBRARY_PATH=/data/local/tmp/ReTerminal
           export PROOT_TMP_DIR=/data/local/tmp/ReTerminal/$2
           /data/local/tmp/ReTerminal/proot $result /bin/login -f root
       "
else
    echo "Unknown working mode $1"
fi


