UNAME=$(shell uname -s)

ifeq ($(UNAME), SunOS)
SUB_UNAME=$(shell uname -p)
ifeq ($(SUB_UNAME), sparc)
OS=OS_SOLARIS_SPARC
SUFFIX=dxe
else
OS=OS_SOLARIS_X86
SUFFIX=xxe
endif
BASICOPTS =-g
SPECIALLIBS =  -lsocket
endif

ifeq ($(UNAME), AIX)
OS=OS_AIX
SUFFIX=axe
CC=gcc
BASICOPTS =-g -Wall -D_LINUX_SOURCE_COMPAT
SPECIALLIBS =  
endif

ifeq ($(UNAME), Linux)
OS=OS_LINUX
SUFFIX=lxe
BASICOPTS =-g -Wall -D_LINUX_SOURCE_COMPAT
SPECIALLIBS =  
endif


ifndef OS
	$(error makefile has not supported the current os.)
endif


COMPILE = $(CC)
LINK = $(COMPILE) $(LDFLAGS) $(TARGET ARCH)
ARCHIVE= $(AR) $(ARFLAGS)

BASICLIBS = -lm -lnsl -lpthread -lxml2


SYSLIBS = 
