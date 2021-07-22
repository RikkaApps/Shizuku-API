#include <jni.h>
#include <unistd.h>
#include <termios.h>
#include <fcntl.h>
#include <cstdlib>
#include <android/log.h>
#include <pthread.h>
#include "logging.h"
#include "pts.h"

static pthread_mutex_t mutex;
static struct termios old_stdin{};
static int is_stdin_raw = 0;

static int64_t getWindowSize() {
    static_assert(sizeof(jlong) == sizeof(winsize));
    jlong screen_size;

    if (ioctl(STDOUT_FILENO, TIOCGWINSZ, &screen_size) == -1) {
        PLOGE("ioctl TIOCGWINSZ");
        return 0;
    }

    return screen_size;
}

static void BSHTerminal_prepareToWaitWindowSizeChange(JNIEnv *env, jclass clazz) {
    sigset_t winch;
    sigemptyset(&winch);
    sigaddset(&winch, SIGWINCH);
    pthread_sigmask(SIG_BLOCK, &winch, nullptr);
}

static jint BSHTerminal_start(JNIEnv *env, jclass clazz, jint stdin_write_pipe, jint stdout_read_pipe) {
    if (!isatty(STDIN_FILENO) || !isatty(STDOUT_FILENO) || !isatty(STDERR_FILENO)) {
        env->ThrowNew(env->FindClass("java/lang/IllegalStateException"), "stdin/stdout/stderr is not tty");
        return 0;
    }

    if (make_fd_raw(STDIN_FILENO, old_stdin) == 0) {
        is_stdin_raw = 1;
    }

    if (pthread_mutex_lock(&mutex) != 0) {
        PLOGE("pthread_mutex_lock");
    }

    auto called = std::make_shared<std::atomic_bool>(false);
    auto func = [=]() {
        if (called->exchange(true)) {
            return;
        }

        LOGI("remote exit");

        if (is_stdin_raw) {
            restore_fd(STDIN_FILENO, old_stdin);
        }

        if (pthread_mutex_unlock(&mutex) != 0) {
            PLOGE("pthread_mutex_unlock");
        }
    };

    transfer_async(STDIN_FILENO, stdin_write_pipe/*, func*/);
    transfer_async(stdout_read_pipe, STDOUT_FILENO, func);

    return 0;
}

static jlong BSHTerminal_waitForWindowSizeChange(JNIEnv *env, jclass clazz) {
    sigset_t winch;
    int sig;

    static bool first = false;
    if (!first) {
        first = true;

        sigemptyset(&winch);
        sigaddset(&winch, SIGWINCH);
        pthread_sigmask(SIG_UNBLOCK, &winch, nullptr);
    } else {
        if (sigwait(&winch, &sig) == -1) {
            PLOGE("sigwait SIGWINCH");
        }
    }

    return (jlong) getWindowSize();
}

static void BSHTerminal_waitForProcessExit(JNIEnv *env, jclass clazz) {
    if (pthread_mutex_lock(&mutex) != 0) {
        PLOGE("pthread_mutex_lock");
    }

    if (pthread_mutex_unlock(&mutex) != 0) {
        PLOGE("pthread_mutex_unlock");
    }
}

int rikka_bsh_BSHTerminal_registerNatives(JNIEnv *env) {
    pthread_mutex_init(&mutex, nullptr);

    auto clazz = env->FindClass("rikka/bsh/BSHTerminal");
    JNINativeMethod methods[] = {
            {"start",                         "(II)I", (void *) BSHTerminal_start},
            {"prepareToWaitWindowSizeChange", "()V",   (void *) BSHTerminal_prepareToWaitWindowSizeChange},
            {"waitForWindowSizeChange",       "()J",   (void *) BSHTerminal_waitForWindowSizeChange},
            {"waitForProcessExit",            "()V",   (void *) BSHTerminal_waitForProcessExit},
    };
    return env->RegisterNatives(clazz, methods, sizeof(methods) / sizeof(methods[0]));
}
