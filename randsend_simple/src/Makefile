## -*- Makefile -*-
##
## User: fengxun
## Time: 2019-7-3 17:33:01
## Makefile created by Fengxun.
##
## This file is generated automatically.
##


#### Compiler and tool definitions shared by all build targets #####
#include ../../../global.make
include ./global.make

CFLAGS = $(BASICOPTS) -c


# Define the target directories.
TARGETDIR=.
TARGET=randSend.$(SUFFIX)

all: $(TARGETDIR)/$(TARGET)

CPPFLAGS = -I../inc 

OBJS =  \
	$(TARGETDIR)/cJSON.o \
	$(TARGETDIR)/main.o 

USERLIBS = -L/usr/local/lib -lmosquitto

DEPLIBS = -L/usr/lib64
LDLIBS = $(SYSLIBS) $(USERLIBS) $(BASICLIBS) 

# Link or archive
$(TARGETDIR)/$(TARGET): $(TARGETDIR) $(OBJS) 
	$(LINK) -o $@ $(OBJS) $(DEPLIBS) $(LDLIBS)

# Compile source files into .o files
$(TARGETDIR)/cJSON.o: $(TARGETDIR) cJSON.c
	$(COMPILE) $(CFLAGS) $(CPPFLAGS) -o $@ cJSON.c

$(TARGETDIR)/main.o: $(TARGETDIR) main.c
	$(COMPILE) $(CFLAGS) $(CPPFLAGS) -o $@ main.c


#### Clean target deletes all generated files ####
clean:
	$(RM) \
	$(TARGETDIR)/$(TARGET) \
	$(OBJS)

# Enable dependency checking
.KEEP_STATE:
.KEEP_STATE_FILE: .make.state
