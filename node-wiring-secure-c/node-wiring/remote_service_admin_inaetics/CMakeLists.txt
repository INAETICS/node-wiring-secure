#
# Licensed under Apache License v2. See LICENSE for more information.
#

# Add -fPIC for x86_64 Unix platforms; this lib will be linked to a shared lib
if(UNIX AND NOT WIN32)
	find_program(CMAKE_UNAME uname /bin /usr/bin /usr/local/bin )
	if(CMAKE_UNAME)
		exec_program(uname ARGS -m OUTPUT_VARIABLE CMAKE_SYSTEM_PROCESSOR)
		set(CMAKE_SYSTEM_PROCESSOR ${CMAKE_SYSTEM_PROCESSOR} CACHE INTERNAL "processor type (i386 and x86_64)")
		if(CMAKE_SYSTEM_PROCESSOR MATCHES "x86_64")
			add_definitions(-fPIC)
		endif(CMAKE_SYSTEM_PROCESSOR MATCHES "x86_64")
	endif(CMAKE_UNAME)
endif(UNIX AND NOT WIN32)

add_subdirectory(rsa)
