cmake_minimum_required(VERSION 3.4.1)

set(CMAKE_VERBOSE_MAKEFILE on)

# now build app's shared lib
set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} \
  -D__USE_GNU -D__linux__ \
  -DNO_ARDRONE_MAINLOOP -DUSE_ANDROID \
  -DTARGET_CPU_ARM=1 -DTARGET_CPU_X86=0 \
  -DUSE_WIFI -DFFMPEG_SUPPORT -fstack-protector \
  ")
set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} -DANDROID_NDK -include ${Project_SOURCE_DIR}/vmcipc_defines.h")
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} \
  -I${Project_SOURCE_DIR} \
  -I${Project_SOURCE_DIR}/include \
  -I${Project_SOURCE_DIR}/DEC/ \
  -I${Project_SOURCE_DIR}/DEC/include/ -std=c++11 \
  -L${Project_SOURCE_DIR} -L${Project_SOURCE_DIR}/LIB -L${Project_BINARY_DIR} \
  ")

set(vmcipc_SRCS vmcipc.cpp
                gl_bg_video_sprite_stub.cpp
                gl_video_stage.cpp
                java_callbacks.cpp
                vmcipc_videostream.cpp
                vmcipc_cmd.cpp
                vmcipc_javatool.cpp
                vmcipc_debug.cpp
                ftp_stub.cpp
                config_stub.cpp
                config/minIni.c
                config/IPCConfig.cpp
                legacy/wbuf.c
                legacy/fflush.c
                legacy/wsetup.c
                legacy/makebuf.c
                legacy/findfp.c
                legacy/fwalk.c
                legacy/atexit.c
                legacy/thread_atexit.cpp
                legacy/stdio.c)
add_library(vmcipc SHARED ${vmcipc_SRCS})

# add lib dependencies
set(vmcipc_LIBS c stdc++ cutils utils android
                stagefright_foundation stagefright binder ui
                ssl crypto boost_system boost_timer boost_chrono boost_thread
                wa rtmptoolkit vmcffmpegdec ftp
                avcodec avutil utils binder jpeg avformat vnettool)
target_link_libraries(vmcipc log GLESv2 jnigraphics ${vmcipc_LIBS})
