#
# JNA JNI dispatch and test library
# Copyright (c) 2007 Timothy Wall All Rights Reserved
#
# You may need to run 'ant javah' at the root before performing a build from 
# this directory.
#
# To build the JNI headers, do 'ant javah' from the root
# To build the windows DLL using gcc, do 
#   make CC=gcc
# If you have the OSX 10.4 universal SDK installed, 
#   to target ppc on OSX/i386, do 
#     make ARCH=ppc
#   to target i386 on OSX/PPC, do 
#     make ARCH=i386
OS=$(shell uname | sed -e 's/CYGWIN.*/win32/g' \
                        -e 's/MINGW32.*/win32/g' \
                        -e 's/SunOS.*/solaris/g' \
                        -e 's/Darwin.*/darwin/g' \
                        -e 's/FreeBSD.*/freebsd/g' \
                        -e 's/Linux.*/linux/g')

VERSION=$(shell sed -n 's/^.*version. value="\(.*\)".*/\1/p' ../build.xml)
VERSION:=$(shell echo $(VERSION) | sed -n 's/^\([0-9][0-9]*\)\.\([0-9][0-9]*\)\.\([0-9][0-9]*\).*/\1\.\2\.\3/p')
JAVA_INCLUDES=-I"$(JAVA_HOME)/include" \
              -I"$(JAVA_HOME)/include/$(OS)"
SRCDIR=$(shell pwd)
BUILD=../build/native
JAVAH=$(BUILD)
INSTALLDIR=../build/$(OS)
JNIDISPATCH_OBJS=$(BUILD)/dispatch.o \
                 $(BUILD)/callback.o

FFI_OBJS=$(BUILD)/libffi/.libs/libffi_convenience.a

LIBRARY=$(BUILD)/$(LIBPFX)jnidispatch$(JNISFX)
TESTLIB=$(BUILD)/$(LIBPFX)testlib$(LIBSFX)

# Reasonable defaults based on GCC
LIBPFX=lib
LIBSFX=.so
JNISFX=$(LIBSFX)
CC=gcc
LD=gcc
# Default to Sun recommendations for JNI compilation
# Optimization causes errors in FP callback return values on linux/x86,
# crashes osx/x86, and other errors on ppc
# Turning it off eliminates those failures
#COPT=-O2 -fomit-frame-pointer
COPT=-fno-omit-frame-pointer -fno-strict-aliasing 
CASM=-S
ifeq ($(DEBUG),true)
CDEBUG=-g
endif
COUT=-o $@
CINCLUDES=$(JAVA_INCLUDES) -I"$(JAVAH)" -I$(BUILD)/libffi/include
CDEFINES=-D_REENTRANT
CDEFINES+=-W -Wall -Wno-unused -Wno-parentheses -Werror
CFLAGS=$(PCFLAGS) $(COPT) $(CDEBUG) $(CDEFINES) $(CINCLUDES)
LDFLAGS=-o $@ -shared -Wl,-soname,$@ 
LIBDIR=$(JAVA_HOME)/jre/lib/$(ARCH)
LIBS=-L"$(LIBDIR)" -ljawt 
STRIP=strip -x
# end defaults

ifeq ($(OS),win32)
CC=gcc
ARCH=i386

ifeq ($(CC),cl)
LD=link
COPT=-O1 -Op
ifeq ($(DEBUG),true)
COPT=-Od
DBG=d
CDEBUG=-D_DEBUG -GZ -Zi
endif
COUT=-Fo$@
CDEFINES=-DWIN32 -D_WINDOWS -D_MBCS -D_USRDLL -DNATIVE_EXPORTS 
PCFLAGS=-nologo -MD$(DBG) -W3 -WX -GX -YX -Fd$(@D)/$* -Fp$(@D)/$*
CASM=-FAs

LDFLAGS=/pdb:$(subst .dll,,$@).pdb /implib:$(subst .dll,,$@).lib /debug /machine:i386 /pdbtype:sept /dll /nologo /opt:REF /incremental:no /out:$@ 
LIBDIR=/libpath:"$(JAVA_HOME)/lib" 
LIBS=$(LIBDIR) jawt.lib
STRIP=@echo
else
CDEFINES=-D__int64="long long" -D_JNI_IMPLEMENTATION
PCFLAGS=-mno-cygwin
# NOTE: have to recreate specs or dllcrt2.o isn't found
SPECS=$(BUILD)/specs.new
LDFLAGS+=-mno-cygwin -Wl,--add-stdcall-alias -specs $(SPECS) -L /lib/mingw 
LIBDIR="$(JAVA_HOME)/lib"
LIBS=$(LIBDIR)/jawt.lib
endif

LIBPFX=
LIBSFX=.dll
endif

ifeq ($(OS),linux)
ARCH=i386
PCFLAGS+=-fPIC
# Avoid 1.5/1.6 bug caused by linking directly to -ljawt
CDEFINES+=-DNEED_JAWT_HACK
LIBS=
endif

ifeq ($(OS),freebsd)
ARCH=i386
PCFLAGS+=-fPIC
CINCLUDES+=-I/usr/X11R6/include
LDFLAGS=-o $@ -shared -pthread
# Avoid 1.5/1.6 bug caused by linking directly to -ljawt
CDEFINES+=-DNEED_JAWT_HACK
LIBS=
endif

# FIXME untested
ifeq ($(OS),solaris)
ifeq ($(ARCH),)
  ARCH=$(shell uname -n)
