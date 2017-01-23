#!/bin/sh

function usage(){
    echo "Khc Usage:"
    echo ""
    echo "      khc [ -p port ] [ -c configuration ] [ -l log-configuration ]"
    echo ""
    echo "           -p    TCP port number where Apache Tomcat will be listening for requests"
    echo "           -c    Configuration file"
    echo "           -l    Log4j file properties to be used"
    echo ""
}

# Get input parameters
while [ "$1" != "" ]; do
    case $1 in
        -c )
            shift
            KHC_CONFIG=$1
            ;;
        -l )
            shift
            KHC_LOG_CONFIG=$1
            ;;
        -p )
            shift
            SERVER_PORT=$1
            ;;
        -e )
            PROFILE="embed_db"
            ;;
        *  )
            usage
            exit
            ;;
    esac
    shift
done

DIRNAME=$(dirname "$0")
GREP="grep"

# OS specific support (must be 'true' or 'false').
cygwin=false;
darwin=false;
linux=false;
case "`uname`" in
    CYGWIN*)
        cygwin=true
        ;;

    Darwin*)
        darwin=true
        ;;

    Linux)
        linux=true
        ;;
esac

# Setup the JVM
if [ "x$JAVA" = "x" ]; then
    if [ "x$JAVA_HOME" != "x" ]; then
        JAVA="$JAVA_HOME/bin/java"
    else
        JAVA="java"
    fi
fi

if [ "$PRESERVE_JAVA_OPTS" != "true" ]; then
    # Check for -d32/-d64 in JAVA_OPTS
    JVM_D64_OPTION=`echo $JAVA_OPTS | $GREP "\-d64"`
    JVM_D32_OPTION=`echo $JAVA_OPTS | $GREP "\-d32"`

    # Check If server or client is specified
    SERVER_SET=`echo $JAVA_OPTS | $GREP "\-server"`
    CLIENT_SET=`echo $JAVA_OPTS | $GREP "\-client"`

    if [ "x$JVM_D32_OPTION" != "x" ]; then
        JVM_OPTVERSION="-d32"
    elif [ "x$JVM_D64_OPTION" != "x" ]; then
        JVM_OPTVERSION="-d64"
    elif $darwin && [ "x$SERVER_SET" = "x" ]; then
        # Use 32-bit on Mac, unless server has been specified or the user opts are incompatible
        "$JAVA" -d32 $JAVA_OPTS -version > /dev/null 2>&1 && PREPEND_JAVA_OPTS="-d32" && JVM_OPTVERSION="-d32"
    fi

    CLIENT_VM=false
    if [ "x$CLIENT_SET" != "x" ]; then
        CLIENT_VM=true
    elif [ "x$SERVER_SET" = "x" ]; then
        if $darwin && [ "$JVM_OPTVERSION" = "-d32" ]; then
            # Prefer client for Macs, since they are primarily used for development
            CLIENT_VM=true
            PREPEND_JAVA_OPTS="$PREPEND_JAVA_OPTS -client"
        else
            PREPEND_JAVA_OPTS="$PREPEND_JAVA_OPTS -server"
        fi
    fi

    if [ $CLIENT_VM = false ]; then
        NO_COMPRESSED_OOPS=`echo $JAVA_OPTS | $GREP "\-XX:\-UseCompressedOops"`
        if [ "x$NO_COMPRESSED_OOPS" = "x" ]; then
            "$JAVA" $JVM_OPTVERSION -server -XX:+UseCompressedOops -version >/dev/null 2>&1 && PREPEND_JAVA_OPTS="$PREPEND_JAVA_OPTS -XX:+UseCompressedOops"
        fi

        NO_TIERED_COMPILATION=`echo $JAVA_OPTS | $GREP "\-XX:\-TieredCompilation"`
        if [ "x$NO_TIERED_COMPILATION" = "x" ]; then
            "$JAVA" $JVM_OPTVERSION -server -XX:+TieredCompilation -version >/dev/null 2>&1 && PREPEND_JAVA_OPTS="$PREPEND_JAVA_OPTS -XX:+TieredCompilation"
        fi
    fi

    JAVA_OPTS="$PREPEND_JAVA_OPTS $JAVA_OPTS"
fi

# Find out installation type
KHC_HOME=$(cd $DIRNAME/..;pwd)
KHC_BINARY=$KHC_HOME/lib/khc.war

if [ -f $KHC_BINARY ]; then
    # Start from local
    [ -z "$KHC_CONFIG" ] && KHC_CONFIG=$KHC_HOME/config/khc.properties
    [ -z "$KHC_LOG_CONFIG" ] && KHC_LOG_CONFIG=$KHC_HOME/config/khc-log4j.properties
else
    # Start from system
    KHC_HOME=/var/lib/khc
    KHC_BINARY=$KHC_HOME/khc.war
    [ -z "$KHC_CONFIG" ] && KHC_CONFIG=/etc/khc/khc.properties
    [ -z "$KHC_LOG_CONFIG" ] && KHC_LOG_CONFIG=/etc/khc/khc-log4j.properties
fi

[ -z "$SERVER_PORT" ] && SERVER_PORT=8080
[ -n "$PROFILE" ] && PROFILE="-Dspring.profiles.active=$PROFILE"

KHC_OPTS="-Dserver.port=$SERVER_PORT -Dkhc.config=$KHC_CONFIG -Dlog4j.configuration=$KHC_LOG_CONFIG $PROFILE"


[ -f $KHC_BINARY ] || { echo "Unable to find KHC binary file"; exit 1; }
[ -f $KHC_CONFIG ] || { echo "Unable to find configuration file: $KHC_CONFIG"; exit 1; }

# Display our environment
echo "========================================================================="
echo ""
echo "  Kurento Communicator Bootstrap Environment"
echo ""
echo "  KHC_BINARY: $KHC_BINARY"
echo ""
echo "  JAVA: $JAVA"
echo ""
echo "  JAVA_OPTS: $JAVA_OPTS"
echo ""
echo "  KHC_OPTS: $KHC_OPTS"
echo ""
echo "========================================================================="
echo ""

cd $KHC_HOME
exec $JAVA $JAVA_OPTS $KHC_OPTS -jar $KHC_BINARY
