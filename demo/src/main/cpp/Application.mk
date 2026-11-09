APP_CFLAGS     := -Wall -Wextra -Wmost
APP_CONLYFLAGS := -std=c23
APP_CPPFLAGS   := -std=c++23
APP_STL        := none

ifneq ($(NDK_DEBUG),1)
APP_CFLAGS     += -Os -flto -Werror
APP_CFLAGS     += -fvisibility=hidden -fvisibility-inlines-hidden
APP_CFLAGS     += -fno-unwind-tables -fno-asynchronous-unwind-tables
APP_CFLAGS     += -fno-stack-protector -fno-threadsafe-statics -U_FORTIFY_SOURCE
APP_LDFLAGS    += -flto -Wl,--icf=all -Wl,--exclude-libs,ALL
endif