endif

# Avoid 1.5/1.6 bug caused by linking directly to -ljawt
PCFLAGS+=-fPIC
CDEFINES+=-DNEED_JAWT_HACK
LDFLAGS=-o $@ -G
LIBS=
ifeq ($(ARCH), sparcv9)
  # alter CC instead of PCFLAGS, since we need to pass it down to libffi configure
  # and some of the other settings in PCFLAGS might make the build choke
  CC += -m64  
  LDFLAGS += -m64 
endif
endif

ifeq ($(OS),darwin)
ARCH=$(shell arch)
LIBSFX=.dylib
JNISFX=.jnilib
# Use universal binary when updated to work with gcc 4
ifneq ($(SDKROOT),)
SYSLIBROOT=-Wl,-syslibroot,$(SDKROOT)
ISYSROOT=-isysroot $(SDKROOT)
ARCHFLAGS=
#endif
endif
PCFLAGS+=$(ARCHFLAGS) $(ISYSROOT) -x objective-c
CDEFINES=-DTARGET_RT_MAC_CFM=0
LDFLAGS=-arch i386 -arch ppc -dynamiclib -o $@ -framework JavaVM \
  -compatibility_version $(shell echo ${VERSION}|sed 's/^\([0-9][0-9]*\).*/\1/g') \
  -current_version $(VERSION) \
  -mmacosx-version-min=10.3 \
  -install_name ${@F} \
  $(SYSLIBROOT)
# JAWT linkage handled by -framework JavaVM
LIBS=
endif

ifeq ($(OS),darwin)
$(BUILD)/%.o : %.c
	@mkdir -p $(BUILD)
	$(CC) -arch i386 -I$(BUILD)/libffi-i386/include $(CFLAGS) -c $< -o $@.i386
	$(CC) -arch ppc -I$(BUILD)/libffi-ppc/include $(CFLAGS) -c $< -o $@.ppc
	lipo -create -output $@ $@.i386 $@.ppc
else
$(BUILD)/%.o : %.c
	@mkdir -p $(BUILD)
	$(CC) $(CFLAGS) -c $< $(COUT)
endif

all: $(LIBRARY) $(TESTLIB)

install:
	mkdir $(INSTALLDIR)
	cp $(LIBRARY) $(INSTALLDIR)

$(BUILD)/test_dispatch: test_dispatch.c $(BUILD)/dispatch.o $(BUILD)/dispatch_$(ARCH).o $(BUILD)/callback.o 
	$(CC) $(CFLAGS) $< $(BUILD)/dispatch.o $(BUILD)/dispatch_$(ARCH).o $(BUILD)/callback.o $(COUT)

$(BUILD)/specs.new: 
	gcc -dumpspecs | sed s%dllcrt2%/lib/mingw/dllcrt2%g > $@

$(LIBRARY): $(FFI_OBJS) $(JNIDISPATCH_OBJS) $(SPECS)
	$(LD) $(LDFLAGS) $(JNIDISPATCH_OBJS) $(FFI_OBJS) $(LIBS)

$(TESTLIB): $(BUILD)/testlib.o $(SPECS)
	$(LD) $(LDFLAGS) $< $(TESTDEF) 

ifeq ($(OS),darwin)
$(FFI_OBJS):		
	@mkdir -p $(BUILD)/libffi-ppc
	@if [ ! -f $(BUILD)/libffi-ppc/Makefile ]; then \
	    echo "Configuring libffi for powerpc"; \
	    cd $(BUILD)/libffi-ppc && \
	      env CFLAGS="-O -g -isysroot /Developer/SDKs/MacOSX10.4u.sdk -arch ppc" \
                LDFLAGS="-arch ppc" $(SRCDIR)/libffi/configure --disable-static --with-pic=yes \
                --build=powerpc-apple-darwin --disable-dependency-tracking > /dev/null; \
	fi
	$(MAKE) -C $(BUILD)/libffi-ppc
	
	@mkdir -p $(BUILD)/libffi-i386
	@if [ ! -f $(BUILD)/libffi-i386/Makefile ]; then \
	    echo "Configuring libffi for i386"; \
	    cd $(BUILD)/libffi-i386 && \
	      env CFLAGS="-O -g -isysroot /Developer/SDKs/MacOSX10.4u.sdk -arch i386" \
                LDFLAGS="-arch i386" $(SRCDIR)/libffi/configure --disable-static --with-pic=yes \
                --build=i386-apple-darwin --disable-dependency-tracking > /dev/null; \
	fi
	$(MAKE) -C $(BUILD)/libffi-i386
	
	# Assemble into a FAT (both ppc & i386) library
	@mkdir -p $(BUILD)/libffi/.libs
	/usr/bin/libtool -static -o $@ $(BUILD)/libffi-i386/.libs/libffi_convenience.a \
            $(BUILD)/libffi-ppc/.libs/libffi_convenience.a 	
else
$(FFI_OBJS):		
	@mkdir -p $(BUILD)/libffi
	@if [ ! -f $(BUILD)/libffi/Makefile ]; then \
	    echo "Configuring libffi"; \
	    cd $(BUILD)/libffi && \
		CC="$(CC)" $(SRCDIR)/libffi/configure --disable-static --with-pic=yes > /dev/null; \
	fi
	$(MAKE) -C $(BUILD)/libffi
endif
clean:
	$(RM) -rf $(BUILD)
